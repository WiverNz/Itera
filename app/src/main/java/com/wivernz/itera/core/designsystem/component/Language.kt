package com.wivernz.itera.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wivernz.itera.R
import com.wivernz.itera.core.common.AppLanguage
import com.wivernz.itera.core.common.currentLocale
import com.wivernz.itera.core.designsystem.icon.IteraIcons
import com.wivernz.itera.core.designsystem.theme.Itera
import com.wivernz.itera.core.designsystem.theme.colors
import com.wivernz.itera.domain.model.MasteryLevel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSheet(
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = Itera.colors
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        modifier = modifier,
        tonalElevation = 0.dp,
        onDismissRequest = onDismiss,
        sheetState = state,
        containerColor = c.surface,
        scrimColor = c.scrim
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(stringResource(R.string.language_title), style = Itera.type.title, color = c.ink)
            Text(
                stringResource(R.string.language_sub),
                style = Itera.type.bodySmall,
                color = c.ink2
            )
            Column {
                AppLanguage.tags.forEachIndexed { index, tag ->
                    LanguageOption(tag, selected == tag, { onSelect(tag) })
                    if (index == 0) {
                        Box(Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Divider() }
                    }
                }
            }
            IteraButton(stringResource(R.string.action_done), onClick = onDismiss)
        }
    }
}

@Composable
private fun LanguageOption(tag: String, selected: Boolean, onSelect: () -> Unit) {
    val c = Itera.colors
    val locale = currentLocale()
    val deviceLanguage = android.content.res.Resources.getSystem().configuration.locales[0].language
    val title = if (tag.isEmpty()) {
        stringResource(R.string.language_match_device)
    } else {
        AppLanguage.nativeName(tag)
    }
    val subtitle = if (tag.isEmpty()) {
        stringResource(R.string.language_current, AppLanguage.localName(deviceLanguage, locale))
    } else {
        AppLanguage.localName(tag, locale)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .selectable(selected, role = Role.RadioButton, onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(c.surface2),
            contentAlignment = Alignment.Center
        ) {
            if (tag.isEmpty()) {
                Icon(IteraIcons.Globe, null, tint = c.ink, modifier = Modifier.size(20.dp))
            } else {
                Text(
                    tag.uppercase(),
                    style = Itera.type.caption.copy(fontWeight = FontWeight.Bold),
                    color = c.ink
                )
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = Itera.type.label.copy(fontFamily = Itera.type.userText.fontFamily),
                color = c.ink
            )
            Text(subtitle, style = Itera.type.bodySmall, color = c.ink2)
        }
        RadioDot(selected)
    }
}

@Composable
fun RadioDot(on: Boolean, modifier: Modifier = Modifier) {
    val c = Itera.colors
    Box(
        modifier.size(26.dp).clip(CircleShape)
            .background(if (on) c.ink else androidx.compose.ui.graphics.Color.Transparent)
            .border(2.dp, if (on) c.ink else c.line, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (on) Icon(IteraIcons.Check, null, tint = c.onInk, modifier = Modifier.size(15.dp))
    }
}

@Composable
fun LanguagePill(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Itera.colors
    val code = currentLocale().language.uppercase()
    val label = stringResource(
        R.string.language_a11y,
        AppLanguage.nativeName(currentLocale().language)
    )
    Row(
        modifier
            .heightIn(min = 44.dp)
            .clip(CircleShape)
            .background(c.surface2)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(IteraIcons.Globe, null, tint = c.ink, modifier = Modifier.size(18.dp))
        Text(
            code,
            style = Itera.type.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = c.ink
        )
    }
}
