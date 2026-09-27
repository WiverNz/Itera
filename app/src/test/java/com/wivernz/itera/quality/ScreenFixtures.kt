@file:Suppress("ktlint:standard:no-wildcard-imports")
package com.wivernz.itera.quality

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.wivernz.itera.R
import com.wivernz.itera.core.designsystem.component.ScreenColumn
import com.wivernz.itera.core.designsystem.theme.NightSurface
import com.wivernz.itera.domain.model.*
import com.wivernz.itera.feature.onboarding.*
import com.wivernz.itera.feature.today.*
import com.wivernz.itera.feature.focus.*
import com.wivernz.itera.feature.reflection.*
import com.wivernz.itera.feature.daycomplete.*
import com.wivernz.itera.feature.train.*
import com.wivernz.itera.feature.progress.*
import com.wivernz.itera.feature.you.*
import com.wivernz.itera.feature.exercise.runner.*
import com.wivernz.itera.feature.exercise.eisenhower.*
import com.wivernz.itera.feature.exercise.feynman.*
import com.wivernz.itera.feature.exercise.habitstack.*
import com.wivernz.itera.feature.exercise.premortem.*
import com.wivernz.itera.feature.exercise.review.*
import com.wivernz.itera.feature.exercise.combination.*
import com.wivernz.itera.feature.voice.*
import com.wivernz.itera.domain.voice.VoiceLanguage
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

/** Synthetic, resource-backed content; no personal data or screenshot-only production paths. */
internal val auditScreens = listOf("Welcome", "Goals", "Rhythm", "FirstWeek", "Today", "Intro", "Result", "FocusSetup", "FocusTimer", "Reflection", "DayComplete", "EisenhowerEntry", "EisenhowerBoard", "Feynman", "FeynmanFeedback", "Review", "ReviewRevealed", "Premortem", "HabitStack", "Combination", "Train", "Library", "Technique", "Progress", "History", "You", "Topics", "VoicePermission", "VoiceDenied", "VoiceListening", "VoiceUnavailable")

@Composable
internal fun AuditScreen(name: String) {
    val title = stringResource(R.string.t_pomodoro_name)
    val technique = Technique(TechniqueId("pomodoro"), title, stringResource(R.string.t_pomodoro_short), stringResource(R.string.t_pomodoro_why), Skill.FOCUS, 2, ExerciseType.FOCUS_TIMER, 25, false, emptyList(), null, TechniqueDefaults(focusMinutes = 25))
    val eisenhower = EisenhowerActions({}, {}, {})
    val feynman = FeynmanActions({}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
    val today = LocalDate.of(2026, 9, 27)
    when (name) {
        "Welcome" -> WelcomeScreen(true, false, {}, {})
        "Goals" -> GoalsScreen(listOf(Skill.FOCUS, Skill.LEARNING), {}, {}, {})
        "Rhythm" -> RhythmScreen(LocalTime.of(8, 30), LocalTime.of(21, 0), TimeBudget.STANDARD, true, {}, {}, {}, {}, {})
        "FirstWeek" -> FirstWeekScreen((1..7).map { FirstWeekRow(it, "pomodoro", title, Skill.FOCUS, it == 4) }, false, {}, {})
        "Today" -> TodayScreen(TodayUiState(loading = false, hero = TodayHero.Exercise("pomodoro", Skill.FOCUS, title, technique.shortDescription, 25, true, TodayTarget.Exercise(1, "pomodoro"))), {}, {})
        "Intro" -> ExerciseIntroScreen(ExerciseRunnerUiState(loading = false, techniqueId = "pomodoro", name = title, skill = Skill.FOCUS, why = technique.explanation, task = technique.shortDescription), {}, {}, {})
        "Result" -> ExerciseResultScreen(ExerciseResultUiState(loading = false, techniqueId = "pomodoro", name = title), {}, {}, {})
        "FocusSetup", "FocusTimer" -> NightSurface {
            FocusScreen(FocusUiState(loading = false, timer = if (name == "FocusTimer") FocusTimerUi(title, 300, 1500, false) else null), true, {}, {}, {}, {}, {}, {}, {}, {}, {})
        }
        "Reflection" -> NightSurface { ReflectionScreen(ReflectionUiState(loading = false), { _, _ -> }, { _, _, _ -> }, {}, {}, {}) }
        "DayComplete" -> NightSurface { DayCompleteScreen(DayCompleteUiState(loading = false), {}) }
        "EisenhowerEntry", "EisenhowerBoard" -> EisenhowerScreen(EisenhowerUiState(loading = false, entering = name == "EisenhowerEntry"), {}, {}, eisenhower, {}, {})
        "Feynman", "FeynmanFeedback" -> FeynmanScreen(FeynmanUiState(loading = false, step = if (name == "Feynman") 0 else 1), feynman)
        "Review", "ReviewRevealed" -> ReviewScreen(ReviewUiState(loading = false, revealed = name == "ReviewRevealed", previousAnswer = if (name == "ReviewRevealed") "Synthetic prior answer" else null), {}, {}, {}, {})
        "Premortem" -> PremortemScreen(PremortemUiState(loading = false, failureDate = today.plusMonths(6)), PremortemActions({}, {}, {}, {}, {}, { _, _ -> }, {}, {}, {}, {}, {}))
        "HabitStack" -> HabitStackScreen(HabitStackUiState(loading = false), HabitStackActions({}, { _, _, _ -> }, { _, _ -> }, {}, {}, {}, {}))
        "Combination" -> CombinationScreen(CombinationUiState(loading = false), CombinationActions({}, eisenhower, { _, _ -> }, {}, {}, {}))
        "Train" -> TrainScreen(TrainUiState(loading = false, nodes = listOf(TrainNode(1, technique, TrainNodeState.TODAY))), {})
        "Library" -> LibraryScreen(LibraryUiState(rows = listOf(LibraryRow(technique, false, MasteryLevel.NONE))), {}, {})
        "Technique" -> TechniqueDetailScreen(TechniqueDetailUiState(technique = technique, loading = false), {}, {})
        "Progress" -> ProgressScreen(emptyProgress(), {}, {}, {})
        "History" -> HistoryScreen(HistoryUiState(YearMonth.from(today), today, historyDays(YearMonth.from(today), today.minusDays(7), today, emptyList(), emptyMap()), loading = false), {}, {}, {})
        "You" -> YouScreen(YouUiState(loading = false), {}, true)
        "Topics" -> TopicsScreen(emptyList(), {}, {})
        else -> {
            val voice = VoiceController(FakeRecognizer(), FakeGate(), VoiceLanguage.EN)
            val state = when (name) {
                "VoicePermission" -> VoiceSessionState.NeedsPermission(Unit)
                "VoiceDenied" -> VoiceSessionState.PermissionDenied(Unit, true)
                "VoiceListening" -> VoiceSessionState.Listening(Unit)
                else -> VoiceSessionState.Unavailable(Unit, VoiceUnavailable.DEVICE)
            }
            ScreenColumn { VoiceStatusPanel(voice, state, true, {}) }
        }
    }
}
