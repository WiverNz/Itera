package com.wivernz.itera.data.preferences
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
class PreferencesWriteGuardTest {
    private fun forbidden(path: String, source: String): Boolean {
        val allowed =
            path.startsWith("data/preferences/") || path == "domain/model/UserPreferences.kt" ||
                path.substringAfterLast('/') in setOf(
                    "AdvanceProgramDayUseCase.kt",
                    "ResetProgramUseCase.kt",
                    "EraseAllDataUseCase.kt"
                )
        return !allowed && (
            Regex("""\bcurrentProgramDay\s*=(?!=)""").containsMatchIn(source) ||
                source.contains("currentProgramDayKey") ||
                source.contains("\"current_program_day\"")
            )
    }

    @Test fun onlyPrivilegedWritersCanChangeProgramDay() {
        assertTrue(
            forbidden(
                "feature/Bad.kt",
                "prefs.update { it.copy(currentProgramDay = 9) }"
            )
        )
        assertTrue(
            forbidden(
                "data/Bad.kt",
                "intPreferencesKey(\"current_program_day\")"
            )
        )
        assertFalse(
            forbidden(
                "domain/training/AdvanceProgramDayUseCase.kt",
                "currentProgramDay = 2"
            )
        )
        assertFalse(
            forbidden(
                "feature/Good.kt",
                "val day = prefs.currentProgramDay"
            )
        )
        val root = File("src/main/java/com/wivernz/itera")
        root.walkTopDown().filter { it.extension == "kt" }.forEach {
            assertFalse(
                it.path,
                forbidden(
                    it.relativeTo(root).invariantSeparatorsPath,
                    it.readText()
                )
            )
        }
        assertTrue(
            File(
                root,
                "data/preferences/PreferenceKeys.kt"
            ).readText().contains("internal val currentProgramDayKey")
        )
    }
}
