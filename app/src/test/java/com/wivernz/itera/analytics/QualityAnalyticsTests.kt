package com.wivernz.itera.analytics

import com.wivernz.itera.data.database.entity.EventLogEntity
import com.wivernz.itera.domain.EngineHarness
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Balanced call extraction also catches multiline nested arguments. */
internal fun privacyViolations(source: String): List<String> {
    val starts = Regex("(?:analytics\\.(?:track|append)|[Ll]ogger\\.[vdiwe]|Log\\.[vdiwe]|println)\\s*\\(")
    return starts.findAll(source).mapNotNull { match ->
        var depth = 1
        var end = match.range.last + 1
        while (end < source.length && depth > 0) {
            when (source[end++]) { '(' -> depth++; ')' -> depth-- }
        }
        val call = source.substring(match.range.first, end)
        val withoutBooleans = call.replace(Regex("!?[A-Za-z.]*note\\?*\\.isNullOrBlank\\(\\)"), "false")
            .replace(Regex("hasNote\\s*=\\s*[^,\\n]+"), "hasNote = false")
        if (Regex("\\b(note|explanation|answer|taskLabel|anchor|habit|projectName|displayName|transcript|partial|sourceAnswer)\\b").containsMatchIn(withoutBooleans)) call else null
    }.toList()
}

class NoUserTextLoggedTest {
    @Test fun productionDoesNotSendUserTextToLogsOrAnalytics() {
        val failures = File("src/main/java").walkTopDown().filter { it.extension == "kt" }
            .flatMap { file -> privacyViolations(file.readText()).map { "${file.name}: $it" } }.toList()
        assertEquals(emptyList<String>(), failures)
    }
    @Test fun deliberateMultilineViolationsAndVoiceTranscriptFail() {
        listOf("analytics.track(Event.Bad(\n note\n))", "logger.d(TAG, taskLabel)", "Logger.w(TAG, displayName)", "Log.i(TAG, transcript)", "println(answer)").forEach {
            assertTrue(it, privacyViolations(it).isNotEmpty())
        }
        assertTrue(privacyViolations("analytics.track(Event.Completed(hasNote = !note.isNullOrBlank()))").isEmpty())
    }
    @Test fun eventCatalogueAcceptsNoArbitraryStrings() {
        val catalogue = File("src/main/java/com/wivernz/itera/analytics/Event.kt").readText()
        assertFalse(Regex("val \\w+: String[?,)]").containsMatchIn(catalogue.substringBefore("enum class AnalyticsTechnique")))
        val manifest = File("src/main/AndroidManifest.xml").readText()
        assertFalse(manifest.contains("android.permission.INTERNET"))
        assertFalse(manifest.contains("EXACT_ALARM"))
    }
}

@RunWith(RobolectricTestRunner::class)
class EventLogTrimTest {
    @Test fun newestTwoThousandSurviveIncludingTimestampTies() = runBlocking {
        val h = EngineHarness()
        try {
            repeat(2500) { h.db.eventLogDao().insert(EventLogEntity(timestamp = 100, name = "fixture", params = "{}")) }
            h.db.eventLogDao().trimTo(2000)
            val rows = h.db.eventLogDao().exportSince(0)
            assertEquals(2000, rows.size)
            assertEquals(501L, rows.first().id)
            assertEquals(2500L, rows.last().id)
            h.db.eventLogDao().trimTo(2000)
            assertEquals(rows, h.db.eventLogDao().exportSince(0))
        } finally { h.close() }
    }
}

@RunWith(RobolectricTestRunner::class)
class ResetAnalyticsTest {
    @Test fun programKeepsDiagnosticRowsAndEraseDeletesThem() = runBlocking {
        val h = EngineHarness()
        try {
            h.analytics.append(Event.OnboardingStarted)
            h.resetProgram()
            assertTrue(h.eventNames().contains("onboarding_started"))
            h.eraseAll()
            assertTrue(h.eventNames().isEmpty())
        } finally { h.close() }
    }
}

