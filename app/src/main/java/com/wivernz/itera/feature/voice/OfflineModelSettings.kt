// Repository convention deliberately puts modifier last (03-compose-conventions.md).
@file:Suppress("ModifierParameter")

package com.wivernz.itera.feature.voice

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wivernz.itera.R
import com.wivernz.itera.core.common.currentLocale
import com.wivernz.itera.core.designsystem.component.ButtonKind
import com.wivernz.itera.core.designsystem.component.IteraButton
import com.wivernz.itera.core.designsystem.theme.Itera
import com.wivernz.itera.core.voice.VoiceModelInfo
import com.wivernz.itera.core.voice.VoiceModelState
import com.wivernz.itera.domain.voice.VoiceLanguage
import java.text.NumberFormat

/** Document-picker types for a model archive; some providers report zips as a generic binary. */
val MODEL_TYPES =
    arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")

private const val PERCENT = 100.0

@Composable
private fun size(bytes: Long): String = Formatter.formatShortFileSize(LocalContext.current, bytes)

/**
 * The active language's offline model state (milestone 013). [short] is the Settings row value: one or two words,
 * so it fits at large text in every language; the sheet shows the full state with sizes.
 */
@Composable
fun offlineModelValue(
    models: Map<VoiceLanguage, VoiceModelState>,
    info: Map<VoiceLanguage, VoiceModelInfo>,
    language: VoiceLanguage,
    short: Boolean = false
): String {
    val download = info[language]?.let { size(it.downloadBytes) }.orEmpty()
    val state = models[language] ?: VoiceModelState.NotInstalled
    if (short) {
        when (state) {
            is VoiceModelState.Installed -> return stringResource(
                R.string.voice_offline_row_installed
            )
            VoiceModelState.NotInstalled -> return stringResource(
                R.string.voice_offline_row_missing
            )
            VoiceModelState.Damaged -> return stringResource(R.string.voice_offline_row_damaged)
            VoiceModelState.Validating -> return stringResource(
                R.string.voice_offline_row_validating
            )
            VoiceModelState.Unusable -> return stringResource(R.string.voice_offline_row_unusable)
            VoiceModelState.DownloadFailed -> return stringResource(
                R.string.voice_offline_row_failed
            )
            else -> Unit
        }
    }
    return when (state) {
        is VoiceModelState.Installed -> stringResource(
            R.string.voice_offline_value_installed,
            size(state.bytes)
        )
        is VoiceModelState.Downloading -> stringResource(
            R.string.voice_offline_value_downloading,
            // locale-aware ("40%", "40 %")
            NumberFormat.getPercentInstance(currentLocale()).format(state.percent / PERCENT)
        )
        VoiceModelState.WaitingForWifi -> stringResource(R.string.voice_offline_value_wifi)
        VoiceModelState.Damaged -> stringResource(R.string.voice_offline_value_damaged)
        VoiceModelState.Validating -> stringResource(R.string.voice_offline_value_validating)
        VoiceModelState.Unusable -> stringResource(R.string.voice_offline_value_unusable)
        VoiceModelState.DownloadFailed -> stringResource(R.string.voice_offline_value_failed)
        VoiceModelState.NotInstalled -> stringResource(
            R.string.voice_offline_value_missing,
            download
        )
    }
}

/**
 * The offline model for the current app language: what it does, its size and state, and Download (Play only),
 * Import file (document picker, any build) or Remove. Nothing downloads without this explicit tap; a language
 * change only makes this sheet offer the new language.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineModelSheet(
    language: VoiceLanguage,
    state: VoiceModelState,
    info: VoiceModelInfo?,
    downloadAvailable: Boolean,
    onDownload: () -> Unit,
    onImport: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    // validate the installed files again (damaged or unusable): restores the model when it passes
    onRecheck: () -> Unit = {}
) {
    val c = Itera.colors
    val installed = state is VoiceModelState.Installed
    val busy = state is VoiceModelState.Downloading || state == VoiceModelState.WaitingForWifi ||
        state == VoiceModelState.Validating
    val recheckable = state == VoiceModelState.Unusable || state == VoiceModelState.Damaged
    ModalBottomSheet(
        modifier = modifier.testTag("OfflineModelSheet"),
        tonalElevation = 0.dp,
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.surface,
        scrimColor = c.scrim
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                stringResource(R.string.voice_offline_sheet_title, languageName(language)),
                style = Itera.type.title,
                color = c.ink
            )
            Text(
                stringResource(R.string.voice_offline_sheet_body),
                style = Itera.type.body,
                color = c.ink2
            )
            Text(
                offlineModelValue(
                    mapOf(language to state),
                    info?.let {
                        mapOf(language to it)
                    }.orEmpty(),
                    language
                ),
                style = Itera.type.label,
                color = c.ink
            )
            if (recheckable) {
                IteraButton(stringResource(R.string.voice_offline_recheck), onRecheck)
            }
            if (installed || state == VoiceModelState.Unusable) {
                IteraButton(stringResource(R.string.voice_offline_remove), {
                    onRemove()
                    onDismiss()
                }, kind = ButtonKind.Secondary)
            } else if (!busy) {
                if (downloadAvailable) {
                    IteraButton(
                        stringResource(R.string.voice_offline_download),
                        onDownload,
                        kind = if (recheckable) ButtonKind.Secondary else ButtonKind.Primary
                    )
                }
                IteraButton(
                    stringResource(R.string.voice_offline_import),
                    onImport,
                    kind = if (downloadAvailable ||
                        recheckable
                    ) {
                        ButtonKind.Secondary
                    } else {
                        ButtonKind.Primary
                    }
                )
                info?.let {
                    Text(
                        stringResource(R.string.voice_offline_import_hint, it.file),
                        style = Itera.type.caption,
                        color = c.ink2
                    )
                }
            }
            IteraButton(
                stringResource(R.string.action_done),
                onDismiss,
                kind = ButtonKind.Secondary
            )
        }
    }
}
