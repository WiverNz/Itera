package com.wivernz.itera.feature.voice

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.wivernz.itera.core.common.currentLocale
import com.wivernz.itera.core.voice.VoiceRecognizer
import com.wivernz.itera.domain.voice.VoiceLanguage

/** The effective app language, which recognition and the parser both follow. */
@Composable
fun currentVoiceLanguage(): VoiceLanguage = VoiceLanguage.of(currentLocale().language)

/**
 * Provides the one [VoiceController] for the activity. Listening stops when the app goes to the background, when
 * the language changes and when the activity goes away; nothing is restored afterwards.
 */
@Composable
fun VoiceHost(recognizer: VoiceRecognizer, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val language = currentVoiceLanguage()
    val holder = remember { arrayOfNulls<(Boolean) -> Unit>(1) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            holder[0]?.invoke(granted)
            holder[0] = null
        }
    val gate = remember(context) {
        object : VoicePermissionGate {
            override fun granted() =
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED

            override fun request(onResult: (Boolean) -> Unit) {
                holder[0] = onResult
                launcher.launch(Manifest.permission.RECORD_AUDIO)
            }

            override fun permanentlyDenied(): Boolean {
                val activity = context.findActivity() ?: return false
                return !granted() &&
                    !ActivityCompat.shouldShowRequestPermissionRationale(
                        activity,
                        Manifest.permission.RECORD_AUDIO
                    )
            }
        }
    }
    val controller = remember(recognizer) { VoiceController(recognizer, gate, language) }
    LaunchedEffect(language) { controller.onLanguage(language) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { controller.cancel() }
    DisposableEffect(controller) { onDispose { controller.release() } }
    CompositionLocalProvider(LocalVoiceController provides controller, content = content)
}

/** Permanent denial: the app's own settings page. */
fun openAppSettings(context: Context) {
    val intent =
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        )
    launchSafely(context, intent)
}

/** Missing service or language model: the platform's voice input settings, no automatic download. */
fun openSpeechSettings(context: Context) {
    val voice = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)
    if (!launchSafely(context, voice)) launchSafely(context, Intent(Settings.ACTION_SETTINGS))
}

private fun launchSafely(context: Context, intent: Intent): Boolean = runCatching {
    context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}.isSuccess

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
