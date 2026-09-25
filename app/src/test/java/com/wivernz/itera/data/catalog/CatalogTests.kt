package com.wivernz.itera.data.catalog

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import com.wivernz.itera.data.fileAssets
import com.wivernz.itera.domain.MemoryCatalogCache
import com.wivernz.itera.domain.model.CompletionRule
import com.wivernz.itera.domain.model.ExerciseBlock
import com.wivernz.itera.domain.model.ExerciseType
import com.wivernz.itera.domain.testCatalogRepository
import java.io.File
import java.util.Locale
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private fun shipped(): ParsedCatalog = CatalogParser.parse(
    fileAssets.read(CatalogParser.TECHNIQUES_ASSET),
    fileAssets.read(CatalogParser.CURRICULUM_ASSET)
)

/** The eight assertions of docs/data/04 section 6, against the real assets. */
@RunWith(RobolectricTestRunner::class)
class CatalogValidationTest {
    @Test fun shippedCatalogPassesEveryAssertionInAllFourLanguages() {
        val catalog = shipped()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val names = mutableSetOf<String>()
        listOf("en", "ru", "de", "es").forEach { tag ->
            val localized = context.createConfigurationContext(
                Configuration(context.resources.configuration).apply {
                    setLocale(Locale.forLanguageTag(tag))
                }
            )
            val strings = ResourceCatalogStrings(localized)
            assertEquals(
                tag,
                emptyList<String>(),
                CatalogValidator.validate(catalog, { strings.text(it) != null }, {
                    strings.array(it) !=
                        null
                })
            )
            CatalogKeys.stringKeys(catalog).forEach { key ->
                val text = checkNotNull(strings.text(key))
                assertTrue("$tag $key", text.isNotBlank() && !text.contains("TODO"))
            }
            names += checkNotNull(strings.text(CatalogKeys.why("pomodoro")))
        }
        assertEquals("translations differ per language", 4, names.size)
    }

    @Test fun exactPrototypeValues() {
        val byId = shipped().techniques.techniques.associateBy { it.id }
        val minutes = mapOf(
            "two_minute_rule" to 5, "pomodoro" to 30, "eisenhower_matrix" to 10,
            "five_second_rule" to 2,
            "habit_stacking" to 5, "feynman_technique" to 15, "two_list_strategy" to 10,
            "deep_work" to 50,
            "pareto_principle" to 10, "spaced_repetition" to 5, "information_diet" to 5,
            "premortem" to 10,
            "one_percent_improvement" to 5, "daily_reflection" to 2
        )
        minutes.forEach { (id, m) -> assertEquals(id, m, byId.getValue(id).estimatedMinutes) }
        assertEquals(
            listOf("five_second_rule", "habit_stacking", "eisenhower_matrix"),
            byId.getValue("two_minute_rule").related
        )
        assertEquals(
            listOf("feynman_technique", "habit_stacking", "daily_reflection"),
            byId.getValue("spaced_repetition").related
        )
        assertEquals(
            listOf("premortem", "one_percent_improvement", "feynman_technique"),
            byId.getValue("daily_reflection").related
        )
        // exerciseType table of docs/engine/04-unlock-rules.md section 2
        val types = mapOf(
            "daily_reflection" to "REFLECTION", "two_minute_rule" to "TEMPLATE",
            "pomodoro" to "FOCUS_TIMER",
            "eisenhower_matrix" to "EISENHOWER", "five_second_rule" to "TEMPLATE",
            "habit_stacking" to "HABIT_STACK",
            "feynman_technique" to "FEYNMAN", "two_list_strategy" to "TEMPLATE",
            "deep_work" to "FOCUS_TIMER",
            "pareto_principle" to "TEMPLATE", "spaced_repetition" to "TEMPLATE",
            "information_diet" to "TEMPLATE",
            "premortem" to "PREMORTEM", "one_percent_improvement" to "TEMPLATE"
        )
        types.forEach { (id, t) -> assertEquals(id, t, byId.getValue(id).exerciseType) }
        val days = shipped().curriculum.days
        assertEquals((1..14).toList(), days.map { it.day })
        assertEquals(listOf(7, 14), days.filter { it.weeklyLookBack }.map { it.day })
        assertEquals(
            listOf("eisenhower_matrix", "pareto_principle", "deep_work", "daily_reflection"),
            days.last().combination.map { it.technique }
        )
    }

    @Test fun validatorCatchesEachViolation() {
        val good = shipped()
        val t = good.techniques.techniques
        fun broken(transform: (List<TechniqueDto>) -> List<TechniqueDto>) =
            CatalogValidator.validate(
                good.copy(techniques = good.techniques.copy(techniques = transform(t)))
            )
        assertTrue(CatalogValidator.validate(good).isEmpty())
        assertTrue(broken { it.drop(1) }.isNotEmpty())
        assertTrue(
            broken { l ->
                l.map {
                    if (it.id ==
                        "pomodoro"
                    ) {
                        it.copy(introDay = 3)
                    } else {
                        it
                    }
                }
            }.isNotEmpty()
        )
        assertTrue(
            broken { l ->
                l.map {
                    if (it.id ==
                        "pomodoro"
                    ) {
                        it.copy(skill = "PLANNING")
                    } else {
                        it
                    }
                }
            }.isNotEmpty()
        )
        assertTrue(
            broken { l ->
                l.map {
                    if (it.id ==
                        "pomodoro"
                    ) {
                        it.copy(related = listOf("pomodoro", "x"))
                    } else {
                        it
                    }
                }
            }.isNotEmpty()
        )
        assertTrue(
            broken { l ->
                l.map {
                    if (it.id ==
                        "pomodoro"
                    ) {
                        it.copy(template = t[1].template)
                    } else {
                        it
                    }
                }
            }.isNotEmpty()
        )
        assertTrue(
            broken { l ->
                l.map {
                    if (it.id == "two_minute_rule") {
                        it.copy(
                            template = it.template!!.copy(
                                completionRule = CompletionRuleDto.RequireBlocks(listOf("nope"))
                            )
                        )
                    } else {
                        it
                    }
                }
            }.isNotEmpty()
        )
        assertTrue(
            CatalogValidator.validate(good, stringExists = {
                !it.startsWith("t_deep")
            }).isNotEmpty()
        )
        val curriculum = good.curriculum
        assertTrue(
            CatalogValidator.validate(
                good.copy(curriculum = curriculum.copy(days = curriculum.days.drop(1)))
            ).isNotEmpty()
        )
        assertTrue(
            CatalogValidator.validate(
                good.copy(curriculum = curriculum.copy(contentVersion = 2))
            ).isNotEmpty()
        )
    }
}

class CatalogParsingTest {
    @Test fun malformedJsonFails() {
        assertThrows(CatalogException::class.java) { CatalogParser.parse("{", "{}") }
    }

    @Test fun missingRequiredFieldFails() {
        val missingSkill = """
            {"contentVersion":1,"techniques":[{"id":"x","exerciseType":"TEMPLATE",
            "estimatedMinutes":1,"reviewEligible":false,"related":[],"defaults":{}}]}
        """
        assertThrows(CatalogException::class.java) {
            CatalogParser.parse(missingSkill, """{"contentVersion":1,"days":[]}""")
        }
    }

    @Test fun unknownExtraFieldIsIgnored() {
        val extra = """
            {"contentVersion":1,"future":true,"techniques":[{"id":"x","skill":"FOCUS",
            "exerciseType":"TEMPLATE","estimatedMinutes":1,"reviewEligible":false,"related":[],
            "defaults":{"color":"red"},"template":{"completionRule":{"type":"always"},
            "blocks":[{"type":"textInput","key":"a","hint":1}]}}]}
        """
        val days = """{"contentVersion":1,"days":[{"day":1,"newTechnique":"x","mood":1}]}"""
        val parsed = CatalogParser.parse(extra, days)
        assertEquals("x", parsed.techniques.techniques.single().id)
        assertTrue(
            parsed.techniques.techniques.single().template!!.blocks.single() is BlockDto.TextInput
        )
    }

    @Test fun mapsTemplatesToTheDomain() = runTest {
        val techniques = testCatalogRepository().catalog().associateBy { it.id.value }
        val two = checkNotNull(techniques.getValue("two_minute_rule").template)
        assertEquals(CompletionRule.RequireChecked("tasks", 2), two.completionRule)
        val checklist = two.blocks[1] as ExerciseBlock.Checklist
        assertTrue(checklist.withStopwatch)
        assertEquals(2, checklist.minItems)
        assertEquals("exercise_two_minute_rule_tasks_placeholder", checklist.addItemLabel)
        assertEquals(
            CompletionRule.Always,
            techniques.getValue("five_second_rule").template!!.completionRule
        )
        assertTrue(
            techniques.getValue(
                "pareto_principle"
            ).template!!.blocks.single() is ExerciseBlock.PickOne
        )
        assertTrue(
            techniques.values.filter { it.exerciseType == ExerciseType.TEMPLATE }.all {
                it.template !=
                    null
            }
        )
        assertEquals(25, techniques.getValue("pomodoro").defaults.focusMinutes)
        val steps = testCatalogRepository().curriculum().days.last().combination
        assertEquals("combination_day14_step3_prompt", steps[2].prompt)
    }
}

/**
 * The four forbidden changes of docs/data/05 section 3, against the committed snapshot of the last
 * released catalogue (`src/test/resources/catalog-snapshot/techniques.v1.json`).
 */
class CatalogCompatibilityTest {
    private fun ids(json: String): Map<String, JsonObject> =
        Json.parseToJsonElement(json).jsonObject.getValue("techniques").jsonArray
            .associate { it.jsonObject.getValue("id").jsonPrimitive.content to it.jsonObject }

    private fun blockKeys(t: JsonObject): Set<String> =
        t["template"]?.jsonObject?.get("blocks")?.jsonArray.orEmpty()
            .map { it.jsonObject.getValue("key").jsonPrimitive.content }.toSet()

    private fun violations(previous: String, current: String): List<String> {
        val before = ids(previous)
        val after = ids(current)
        return buildList {
            before.forEach { (id, old) ->
                val new = after[id]
                if (new == null) {
                    add("removed $id")
                    return@forEach
                }
                if (old["skill"] != new["skill"]) add("skill changed $id")
                (blockKeys(old) - blockKeys(new)).forEach { add("block key renamed $id.$it") }
            }
            // a renamed id shows up as a removal; a reused id is caught by the skill check
        }
    }

    private val snapshot = File("src/test/resources/catalog-snapshot/techniques.v1.json").readText()
    private val current = fileAssets.read(CatalogParser.TECHNIQUES_ASSET)

    @Test fun shippedCatalogIsCompatibleWithTheSnapshot() {
        assertEquals(emptyList<String>(), violations(snapshot, current))
    }

    @Test fun detectsEachForbiddenChange() {
        assertTrue(
            violations(snapshot, current.replace("\"LEARNING\"", "\"FOCUS\"")).any {
                it.startsWith("skill")
            }
        )
        assertTrue(
            violations(
                snapshot,
                current.replace("\"id\": \"premortem\"", "\"id\": \"pre_mortem\"")
            ).any {
                it.startsWith("removed")
            }
        )
        assertTrue(
            violations(snapshot, current.replace("\"key\": \"tasks\"", "\"key\": \"todo\"")).any {
                it.startsWith("block key")
            }
        )
        val removed = Json.parseToJsonElement(current).jsonObject.let { root ->
            JsonObject(
                root +
                    (
                        "techniques" to
                            kotlinx.serialization.json.JsonArray(
                                root.getValue("techniques").jsonArray.drop(1)
                            )
                        )
            )
        }.toString()
        assertTrue(violations(snapshot, removed).any { it.startsWith("removed") })
    }
}

class CatalogCachingTest {
    @Test fun assetsAreReadOncePerProcess() = runTest {
        val reads = mutableMapOf<String, Int>()
        val counting = CatalogAssetSource { path ->
            reads[path] = (reads[path] ?: 0) + 1
            fileAssets.read(path)
        }
        val repository = testCatalogRepository(counting)
        repeat(100) {
            repository.catalog()
            repository.curriculum()
            repository.technique(com.wivernz.itera.domain.id("pomodoro"))
        }
        assertEquals(
            mapOf(CatalogParser.TECHNIQUES_ASSET to 1, CatalogParser.CURRICULUM_ASSET to 1),
            reads
        )
    }

    @Test fun malformedCatalogFailsFastInDebugAndFallsBackInRelease() = runTest {
        val broken = CatalogAssetSource { "{" }
        assertThrows(CatalogException::class.java) {
            kotlinx.coroutines.runBlocking {
                testCatalogRepository(broken, failFast = true).catalog()
            }
        }
        // release, no last good copy: blocking failure
        assertThrows(CatalogException::class.java) {
            kotlinx.coroutines.runBlocking {
                testCatalogRepository(broken, failFast = false).catalog()
            }
        }
        // release with a last good copy from an earlier successful parse
        val cache = MemoryCatalogCache()
        testCatalogRepository(fileAssets, cache = cache).catalog()
        assertNotNull(cache.entry)
        assertEquals(
            14,
            testCatalogRepository(broken, failFast = false, cache = cache).catalog().size
        )
    }
}
