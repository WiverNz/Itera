@file:Suppress("ktlint:standard:max-line-length")

package com.wivernz.itera.core.common

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

internal fun catalogue(locale: String): Map<String, Element> = buildMap {
    val directory = File("src/main/res/values${if (locale.isEmpty()) "" else "-$locale"}")
    directory.listFiles().orEmpty().filter { it.extension == "xml" }.forEach { file ->
        val children = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
            file
        ).documentElement.childNodes
        for (i in 0 until children.length) {
            val element = children.item(i) as? Element ?: continue
            if (element.tagName in setOf("string", "plurals", "string-array") &&
                element.getAttribute("translatable") != "false"
            ) {
                put(element.getAttribute("name"), element)
            }
        }
    }
}

class TranslationCompletenessTest {
    @Test fun everyShippingResourceExistsAndIsNonempty() {
        val base = catalogue("")
        listOf("ru", "de", "es").forEach { language ->
            val translated = catalogue(language)
            assertEquals(language, base.keys, translated.keys)
            translated.forEach { (key, value) ->
                assertTrue("$language/$key", value.textContent.isNotBlank())
            }
        }
    }
}

class PluralCategoryTest {
    @Test fun everyPluralHasRequiredCategories() {
        listOf("", "ru", "de", "es").forEach { language ->
            catalogue(language).filterValues {
                it.tagName == "plurals"
            }.forEach { (name, element) ->
                val nodes = element.getElementsByTagName("item")
                val categories = (0 until nodes.length).map {
                    (nodes.item(it) as Element).getAttribute("quantity")
                }.toSet()
                val required = if (language ==
                    "ru"
                ) {
                    setOf("one", "few", "many", "other")
                } else {
                    setOf("one", "other")
                }
                assertTrue("$language/$name: $categories", categories.containsAll(required))
            }
        }
    }
}

class NoConcatenationTest {
    @Test fun substitutionsArePositionalAndMatchAcrossLocales() {
        val positional = Regex("%(\\d+)\\$[dsf]")
        val invalid = Regex("%(?!\\d+\\$[dsf]|%)")
        val base = catalogue("")
        listOf("", "ru", "de", "es").forEach { language ->
            catalogue(language).forEach { (name, element) ->
                if (element.getAttribute("formatted") != "false") {
                    assertFalse(
                        "$language/$name: ${element.textContent}",
                        invalid.containsMatchIn(element.textContent)
                    )
                    assertEquals(
                        "$language/$name",
                        positional.findAll(base.getValue(name).textContent).map {
                            it.value
                        }.toSet(),
                        positional.findAll(element.textContent).map { it.value }.toSet()
                    )
                }
            }
        }
    }

    @Test fun featureDatesUseCentralFormatters() {
        val failures = File("src/main/java/com/wivernz/itera/feature").walkTopDown().filter {
            it.extension ==
                "kt"
        }
            .filter {
                it.readText().contains("DateTimeFormatter.ofPattern")
            }.map { it.name }.toList()
        assertEquals(emptyList<String>(), failures)
    }
}

class UntranslatedStringTest {
    // Proper names, shared vocabulary, units, and punctuation-only formats are intentionally identical.
    private val shared =
        setOf("app_name", "t_pomodoro_name", "t_deep_name", "t_premortem_name", "pace_standard", "export_markdown", "theme_system", "template_option_hint", "focus_pause", "sec_coach", "feel_okay", "voice_provisional", "minutes_short", "minutes_plus", "interval_days", "focus_plus5", "export_minutes", "hero_minutes_meta", "activity_reflection", "day_ring_count", "today_count", "shell_placeholder", "week_row_with_review")

    @Test fun noUnexpectedEnglishCopyRemains() {
        val base = catalogue("")
        listOf("ru", "de", "es").forEach { language ->
            val unchanged = catalogue(language).filter { (name, value) ->
                name !in shared &&
                    value.textContent == base.getValue(name).textContent
            }.keys
            assertEquals(language, emptySet<String>(), unchanged)
        }
    }
}

class UnreferencedStringTest {
    @Test fun allNotificationResourcesAreReferencedAndEveryStaticReferenceExists() {
        val resources = catalogue("").keys + setOf("exercise_chars", "hs_blank")
        val source = File("src/main/java").walkTopDown().filter {
            it.extension == "kt"
        }.joinToString("\n") { it.readText() }
        val references = Regex(
            "(?<!android\\.)R\\.(?:string|plurals)\\.(\\w+)"
        ).findAll(source).map {
            it.groupValues[1]
        }.toSet()
        assertEquals(emptySet<String>(), references - resources)
        assertEquals(
            emptySet<String>(),
            resources.filter { it.startsWith("notification_") }.toSet() - references
        )
    }
}
