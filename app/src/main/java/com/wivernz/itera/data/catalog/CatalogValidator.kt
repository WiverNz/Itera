package com.wivernz.itera.data.catalog

import com.wivernz.itera.domain.model.ExerciseType
import com.wivernz.itera.domain.model.Skill

/**
 * String-resource keys, docs/data/04-technique-catalog-format.md section 4. `{slug}` is the prototype's short
 * form, fixed per frozen id; a technique added later adds its slug here with its strings.
 */
object CatalogKeys {
    val SLUGS: Map<String, String> = mapOf(
        "two_minute_rule" to "two",
        "pomodoro" to "pomodoro",
        "eisenhower_matrix" to "eisenhower",
        "five_second_rule" to "five",
        "habit_stacking" to "stack",
        "feynman_technique" to "feynman",
        "two_list_strategy" to "twolist",
        "deep_work" to "deep",
        "pareto_principle" to "pareto",
        "spaced_repetition" to "spaced",
        "information_diet" to "diet",
        "premortem" to "premortem",
        "one_percent_improvement" to "onepct",
        "daily_reflection" to "reflect"
    )

    fun name(id: String) = "t_${SLUGS[id]}_name"
    fun short(id: String) = "t_${SLUGS[id]}_short"
    fun why(id: String) = "t_${SLUGS[id]}_why"
    fun task(id: String) = "t_${SLUGS[id]}_task"

    fun blockLabel(id: String, block: String) = "exercise_${id}_${block}_label"
    fun blockPlaceholder(id: String, block: String) = "exercise_${id}_${block}_placeholder"
    fun blockSecondaryLabel(id: String, block: String) = "exercise_${id}_${block}_secondary_label"

    fun stepPrompt(day: Int, step: Int) = "combination_day${day}_step${step}_prompt"
    fun stepHint(day: Int, step: Int) = "combination_day${day}_step${step}_hint"

    /** Every `<string>` key the catalog derives. */
    fun stringKeys(catalog: ParsedCatalog): List<String> =
        catalog.techniques.techniques.flatMap { t ->
            listOf(name(t.id), short(t.id), why(t.id), task(t.id)) +
                t.template?.blocks.orEmpty().flatMap { blockStringKeys(t.id, it) }
        } + catalog.curriculum.days.flatMap { day ->
            day.combination.indices.flatMap { i ->
                listOf(stepPrompt(day.day, i + 1), stepHint(day.day, i + 1))
            }
        }

    /** Every `<string-array>` key the catalog derives. */
    fun arrayKeys(catalog: ParsedCatalog): List<String> =
        catalog.techniques.techniques.flatMap { t ->
            t.template?.blocks.orEmpty().mapNotNull {
                when (it) {
                    is BlockDto.PickOne -> it.suggestionsKey
                    is BlockDto.ChipSelect -> it.optionsKey
                    else -> null
                }
            }
        }

    private fun blockStringKeys(id: String, block: BlockDto): List<String> = when (block) {
        is BlockDto.Instruction, is BlockDto.PickOne, is BlockDto.ChipSelect -> listOf(
            blockLabel(id, block.key)
        )
        is BlockDto.TextInput, is BlockDto.Checklist ->
            listOf(blockLabel(id, block.key), blockPlaceholder(id, block.key))
        is BlockDto.TwoLists -> listOf(
            blockLabel(id, block.key),
            blockSecondaryLabel(id, block.key)
        )
    }
}

/**
 * The eight assertions of docs/data/04-technique-catalog-format.md section 6, as a list of violations. Used by
 * `CatalogValidationTest` against the shipped assets and by the loader before trusting a parsed copy.
 */
object CatalogValidator {
    val FROZEN_IDS: Set<String> = CatalogKeys.SLUGS.keys
    val SKILL_COUNTS: Map<Skill, Int> = mapOf(
        Skill.FOCUS to 4,
        Skill.PLANNING to 3,
        Skill.LEARNING to 2,
        Skill.HABITS to 3,
        Skill.REFLECTION to 2
    )

    fun validate(
        catalog: ParsedCatalog,
        stringExists: (String) -> Boolean = { true },
        arrayExists: (String) -> Boolean = { true }
    ): List<String> = buildList {
        val techniques = catalog.techniques.techniques
        val byId = techniques.associateBy { it.id }
        // 1. exactly the frozen ids
        if (techniques.size != 14 || byId.keys != FROZEN_IDS) add("ids: ${byId.keys}")
        // enums parse
        techniques.forEach { t ->
            if (Skill.entries.none { it.name == t.skill }) add("${t.id}: skill ${t.skill}")
            if (ExerciseType.entries.none {
                    it.name == t.exerciseType
                }
            ) {
                add("${t.id}: type ${t.exerciseType}")
            }
        }
        // 2. introDay 1..13, no gaps, no duplicates
        val introDays = techniques.mapNotNull { it.introDay }
        if (introDays.sorted() != (1..13).toList()) add("introDay: ${introDays.sorted()}")
        // 3. skill counts
        val counts = techniques.groupingBy { it.skill }.eachCount()
        SKILL_COUNTS.forEach { (skill, n) ->
            if (counts[skill.name] !=
                n
            ) {
                add("skill $skill: ${counts[skill.name]}")
            }
        }
        // 4. related ids exist, never self, 2-3 entries
        techniques.forEach { t ->
            if (t.related.size !in 2..3) add("${t.id}: related size ${t.related.size}")
            t.related.forEach { r ->
                if (r == t.id) add("${t.id}: relates to itself")
                if (r !in byId) add("${t.id}: unknown related $r")
            }
        }
        // 5. template iff TEMPLATE; completion rule references an existing block key
        techniques.forEach { t ->
            val isTemplate = t.exerciseType == ExerciseType.TEMPLATE.name
            if (isTemplate != (t.template != null)) add("${t.id}: template presence")
            t.template?.let { template ->
                val keys = template.blocks.map { it.key }
                if (keys.toSet().size != keys.size) add("${t.id}: duplicate block key")
                val referenced = when (val rule = template.completionRule) {
                    CompletionRuleDto.Always -> emptyList()
                    is CompletionRuleDto.RequireBlocks -> rule.keys
                    is CompletionRuleDto.RequireChecked -> listOf(rule.key)
                }
                referenced.filterNot { it in keys }.forEach { add("${t.id}: rule references $it") }
                val checked = template.completionRule as? CompletionRuleDto.RequireChecked
                if (checked != null &&
                    template.blocks.none { it.key == checked.key && it is BlockDto.Checklist }
                ) {
                    add("${t.id}: requireChecked needs a checklist")
                }
            }
            if (t.estimatedMinutes <= 0) add("${t.id}: minutes")
            if (t.reviewEligible != (t.id in REVIEW_ELIGIBLE)) add("${t.id}: reviewEligible")
        }
        // 6. every derived string key resolves
        CatalogKeys.stringKeys(catalog).filterNot(stringExists).forEach {
            add("missing string $it")
        }
        CatalogKeys.arrayKeys(catalog).filterNot(arrayExists).forEach { add("missing array $it") }
        // 7. curriculum days 1..14, known techniques, combination steps already introduced
        val days = catalog.curriculum.days
        if (days.map { it.day } != (1..days.size).toList() ||
            days.size != 14
        ) {
            add("curriculum days")
        }
        days.forEach { day ->
            val isCombination = day.combination.isNotEmpty()
            if (isCombination ==
                (day.newTechnique != null)
            ) {
                add("day ${day.day}: technique xor combination")
            }
            day.newTechnique?.let {
                if (it !in byId) add("day ${day.day}: unknown $it")
                if (byId[it]?.introDay != day.day) add("day ${day.day}: $it introDay mismatch")
            }
            day.combination.forEach { step ->
                val introDay = byId[step.technique]?.introDay
                if (step.technique !in byId) add("day ${day.day}: unknown step ${step.technique}")
                if (introDay != null &&
                    introDay > day.day
                ) {
                    add("day ${day.day}: ${step.technique} not met")
                }
            }
        }
        // 8. content versions match
        if (catalog.techniques.contentVersion !=
            catalog.curriculum.contentVersion
        ) {
            add("contentVersion")
        }
    }

    private val REVIEW_ELIGIBLE = setOf("feynman_technique", "spaced_repetition")
}
