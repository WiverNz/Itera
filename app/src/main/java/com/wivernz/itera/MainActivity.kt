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
import com.wivernz.itera.core.designsystem.theme.IteraTheme
import com.wivernz.itera.core.designsystem.theme.isDark
import com.wivernz.itera.core.navigation.AppNavHost
import com.wivernz.itera.core.navigation.RouteCodec
import com.wivernz.itera.core.navigation.ShellViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val shell: ShellViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { shell.state.value.loading }
        if (savedInstanceState == null) takeDeepLink(intent)
        enableEdgeToEdge()
        setContent {
            val state by shell.state.collectAsStateWithLifecycle()
            val pending by shell.pendingDeepLink.collectAsStateWithLifecycle()
            val preferences = state.preferences
            if (preferences != null) {
                IteraTheme(dark = preferences.theme.isDark()) {
                    AppNavHost(preferences.onboardingCompleted, pending, shell::consumeDeepLink)
                }
            } else if (state.failed) {
                IteraTheme { ErrorState(stringResource(R.string.shell_load_failed)) }
            }
        }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        takeDeepLink(intent)
    }

    private fun takeDeepLink(intent: Intent) {
        shell.acceptDeepLink(intent.getStringExtra(RouteCodec.EXTRA))
        intent.removeExtra(RouteCodec.EXTRA)
    }
}

/** Temporary bootstrap surface. The product theme is implemented in issue 003. */
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
