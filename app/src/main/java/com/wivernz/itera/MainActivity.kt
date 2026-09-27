package com.wivernz.itera

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wivernz.itera.core.designsystem.component.ErrorState
import com.wivernz.itera.core.designsystem.component.ScreenColumn
import com.wivernz.itera.core.designsystem.theme.IteraTheme
import com.wivernz.itera.core.designsystem.theme.isDark
import com.wivernz.itera.core.navigation.AppDestination
import com.wivernz.itera.core.navigation.AppNavHost
import com.wivernz.itera.core.navigation.AppRoute
import com.wivernz.itera.core.navigation.Combination
import com.wivernz.itera.core.navigation.ExerciseResult
import com.wivernz.itera.core.navigation.FocusSession
import com.wivernz.itera.core.navigation.RouteCodec
import com.wivernz.itera.core.navigation.ShellViewModel
import com.wivernz.itera.core.voice.VoiceRecognizer
import com.wivernz.itera.feature.focus.FocusRestoreTarget
import com.wivernz.itera.feature.focus.FocusRestoreViewModel
import com.wivernz.itera.feature.voice.VoiceHost
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val shell: ShellViewModel by viewModels()
    private val focusRestore: FocusRestoreViewModel by viewModels()

    @Inject lateinit var voiceRecognizer: VoiceRecognizer
    @Inject lateinit var notificationDestination: com.wivernz.itera.core.notifications.NotificationDestination
    @Inject lateinit var reminders: com.wivernz.itera.core.notifications.ReminderScheduler
    @Inject lateinit var analytics: com.wivernz.itera.analytics.Analytics

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { shell.state.value.loading }
        if (savedInstanceState == null) {
            val hasDeepLink = intent.getStringExtra(RouteCodec.EXTRA) != null
            takeDeepLink(intent)
            // A notification tap already names its destination; otherwise re-attach a stored timer.
            if (!hasDeepLink) {
                focusRestore.restore { shell.acceptDeepLink(RouteCodec.encode(it.toRoute())) }
            }
        }
        enableEdgeToEdge()
        setContent {
            val state by shell.state.collectAsStateWithLifecycle()
            val pending by shell.pendingDeepLink.collectAsStateWithLifecycle()
            val preferences = state.preferences
            if (preferences != null) {
                IteraTheme(dark = preferences.theme.isDark()) {
                    VoiceHost(voiceRecognizer) {
                        AppNavHost(
                            preferences.onboardingCompleted,
                            pending,
                            shell::consumeDeepLink,
                            destination = { route, actions -> AppDestination(route, actions) }
                        )
                    }
                }
            } else if (state.failed) {
                IteraTheme {
                    ScreenColumn { ErrorState(stringResource(R.string.shell_load_failed)) }
                }
            }
        }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        takeDeepLink(intent)
    }

    private fun FocusRestoreTarget.toRoute(): AppRoute = when (this) {
        is FocusRestoreTarget.Session -> FocusSession(activityId, minutes, techniqueId)
        is FocusRestoreTarget.Result -> ExerciseResult(activityId, techniqueId)
        is FocusRestoreTarget.Chain -> Combination(parentActivityId)
    }

    private fun takeDeepLink(intent: Intent) {
        val encoded = intent.getStringExtra(RouteCodec.EXTRA)
        val type = intent.getStringExtra(com.wivernz.itera.core.notifications.IteraNotifier.TYPE_EXTRA)
        intent.removeExtra(RouteCodec.EXTRA)
        intent.removeExtra(com.wivernz.itera.core.notifications.IteraNotifier.TYPE_EXTRA)
        val route = encoded?.let(RouteCodec::decode) ?: return
        lifecycleScope.launch {
            val resolved = notificationDestination.resolve(route)
            shell.acceptDeepLink(RouteCodec.encode(resolved))
            com.wivernz.itera.analytics.NotificationType.entries.firstOrNull { it.name == type }?.let {
                analytics.track(com.wivernz.itera.analytics.Event.NotificationOpened(it))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch { reminders.rescheduleAll() }
    }
}

/** Font smoke-test fixture retained for the bootstrap preview tests. */
@Composable
fun BootstrapPlaceholder(fontResource: Int = R.font.inter) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.fillMaxSize().safeDrawingPadding(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    fontFamily = FontFamily(Font(fontResource))
                )
            }
        }
    }
}

class BootstrapFonts : PreviewParameterProvider<Int> {
    override val values = sequenceOf(
        R.font.bricolage_grotesque,
        R.font.instrument_sans,
        R.font.inter_tight,
        R.font.inter
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun BootstrapPreview(@PreviewParameter(BootstrapFonts::class) fontResource: Int) {
    BootstrapPlaceholder(fontResource)
}
