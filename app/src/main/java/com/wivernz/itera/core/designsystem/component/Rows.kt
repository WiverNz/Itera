// Repository convention deliberately puts modifier last (03-compose-conventions.md).
@file:Suppress("ModifierParameter")

package com.wivernz.itera.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.wivernz.itera.core.designsystem.icon.IteraIcons
import com.wivernz.itera.core.designsystem.theme.Itera
import com.wivernz.itera.core.designsystem.theme.colors
import com.wivernz.itera.domain.model.MasteryLevel

@Composable
fun Group(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val c = Itera.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title.uppercase(),
            style = Itera.type.eyebrow,
            color = c.ink2,
            modifier = Modifier.padding(start = 4.dp)
        )
        Column(
            Modifier.fillMaxWidth().clip(
                RoundedCornerShape(20.dp)
            ).background(c.surface).padding(horizontal = 16.dp),
            content = content
        )
    }
}

@Composable
fun ValueRow(label: String, value: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Itera.colors
    androidx.compose.foundation.layout.BoxWithConstraints(modifier.fillMaxWidth()) {
        val valueMaxWidth = maxWidth * 0.55f
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp)
                .clickable(role = Role.Button, onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                label,
                style = Itera.type.body.copy(fontWeight = FontWeight.Medium),
                color = c.ink,
                modifier = Modifier.weight(1f)
            )
            if (value.isNotEmpty()) {
                Text(
                    value,
                    style = Itera.type.bodySmall,
                    color = c.ink2,
                    modifier = Modifier.widthIn(max = valueMaxWidth)
                )
            }
            Icon(IteraIcons.Chevron, null, tint = c.ink3, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun SwitchRow(
    label: String,
    checked: Boolean,
    accent: androidx.compose.ui.graphics.Color,
    sub: String? = null,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = Itera.colors
    Row(
        modifier.fillMaxWidth().heightIn(
            min = 56.dp
        ).alpha(
            if (enabled) 1f else 0.55f
        ).toggleable(checked, enabled = enabled, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = Itera.type.body.copy(fontWeight = FontWeight.Medium), color = c.ink)
            if (sub != null) Text(sub, style = Itera.type.caption, color = c.ink2)
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedTrackColor = accent,
                checkedThumbColor = c.surface,
                uncheckedTrackColor = c.surface2,
                uncheckedThumbColor = c.ink3,
                uncheckedBorderColor = c.line
            )
        )
    }
}

@Composable
fun LinkRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    sub: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = Itera.colors
    Row(
        modifier.fillMaxWidth().heightIn(
            min = 60.dp
        ).clip(RoundedCornerShape(16.dp)).clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(c.surface2),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = c.ink, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = Itera.type.body.copy(fontWeight = FontWeight.SemiBold),
                color = c.ink
            )
            Text(sub, style = Itera.type.bodySmall, color = c.ink2)
        }
        Icon(IteraIcons.Chevron, null, tint = c.ink2, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun TimeRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    sub: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = Itera.colors
    Row(
        modifier.fillMaxWidth().heightIn(min = 64.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(c.surface2),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = c.ink, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = Itera.type.body.copy(fontWeight = FontWeight.SemiBold),
                color = c.ink
            )
            Text(sub, style = Itera.type.bodySmall, color = c.ink2)
        }
        Box(
            Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(14.dp)).background(c.surface2)
                .clickable(role = Role.Button, onClick = onClick).semantics {
                    contentDescription =
                        title
                }.padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                value,
                style = Itera.type.headline.copy(fontWeight = FontWeight.SemiBold),
                color = c.ink
            )
        }
    }
}
