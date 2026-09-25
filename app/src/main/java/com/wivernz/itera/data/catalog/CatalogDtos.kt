package com.wivernz.itera.data.catalog

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** `techniques.v1.json`, docs/data/04-technique-catalog-format.md section 1. */
@Serializable
data class TechniquesFileDto(val contentVersion: Int, val techniques: List<TechniqueDto>)

@Serializable
data class TechniqueDto(
    val id: String,
    val skill: String,
    val introDay: Int? = null,
    val exerciseType: String,
    val estimatedMinutes: Int,
    val reviewEligible: Boolean,
    val retired: Boolean = false,
    val related: List<String>,
    val defaults: DefaultsDto,
    val template: TemplateDto? = null
)

@Serializable
data class DefaultsDto(val focusMinutes: Int? = null, val breakMinutes: Int? = null)

@Serializable
data class TemplateDto(val completionRule: CompletionRuleDto, val blocks: List<BlockDto>)

@Serializable
sealed interface CompletionRuleDto {
    @Serializable
    @SerialName("always")
    data object Always : CompletionRuleDto

    @Serializable
    @SerialName("requireBlocks")
    data class RequireBlocks(val keys: List<String>) : CompletionRuleDto

    @Serializable
    @SerialName("requireChecked")
    data class RequireChecked(val key: String, val count: Int) : CompletionRuleDto
}

@Serializable
sealed interface BlockDto {
    val key: String

    @Serializable
    @SerialName("instruction")
    data class Instruction(override val key: String, val emphasis: Boolean = false) : BlockDto

    @Serializable
    @SerialName("textInput")
    data class TextInput(override val key: String, val minLines: Int = 1, val maxLines: Int = 4) :
        BlockDto

    @Serializable
    @SerialName("checklist")
    data class Checklist(
        override val key: String,
        val minItems: Int,
        val maxItems: Int,
        val withStopwatch: Boolean = false
    ) : BlockDto

    @Serializable
    @SerialName("pickOne")
    data class PickOne(
        override val key: String,
        val itemCount: Int,
        val suggestionsKey: String? = null
    ) : BlockDto

    @Serializable
    @SerialName("twoLists")
    data class TwoLists(override val key: String, val primaryCount: Int, val secondaryCount: Int) :
        BlockDto

    @Serializable
    @SerialName("chipSelect")
    data class ChipSelect(
        override val key: String,
        val optionsKey: String,
        val allowCustom: Boolean = true,
        val maxSelections: Int = 1
    ) : BlockDto
}

/** `curriculum.v1.json`, section 2. */
@Serializable
data class CurriculumFileDto(val contentVersion: Int, val days: List<CurriculumDayDto>)

@Serializable
data class CurriculumDayDto(
    val day: Int,
    val newTechnique: String? = null,
    val combination: List<CombinationStepDto> = emptyList(),
    val weeklyLookBack: Boolean = false
)

@Serializable
data class CombinationStepDto(val technique: String)

/** Both parsed files. */
data class ParsedCatalog(val techniques: TechniquesFileDto, val curriculum: CurriculumFileDto)

class CatalogException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Pure JVM parsing; `ignoreUnknownKeys` so an additive field never breaks an older build (section 5). */
object CatalogParser {
    const val TECHNIQUES_ASSET = "catalog/techniques.v1.json"
    const val CURRICULUM_ASSET = "catalog/curriculum.v1.json"

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    fun parse(techniquesJson: String, curriculumJson: String): ParsedCatalog = try {
        ParsedCatalog(
            json.decodeFromString(TechniquesFileDto.serializer(), techniquesJson),
            json.decodeFromString(CurriculumFileDto.serializer(), curriculumJson)
        )
    } catch (e: IllegalArgumentException) {
        // SerializationException is an IllegalArgumentException. Its message names the JSON path only.
        throw CatalogException("Technique catalog is malformed: ${e.message}", e)
    }
}
