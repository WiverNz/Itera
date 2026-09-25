package com.wivernz.itera.core.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.wivernz.itera.R
import com.wivernz.itera.core.designsystem.component.Divider
import com.wivernz.itera.core.designsystem.component.ScreenColumn
import com.wivernz.itera.core.designsystem.component.TopBar
import com.wivernz.itera.core.designsystem.icon.IteraIcons
import com.wivernz.itera.core.designsystem.theme.Itera
import com.wivernz.itera.core.designsystem.theme.LocalReduceMotion
import com.wivernz.itera.core.designsystem.theme.NightSurface
import com.wivernz.itera.core.designsystem.theme.colors

/** Screens receive commands only; this file exclusively owns the navigation controller. */
class NavigationActions(
    val navigate: (AppRoute) -> Unit,
    val back: () -> Unit,
    val finishOnboarding: () -> Unit,
    val backToToday: () -> Unit,
    val showResult: (ExerciseResult) -> Unit
)

/** Injectable destination content keeps shell tests independent of feature repositories. */
@Composable
fun AppNavHost(
    onboardingCompleted: Boolean,
    pendingDeepLink: String? = null,
    onDeepLinkConsumed: (String) -> Unit = {},
    destination: @Composable (
        AppRoute,
        NavigationActions
    ) -> Unit = { route, actions -> PlaceholderRoute(route, actions) }
) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val startCompleted = rememberSaveable { onboardingCompleted }
    val current = entry?.destination
    val tab = when {
        current?.hasRoute<Today>() == true -> Today
        current?.hasRoute<Train>() == true -> Train
        current?.hasRoute<Progress>() == true -> Progress
        current?.hasRoute<You>() == true -> You
        else -> null
    }
    val actions = remember(nav) {
        NavigationActions(
            navigate = { nav.navigate(it) },
            back = { nav.popBackStack() },
            finishOnboarding = {
                nav.navigate(Today) {
                    popUpTo<Welcome> { inclusive = true }
                    launchSingleTop =
                        true
                }
            },
            backToToday = { nav.backToToday() },
            showResult = { result ->
                nav.navigate(result) {
                    popUpTo(ExerciseIntro(result.activityId, result.technique)) { inclusive = true }
                }
            }
        )
    }
    LaunchedEffect(pendingDeepLink, onboardingCompleted, entry != null) {
        if (pendingDeepLink != null && entry != null && onboardingCompleted) {
            val target = RouteCodec.decode(pendingDeepLink)
            if (target != null) {
                nav.navigate(Today) {
                    popUpTo(nav.graph.id) { inclusive = false }
                    launchSingleTop =
                        true
                }
                if (target != Today) nav.navigate(target) { launchSingleTop = true }
            }
            onDeepLinkConsumed(pendingDeepLink)
        }
    }
    Scaffold(
        containerColor = Itera.colors.bg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = { if (tab != null) IteraBottomBar(tab, { nav.switchTab(it) }) }
    ) { padding ->
        val reduced = LocalReduceMotion.current
        NavHost(
            navController = nav,
            startDestination = if (startCompleted) Today else Welcome,
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
            enterTransition = { if (reduced) EnterTransition.None else fadeIn() },
            exitTransition = { if (reduced) ExitTransition.None else fadeOut() },
            popEnterTransition = { if (reduced) EnterTransition.None else fadeIn() },
            popExitTransition = { if (reduced) ExitTransition.None else fadeOut() }
        ) {
            composable<Welcome> { entry ->
                OnboardingScope(nav, entry) { destination(Welcome, actions) }
            }
            composable<Goals> { entry ->
                OnboardingScope(nav, entry) { destination(Goals, actions) }
            }
            composable<Rhythm> { entry ->
                OnboardingScope(nav, entry) { destination(Rhythm, actions) }
            }
            composable<FirstWeek> { entry ->
                OnboardingScope(nav, entry) { destination(FirstWeek, actions) }
            }
            composable<Today> { destination(Today, actions) }
            composable<Train> { destination(Train, actions) }
            composable<Progress> { destination(Progress, actions) }
            composable<You> { destination(You, actions) }
            composable<Library> { destination(Library, actions) }
            composable<History> { destination(History, actions) }
            composable<TwoMinute> { entry ->
                destination(entry.toRoute<TwoMinute>(), actions)
            }
            composable<Eisenhower> { entry ->
                destination(entry.toRoute<Eisenhower>(), actions)
            }
            composable<Feynman> { entry ->
                destination(entry.toRoute<Feynman>(), actions)
            }
            composable<FeynmanFeedback> { entry ->
                destination(entry.toRoute<FeynmanFeedback>(), actions)
            }
            composable<Review> { entry ->
                destination(entry.toRoute<Review>(), actions)
            }
            composable<Premortem> { entry ->
                destination(entry.toRoute<Premortem>(), actions)
            }
            composable<HabitStack> { entry ->
                destination(entry.toRoute<HabitStack>(), actions)
            }
            composable<Combination> { entry ->
                destination(entry.toRoute<Combination>(), actions)
            }
            composable<Reflection> { entry ->
                NightSurface { destination(entry.toRoute<Reflection>(), actions) }
            }
            composable<TechniqueDetail> { entry ->
                destination(entry.toRoute<TechniqueDetail>(), actions)
            }
            composable<ExerciseIntro> { entry ->
                destination(entry.toRoute<ExerciseIntro>(), actions)
            }
            composable<ExerciseResult> { entry ->
                BackHandler { actions.backToToday() }
                destination(entry.toRoute<ExerciseResult>(), actions)
            }
            composable<FocusSession> { entry ->
                NightSurface { destination(entry.toRoute<FocusSession>(), actions) }
            }
            composable<DayComplete> { entry ->
                BackHandler { actions.backToToday() }
                NightSurface { destination(entry.toRoute<DayComplete>(), actions) }
            }
        }
    }
}

/** The onboarding flow shares one view model, owned by the Welcome entry at the bottom of the flow. */
val LocalOnboardingOwner = staticCompositionLocalOf<ViewModelStoreOwner?> { null }

@Composable
private fun OnboardingScope(
    nav: NavHostController,
    entry: NavBackStackEntry,
    content: @Composable () -> Unit
) {
    val owner = remember(entry) {
        runCatching { nav.getBackStackEntry<Welcome>() }.getOrNull() ?: entry
    }
    CompositionLocalProvider(LocalOnboardingOwner provides owner, content = content)
}

private fun NavHostController.switchTab(route: AppRoute) {
    if (currentDestination?.hasRoute(route::class) == true) return
    navigate(route) {
        popUpTo<Today> { saveState = true }
        launchSingleTop = true
        restoreState =
            true
    }
}

private fun NavHostController.backToToday() {
    navigate(Today) {
        popUpTo<Today> { inclusive = true }
        launchSingleTop = true
    }
}

@Composable
fun IteraBottomBar(current: AppRoute, onSelect: (AppRoute) -> Unit, modifier: Modifier = Modifier) {
    val c = Itera.colors
    val tabs = listOf(
        Triple(Today, R.string.nav_today, IteraIcons.Today),
        Triple(Train, R.string.nav_train, IteraIcons.Train),
        Triple(Progress, R.string.nav_progress, IteraIcons.Progress),
        Triple(You, R.string.nav_you, IteraIcons.You)
    )
    Column(modifier.fillMaxWidth().background(c.bg).testTag("BottomBar")) {
        com.wivernz.itera.core.designsystem.component.Divider()
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().height(
                68.dp
            ).padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { (route, label, icon) ->
                val selected = route == current
                Column(
                    Modifier.widthIn(min = 72.dp).clip(RoundedCornerShape(16.dp))
                        .selectable(selected, role = Role.Tab) { onSelect(route) }
                        .testTag("Tab" + route::class.simpleName)
                        .padding(vertical = 6.dp, horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        icon,
                        null,
                        tint = if (selected) c.ink else c.ink2,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        stringResource(label),
                        style = Itera.type.caption.copy(
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                        ),
                        color = if (selected) c.ink else c.ink2,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
internal fun PlaceholderRoute(route: AppRoute, actions: NavigationActions) {
    val name = route::class.simpleName.orEmpty()
    ScreenColumn(modifier = Modifier.testTag(name)) {
        if (route !in listOf(Welcome, Today, Train, Progress, You)) {
            TopBar(
                stringResource(R.string.shell_placeholder, name),
                if (route is ExerciseResult ||
                    route is DayComplete
                ) {
                    actions.backToToday
                } else {
                    actions.back
                },
                icon = IteraIcons.Back
            )
        }
        Text(
            stringResource(R.string.shell_placeholder, name),
            style = Itera.type.display,
            color = Itera.colors.ink
        )
    }
}
