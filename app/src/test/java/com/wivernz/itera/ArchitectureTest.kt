package com.wivernz.itera
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
class ArchitectureTest {
    private data class Rule(
        val name: String,
        val applies: (String) ->
        Boolean,
        val pattern: Regex
    )
    private val rules = listOf(
        Rule(
            "domain dependencies",
            { it.startsWith("domain/") },
            Regex(
"""^import (?:android\.|androidx\.|kotlinx\.serialization\.|javax\.inject\.(?!Inject\b)|com\.wivernz\.itera\.(?:data|feature)\.)"""
            )
        ),
        Rule(
            "feature dependencies",
            { it.startsWith("feature/") },
            Regex("""^import com\.wivernz\.itera\.data\.""")
        ),
        Rule(
            "data dependencies",
            { it.startsWith("data/") },
            Regex("""^import com\.wivernz\.itera\.feature\.""")
        ),
        Rule(
            "DAO domain types",
            { it.startsWith("data/database/dao/") },
            Regex("""(?:import |:.*)com\.wivernz\.itera\.domain\.""")
        ),
        Rule(
            "color literals",
            { it != "core/designsystem/theme/Color.kt" },
            Regex("""(?:#[0-9a-fA-F]{6}|0x[0-9a-fA-F]{6,8})\b""")
        ),
        Rule(
            "hardcoded composable text",
            {
                true
            },
            Regex("""\b(?:Text|contentDescription)\s*(?:\(\s*(?:text\s*=\s*)?|=\s*)"[^"\n]+"""")
        ),

        Rule(
            "ambient time",
            {
                it != "core/common/Clock.kt"
            },
            Regex(
                """(?:LocalDate|LocalTime|Instant)\.now\(\s*\)|System\.currentTimeMillis\(|Clock\.system(?:UTC|DefaultZone|\s*\()"""
            )
        ),
        Rule("date patterns", {
            !it.startsWith("core/common/time/")
        }, Regex("""DateTimeFormatter\.ofPattern\(""")),
        // milestone 012: only the adapter touches the platform recogniser
        Rule(
            "speech adapter boundary",
            { !it.startsWith("core/voice/") },
            Regex("""^import android\.speech\.""")
        ),
        // recognition and parsing never write storage or analytics; they reach business rules via screens
        Rule(
            "voice storage boundary",
            {
                it.startsWith("core/voice/") || it.startsWith("domain/voice/") ||
                    it.startsWith("feature/voice/")
            },
            Regex(
                """^import (?:androidx\.room\.|androidx\.datastore\.|com\.wivernz\.itera\.(?:data|analytics|domain\.repository|domain\.training|domain\.focus)\.)"""
            )
        )
    )
    private fun checkRule(index: Int, path: String, bad: String, good: String) {
        val rule = rules[index]
        fun errors(path: String, source: String) = source.lines()
            .mapIndexedNotNull {
                    line,
                    text
                ->

                if (rule.applies(path) &&
                    rule.pattern.containsMatchIn(text.trim())
                ) {
                    "$path:${line + 1}: ${rule.name}"
                } else {
                    null
                }
            }
        assertTrue("negative fixture ${rule.name}", errors(path, bad).isNotEmpty())
        assertTrue("positive fixture ${rule.name}", errors(path, good).isEmpty())
        val base = File("src/main/java/com/wivernz/itera")
        assertTrue(base.isDirectory)
        val errors = base.walkTopDown().filter {
            it.extension == "kt"
        }.flatMap {
            errors(
                it.relativeTo(base)
                    .invariantSeparatorsPath,
                it.readText()
            )
                .asSequence()
        }.toList()
        assertEquals(emptyList<String>(), errors)
    }

    @Test fun domainBoundary() = checkRule(
        0,
        "domain/model/Bad.kt",
        "import android.util.Log",
        "import java.time.Clock"
    )

    @Test fun featureBoundary() = checkRule(
        1,
        "feature/Bad.kt",
        "import com.wivernz.itera.data.Bad",
        "import com.wivernz.itera.domain.model.Skill"
    )

    @Test fun dataBoundary() = checkRule(
        2,
        "data/Bad.kt",
        "import com.wivernz.itera.feature.Bad",
        "import com.wivernz.itera.domain.model.Skill"
    )

    @Test fun daoBoundary() = checkRule(
        3,
        "data/database/dao/Bad.kt",
        "fun read(): com.wivernz.itera.domain.model.TrainingDay",
        "fun read(): TrainingDayEntity"
    )

    @Test fun colorBoundary() = checkRule(
        4,
        "feature/Bad.kt",
        "val c = Color(0xFF112233)",
        "val c = colors.accent"
    )

    @Test fun textBoundary() = checkRule(
        5,
        "feature/Bad.kt",
        "Text(\"Hello\")",
        "Text(stringResource(R.string.app_name))"
    )

    @Test fun clockBoundary() = checkRule(
        6,
        "feature/Bad.kt",
        "val now = Instant.now()",
        "val now = clock.instant()"
    )

    @Test fun speechAdapterBoundary() = checkRule(
        8,
        "feature/voice/Bad.kt",
        "import android.speech.SpeechRecognizer",
        "import com.wivernz.itera.core.voice.VoiceRecognizer"
    )

    @Test fun voiceStorageBoundary() = checkRule(
        9,
        "domain/voice/Bad.kt",
        "import com.wivernz.itera.domain.repository.TrainingPlanRepository",
        "import java.text.Normalizer"
    )

    @Test fun formatterBoundary() = checkRule(
        7,
        "feature/Bad.kt",
        "DateTimeFormatter.ofPattern(\"hh\")",
        "formatTime(time, locale, context)"
    )
}
