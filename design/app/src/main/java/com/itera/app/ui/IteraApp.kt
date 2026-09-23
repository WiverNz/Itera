package com.itera.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.itera.app.R
import com.itera.app.data.AppViewModel
import com.itera.app.model.ExerciseKind
import com.itera.app.model.Technique
import com.itera.app.ui.components.Divider
import com.itera.app.ui.components.IteraIcons
import com.itera.app.ui.screens.CombinationScreen
import com.itera.app.ui.screens.DayCompleteScreen
import com.itera.app.ui.screens.EisenhowerScreen
import com.itera.app.ui.screens.ExerciseIntroScreen
import com.itera.app.ui.screens.ExerciseResultScreen
import com.itera.app.ui.screens.FeynmanFeedbackScreen
import com.itera.app.ui.screens.FeynmanScreen
import com.itera.app.ui.screens.FirstWeekScreen
import com.itera.app.ui.screens.FocusScreen
import com.itera.app.ui.screens.GoalsScreen
import com.itera.app.ui.screens.HabitStackScreen
import com.itera.app.ui.screens.HistoryScreen
import com.itera.app.ui.screens.LibraryScreen
import com.itera.app.ui.screens.PremortemScreen
import com.itera.app.ui.screens.ProfileScreen
import com.itera.app.ui.screens.ProgressScreen
import com.itera.app.ui.screens.ReflectionScreen
import com.itera.app.ui.screens.ReviewScreen
import com.itera.app.ui.screens.RhythmScreen
import com.itera.app.ui.screens.TechniqueDetailScreen
import com.itera.app.ui.screens.TodayScreen
import com.itera.app.ui.screens.TrainScreen
import com.itera.app.ui.screens.TwoMinuteScreen
import com.itera.app.ui.screens.WelcomeScreen
import com.itera.app.ui.theme.Itera

object Routes {
    const val WELCOME = "welcome"
    const val GOALS = "goals"
    const val RHYTHM = "rhythm"
    const val FIRST_WEEK = "first-week"
    const val TODAY = "today"
    const val TRAIN = "train"
    const val PROGRESS = "progress"
    const val YOU = "you"
    const val LIBRARY = "library"
    const val HISTORY = "history"
    const val INTRO = "intro/{technique}"
    const val RESULT = "result/{technique}"
    const val DETAIL = "technique/{technique}"
    const val FOCUS = "focus/{minutes}?technique={technique}"
    const val TWO_MINUTE = "two-minute"
    const val EISENHOWER = "eisenhower"
    const val FEYNMAN = "feynman"
    const val FEYNMAN_FEEDBACK = "feynman-feedback"
    const val REVIEW = "review"
    const val PREMORTEM = "premortem"
    const val HABIT_STACK = "habit-stack"
    const val COMBINATION = "combination"
    const val REFLECTION = "reflection"
    const val DAY_COMPLETE = "day-complete"

    fun intro(t: Technique) = "intro/${t.name}"
    fun result(t: Technique) = "result/${t.name}"
    fun detail(t: Technique) = "technique/${t.name}"
    fun focus(minutes: Int, t: Technique? = null) = "focus/$minutes" + (t?.let { "?technique=${it.name}" } ?: "")

    /** Where the interactive part of a technique's exercise lives. */
    fun practice(t: Technique): String = when (t.kind) {
        ExerciseKind.TwoMinute -> TWO_MINUTE
        ExerciseKind.Focus -> focus(if (t == Technique.DeepWork) 50 else 25, t)
        ExerciseKind.Eisenhower -> EISENHOWER
        ExerciseKind.Feynman -> FEYNMAN
        ExerciseKind.Premortem -> PREMORTEM
        ExerciseKind.HabitStack -> HABIT_STACK
        ExerciseKind.Pareto -> COMBINATION
        ExerciseKind.Review -> REVIEW
        ExerciseKind.Reflection -> REFLECTION
        ExerciseKind.Generic -> result(t)
    }
}

private data class Tab(val route: String, val label: Int, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.TODAY, R.string.nav_today, IteraIcons.Today),
    Tab(Routes.TRAIN, R.string.nav_train, IteraIcons.Train),
    Tab(Routes.PROGRESS, R.string.nav_progress, IteraIcons.Progress),
    Tab(Routes.YOU, R.string.nav_you, IteraIcons.You),
)

/** Back to Today, clearing the flow screens above it. */
fun NavController.backToToday() {
    navigate(Routes.TODAY) {
        popUpTo(Routes.TODAY) { inclusive = true }
        launchSingleTop = true
    }
}

@Composable
fun IteraApp(vm: AppViewModel) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val showBar = tabs.any { it.route == route }
    val start = rememberSaveable { if (vm.onboarded) Routes.TODAY else Routes.WELCOME }

    Scaffold(
        containerColor = Itera.colors.bg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = { if (showBar) BottomBar(route) { target -> nav.switchTab(target) } },
    ) { padding ->
        val barPadding = PaddingValues(bottom = padding.calculateBottomPadding())
        NavHost(
            navController = nav,
            startDestination = start,
            modifier = Modifier.padding(barPadding).consumeWindowInsets(barPadding),
        ) {
            val techArg = navArgument("technique") { type = NavType.StringType }
            fun technique(e: androidx.navigation.NavBackStackEntry): Technique =
                Technique.valueOf(e.arguments?.getString("technique") ?: Technique.TwoMinute.name)

            // onboarding
            composable(Routes.WELCOME) {
                WelcomeScreen(
                    onStart = { nav.navigate(Routes.GOALS) },
                    onDemo = {
                        vm.loadDemo()
                        nav.navigate(Routes.TODAY) { popUpTo(Routes.WELCOME) { inclusive = true } }
                    },
                )
            }
            composable(Routes.GOALS) { GoalsScreen(vm, onBack = { nav.popBackStack() }, onNext = { nav.navigate(Routes.RHYTHM) }) }
            composable(Routes.RHYTHM) { RhythmScreen(vm, onBack = { nav.popBackStack() }, onNext = { nav.navigate(Routes.FIRST_WEEK) }) }
            composable(Routes.FIRST_WEEK) {
                FirstWeekScreen(onBack = { nav.popBackStack() }, onStart = {
                    vm.finishOnboarding()
                    nav.navigate(Routes.TODAY) { popUpTo(Routes.WELCOME) { inclusive = true } }
                })
            }

            // tabs
            composable(Routes.TODAY) {
                TodayScreen(
                    vm = vm,
                    onExercise = { t -> nav.navigate(Routes.intro(t)) },
                    onCombination = { nav.navigate(Routes.COMBINATION) },
                    onFocus = { minutes -> nav.navigate(Routes.focus(minutes)) },
                    onReflection = { nav.navigate(Routes.REFLECTION) },
                    onWrapUp = { nav.navigate(Routes.DAY_COMPLETE) },
                )
            }
            composable(Routes.TRAIN) {
                TrainScreen(
                    vm = vm,
                    onToday = { nav.switchTab(Routes.TODAY) },
                    onReview = { nav.navigate(Routes.REVIEW) },
                    onLibrary = { nav.navigate(Routes.LIBRARY) },
                    onTechnique = { t -> nav.navigate(Routes.detail(t)) },
                )
            }
            composable(Routes.PROGRESS) {
                ProgressScreen(vm, onHistory = { nav.navigate(Routes.HISTORY) }, onLibrary = { nav.navigate(Routes.LIBRARY) })
            }
            composable(Routes.YOU) { ProfileScreen(vm) }

            // secondary
            composable(Routes.LIBRARY) {
                LibraryScreen(vm, onBack = { nav.popBackStack() }, onTechnique = { t -> nav.navigate(Routes.detail(t)) })
            }
            composable(Routes.HISTORY) { HistoryScreen(vm, onBack = { nav.popBackStack() }) }
            composable(Routes.DETAIL, listOf(techArg)) { e ->
                val t = technique(e)
                TechniqueDetailScreen(
                    vm = vm,
                    technique = t,
                    onBack = { nav.popBackStack() },
                    onPractice = { nav.navigate(Routes.intro(t)) },
                    onTechnique = { other -> nav.navigate(Routes.detail(other)) },
                )
            }

            // exercise flow
            composable(Routes.INTRO, listOf(techArg)) { e ->
                val t = technique(e)
                ExerciseIntroScreen(
                    vm = vm,
                    technique = t,
                    onClose = { nav.popBackStack() },
                    onStart = { nav.navigate(Routes.practice(t)) },
                )
            }
            composable(Routes.RESULT, listOf(techArg)) { e ->
                ExerciseResultScreen(vm, technique(e), onDone = { nav.backToToday() })
            }
            composable(Routes.TWO_MINUTE) {
                TwoMinuteScreen(onClose = { nav.popBackStack() }, onFinish = { nav.navigate(Routes.result(Technique.TwoMinute)) })
            }
            composable(
                Routes.FOCUS,
                listOf(
                    navArgument("minutes") { type = NavType.IntType },
                    navArgument("technique") { type = NavType.StringType; nullable = true; defaultValue = null },
                ),
            ) { e ->
                val minutes = e.arguments?.getInt("minutes") ?: 25
                val t = e.arguments?.getString("technique")?.let { Technique.valueOf(it) }
                FocusScreen(minutes = minutes, onEnd = {
                    if (t != null) {
                        nav.navigate(Routes.result(t))
                    } else {
                        vm.complete(com.itera.app.model.DayStep.Focus, Technique.Pomodoro)
                        nav.popBackStack()
                    }
                })
            }
            composable(Routes.EISENHOWER) {
                EisenhowerScreen(onClose = { nav.popBackStack() }, onDone = { nav.navigate(Routes.result(Technique.Eisenhower)) })
            }
            composable(Routes.FEYNMAN) {
                FeynmanScreen(onClose = { nav.popBackStack() }, onDone = { nav.navigate(Routes.FEYNMAN_FEEDBACK) })
            }
            composable(Routes.FEYNMAN_FEEDBACK) {
                FeynmanFeedbackScreen(onBack = { nav.popBackStack() }, onDone = { nav.navigate(Routes.result(Technique.Feynman)) })
            }
            composable(Routes.REVIEW) {
                ReviewScreen(onClose = { nav.popBackStack() }, onDone = { nav.navigate(Routes.result(Technique.Spaced)) })
            }
            composable(Routes.PREMORTEM) {
                PremortemScreen(onClose = { nav.popBackStack() }, onDone = { nav.navigate(Routes.result(Technique.Premortem)) })
            }
            composable(Routes.HABIT_STACK) {
                HabitStackScreen(onClose = { nav.popBackStack() }, onDone = { nav.navigate(Routes.result(Technique.HabitStack)) })
            }
            composable(Routes.COMBINATION) {
                CombinationScreen(
                    vm = vm,
                    onClose = { nav.popBackStack() },
                    onContinue = {
                        if (vm.isCombinationDay) vm.markIntegrated(Technique.Eisenhower, Technique.Pareto, Technique.DeepWork)
                        nav.navigate(Routes.focus(50, vm.todaysTechnique ?: Technique.Pareto))
                    },
                )
            }

            // evening
            composable(Routes.REFLECTION) {
                ReflectionScreen(vm, onClose = { nav.popBackStack() }, onDone = {
                    nav.navigate(Routes.DAY_COMPLETE) { popUpTo(Routes.TODAY) }
                })
            }
            composable(Routes.DAY_COMPLETE) {
                DayCompleteScreen(vm, onGoodNight = {
                    vm.startNextDay()
                    nav.backToToday()
                })
            }
        }
    }
}

private fun NavController.switchTab(route: String) {
    navigate(route) {
        popUpTo(Routes.TODAY) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun BottomBar(current: String?, onSelect: (String) -> Unit) {
    val c = Itera.colors
    Column(Modifier.fillMaxWidth().background(c.bg)) {
        Divider()
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().height(68.dp).padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { tab ->
                val selected = tab.route == current
                Column(
                    Modifier
                        .widthIn(min = 72.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(role = Role.Tab) { onSelect(tab.route) }
                        .padding(vertical = 6.dp, horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(tab.icon, contentDescription = null, tint = if (selected) c.ink else c.ink2, modifier = Modifier.size(24.dp))
                    }
                    Text(
                        stringResource(tab.label),
                        style = Itera.type.caption.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium),
                        color = if (selected) c.ink else c.ink2,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
