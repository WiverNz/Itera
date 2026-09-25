package com.wivernz.itera.data.catalog

import com.wivernz.itera.core.common.Logger
import com.wivernz.itera.core.common.RuntimeChecks
import com.wivernz.itera.core.common.dispatchers.IoDispatcher
import com.wivernz.itera.domain.model.CombinationStep
import com.wivernz.itera.domain.model.CompletionRule
import com.wivernz.itera.domain.model.Curriculum
import com.wivernz.itera.domain.model.CurriculumDay
import com.wivernz.itera.domain.model.ExerciseBlock
import com.wivernz.itera.domain.model.ExerciseTemplate
import com.wivernz.itera.domain.model.ExerciseType
import com.wivernz.itera.domain.model.Skill
import com.wivernz.itera.domain.model.Technique
import com.wivernz.itera.domain.model.TechniqueDefaults
import com.wivernz.itera.domain.model.TechniqueId
import com.wivernz.itera.domain.repository.TechniqueCatalogRepository
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * ADR-0005: the bundled catalogue, parsed once per process. A malformed or invalid asset fails fast in debug; in
 * release the last successfully parsed copy is used, and with none the load fails with [CatalogException].
 * Prose resolves through string resources on every mapping, so a language switch re-renders it.
 */
@Singleton
class AssetTechniqueCatalogRepository @Inject constructor(
    private val assets: CatalogAssetSource,
    private val strings: CatalogStrings,
    private val cache: CatalogCache,
    private val checks: RuntimeChecks,
    private val logger: Logger,
    @param:IoDispatcher private val io: CoroutineDispatcher
) : TechniqueCatalogRepository {
    private val mutex = Mutex()

    @Volatile private var parsed: ParsedCatalog? = null

    @Volatile private var mapped: Pair<Locale, List<Technique>>? = null

    override suspend fun catalog(): List<Technique> {
        val locale = strings.locale
        mapped?.takeIf { it.first == locale }?.let { return it.second }
        val techniques = load().techniques.techniques.map(::toDomain)
        mapped = locale to techniques
        return techniques
    }

    override suspend fun technique(id: TechniqueId): Technique? = catalog().firstOrNull {
        it.id ==
            id
    }

    override suspend fun curriculum(): Curriculum {
        val file = load().curriculum
        val byId = load().techniques.techniques.associateBy { it.id }
        return Curriculum(
            file.contentVersion,
            file.days.map { day ->
                CurriculumDay(
                    day = day.day,
                    newTechniqueId = day.newTechnique?.let(::TechniqueId),
                    combination = day.combination.mapIndexed { index, step ->
                        val minutes = byId[step.technique]?.let {
                            it.defaults.focusMinutes
                                ?: it.estimatedMinutes
                        }
                        CombinationStep(
                            TechniqueId(step.technique),
                            text(
                                CatalogKeys.stepPrompt(day.day, index + 1)
                            ).replace("%1\$d", "$minutes"),
                            text(CatalogKeys.stepHint(day.day, index + 1))
                        )
                    },
                    weeklyLookBack = day.weeklyLookBack
                )
            }
        )
    }

    /** Parses both assets at most once for the process lifetime. */
    suspend fun load(): ParsedCatalog = parsed ?: mutex.withLock {
        parsed ?: withContext(io) { readAndParse() }.also { parsed = it }
    }

    private suspend fun readAndParse(): ParsedCatalog {
        val fresh = try {
            val techniquesJson = assets.read(CatalogParser.TECHNIQUES_ASSET)
            val curriculumJson = assets.read(CatalogParser.CURRICULUM_ASSET)
            val catalog = CatalogParser.parse(techniquesJson, curriculumJson)
            val problems = CatalogValidator.validate(
                catalog,
                { strings.text(it) != null },
                { strings.array(it) != null }
            )
            if (problems.isNotEmpty()) {
                throw CatalogException(
                    "Technique catalog is invalid: $problems"
                )
            }
            cache.write(techniquesJson, curriculumJson)
            catalog
        } catch (e: CatalogException) {
            if (checks.failFast) throw e
            logger.w(TAG, "Bundled catalog rejected; using the last good copy")
            null
        } catch (e: java.io.IOException) {
            if (checks.failFast) throw CatalogException("Technique catalog unreadable", e)
            logger.w(TAG, "Bundled catalog unreadable; using the last good copy")
            null
        }
        return fresh
            ?: cache.read()?.let { (techniques, curriculum) ->
                CatalogParser.parse(techniques, curriculum)
            }
            ?: throw CatalogException("No usable technique catalog")
    }

    private fun toDomain(dto: TechniqueDto) = Technique(
        id = TechniqueId(dto.id),
        name = text(CatalogKeys.name(dto.id)),
        shortDescription = text(CatalogKeys.short(dto.id)),
        explanation = text(CatalogKeys.why(dto.id)),
        skill = Skill.valueOf(dto.skill),
        introDay = dto.introDay,
        exerciseType = ExerciseType.valueOf(dto.exerciseType),
        estimatedMinutes = dto.estimatedMinutes,
        reviewEligible = dto.reviewEligible,
        relatedTechniqueIds = dto.related.map(::TechniqueId),
        template = dto.template?.let { template(dto.id, it) },
        defaults = TechniqueDefaults(
            focusMinutes = dto.defaults.focusMinutes,
            breakMinutes = dto.defaults.breakMinutes
        ),
        retired = dto.retired
    )

    private fun template(id: String, dto: TemplateDto) = ExerciseTemplate(
        blocks = dto.blocks.map { block ->
            val label = text(CatalogKeys.blockLabel(id, block.key))
            when (block) {
                is BlockDto.Instruction -> ExerciseBlock.Instruction(
                    block.key,
                    label,
                    block.emphasis
                )
                is BlockDto.TextInput -> ExerciseBlock.TextInput(
                    block.key,
                    label,
                    text(CatalogKeys.blockPlaceholder(id, block.key)),
                    block.minLines,
                    block.maxLines
                )
                is BlockDto.Checklist -> ExerciseBlock.Checklist(
                    block.key,
                    label,
                    block.minItems,
                    block.maxItems,
                    block.withStopwatch,
                    text(CatalogKeys.blockPlaceholder(id, block.key))
                )
                is BlockDto.PickOne -> ExerciseBlock.PickOne(
                    block.key,
                    label,
                    block.itemCount,
                    block.suggestionsKey?.let { strings.array(it) }.orEmpty()
                )
                is BlockDto.TwoLists -> ExerciseBlock.TwoLists(
                    block.key,
                    label,
                    text(CatalogKeys.blockSecondaryLabel(id, block.key)),
                    block.primaryCount,
                    block.secondaryCount
                )
                is BlockDto.ChipSelect -> ExerciseBlock.ChipSelect(
                    block.key,
                    label,
                    strings.array(block.optionsKey).orEmpty(),
                    block.allowCustom,
                    block.maxSelections
                )
            }
        },
        completionRule = when (val rule = dto.completionRule) {
            CompletionRuleDto.Always -> CompletionRule.Always
            is CompletionRuleDto.RequireBlocks -> CompletionRule.RequireBlocks(rule.keys)
            is CompletionRuleDto.RequireChecked -> CompletionRule.RequireChecked(
                rule.key,
                rule.count
            )
        }
    )

    private fun text(key: String): String = strings.text(key) ?: run {
        check(!checks.failFast) { "Missing catalog string $key" }
        logger.w(TAG, "Missing catalog string $key")
        ""
    }

    private companion object {
        const val TAG = "TechniqueCatalog"
    }
}
