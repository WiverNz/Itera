// Repository convention deliberately puts modifier last (03-compose-conventions.md).
@file:Suppress("ModifierParameter")

package com.wivernz.itera.feature.voice

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.wivernz.itera.R
import com.wivernz.itera.core.designsystem.component.ButtonKind
import com.wivernz.itera.core.designsystem.component.IteraButton
import com.wivernz.itera.core.designsystem.icon.IteraIcons
import com.wivernz.itera.core.designsystem.theme.Itera
import com.wivernz.itera.core.voice.RecognitionProviderInfo

/** Installed recognition apps for Settings; empty where voice is not wired. */
@Composable
fun recognitionProviders(): List<RecognitionProviderInfo> {
    val voice = LocalVoiceController.current
    return remember(voice) { voice?.providers().orEmpty() }
}

/**
 * Settings: change or turn off the chosen recognition app (ADR-0022). Picking an app shows the consent naming it
 * first; only "Use <app>" stores it. "None" turns it off at once.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecognizerSheet(
    providers: List<RecognitionProviderInfo>,
    selected: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = Itera.colors
    var reviewing by remember { mutableStateOf<String?>(null) }
    val pending = providers.firstOrNull { it.id == reviewing }
    ModalBottomSheet(
        modifier = modifier,
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
            if (pending != null) {
                Text(
                    stringResource(R.string.voice_provider_consent_title, pending.label),
                    style = Itera.type.title,
                    color = c.ink
                )
                Text(
                    stringResource(R.string.voice_provider_consent_body, pending.label),
                    style = Itera.type.body,
                    color = c.ink2
                )
                IteraButton(stringResource(R.string.voice_provider_use, pending.label), {
                    onSelect(pending.id)
                    onDismiss()
                })
                IteraButton(
                    stringResource(R.string.voice_not_now),
                    { reviewing = null },
                    kind = ButtonKind.Secondary
                )
            } else {
                Text(
                    stringResource(R.string.voice_provider_setting),
                    style = Itera.type.title,
                    color = c.ink
                )
                Text(
                    stringResource(R.string.voice_provider_setting_sub),
                    style = Itera.type.bodySmall,
                    color = c.ink2
                )
                Column {
                    Option(null, stringResource(R.string.voice_provider_none), selected == null) {
                        onSelect(null)
                    }
                    providers.forEach { provider ->
                        Option(provider, provider.label, selected == provider.id) {
                            if (selected != provider.id) reviewing = provider.id
                        }
                    }
                }
                IteraButton(stringResource(R.string.action_done), onClick = onDismiss)
            }
        }
    }
}

@Composable
private fun Option(
    provider: RecognitionProviderInfo?,
    label: String,
    checked: Boolean,
    onClick: () -> Unit
) {
    val c = Itera.colors
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { selected = checked }
            .testTag("RecognizerOption"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val icon = remember(provider?.id) {
            runCatching { provider?.icon?.toBitmap(96, 96)?.asImageBitmap() }.getOrNull()
        }
        if (icon != null) {
            Image(icon, contentDescription = null, modifier = Modifier.size(28.dp))
        }
        Text(label, style = Itera.type.body, color = c.ink, modifier = Modifier.weight(1f))
        if (checked) Icon(IteraIcons.Check, null, tint = c.ink, modifier = Modifier.size(20.dp))
    }
}
