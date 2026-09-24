package com.wivernz.itera.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
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
fun IntervalLadder(stageIndex: Int, accent: Color, modifier: Modifier = Modifier) {
    require(stageIndex in 0..4)
    val description = stringResource(R.string.review_stage_a11y, stageIndex + 1)
    val c = Itera.colors
    val steps = listOf(
        stringResource(R.string.interval_days, 1),
        stringResource(R.string.interval_days, 4),
        stringResource(R.string.interval_days, 9),
        stringResource(R.string.interval_weeks, 3),
        stringResource(R.string.interval_months, 2)
    )
    Row(
        modifier.fillMaxWidth().semantics(mergeDescendants = true) {
            contentDescription =
                description
        },
        verticalAlignment = Alignment.Top
    ) {
        steps.forEachIndexed { i, label ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.width(48.dp)
            ) {
                val mod = Modifier.size(22.dp).clip(CircleShape)
                when {
                    i < stageIndex -> Box(
                        mod.background(accent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            IteraIcons.Check,
                            null,
                            tint = c.surface,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    i == stageIndex -> Box(
                        mod.background(c.surface).border(3.dp, accent, CircleShape)
                    )
                    else -> Box(mod.border(2.dp, c.line, CircleShape))
                }
                Text(
                    label,
                    style = Itera.type.caption.copy(
                        fontWeight = if (i ==
                            stageIndex
                        ) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Normal
                        }
                    ),
                    color = if (i <=
                        stageIndex
                    ) {
                        c.ink
                    } else {
                        c.ink2
                    },
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            if (i <
                steps.lastIndex
            ) {
                Box(
                    Modifier.weight(1f).padding(top = 10.dp).height(2.dp).background(
                        if (i <
                            stageIndex
                        ) {
                            accent
                        } else {
                            c.line
                        }
                    )
                )
            }
        }
    }
}
