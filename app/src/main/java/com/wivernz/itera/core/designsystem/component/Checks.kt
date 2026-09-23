package com.wivernz.itera.core.designsystem.component
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wivernz.itera.R
import com.wivernz.itera.core.designsystem.icon.IteraIcons
import com.wivernz.itera.core.designsystem.theme.Itera
import com.wivernz.itera.core.designsystem.theme.IteraMotion
import com.wivernz.itera.core.designsystem.theme.LocalReduceMotion
import com.wivernz.itera.core.designsystem.theme.colors
import com.wivernz.itera.domain.model.MasteryLevel

@Composable
fun CheckCircle(checked: Boolean, color: Color, modifier: Modifier = Modifier) {
    val c = Itera.colors
    Box(
        modifier.size(30.dp).clip(CircleShape)
            .background(if (checked) color else androidx.compose.ui.graphics.Color.Transparent)
            .border(2.dp, if (checked) color else c.line, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (checked) Icon(IteraIcons.Check, null, tint = c.surface, modifier = Modifier.size(16.dp))
    }
}

@Composable
fun AnimatedCheck(
    container: androidx.compose.ui.graphics.Color,
    stroke: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    val reduced = LocalReduceMotion.current
    val pop = remember { Animatable(if (reduced) 1f else 0.5f) }
    val draw = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(reduced) {
        pop.animateTo(1f, IteraMotion.pop(reduced, lowStiffness = true))
        draw.animateTo(1f, IteraMotion.check(reduced))
    }
    Box(
        modifier.size(96.dp).scale(pop.value).clip(CircleShape).background(container),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(48.dp)) {
            val s = size.width / 24f
            val path = Path().apply {
                moveTo(5f * s, 12.5f * s)
                lineTo(9.5f * s, 17f * s)
                lineTo(19f * s, 7.5f * s)
            }
            val measure = PathMeasure().apply { setPath(path, false) }
            val partial = Path()
            measure.getSegment(0f, measure.length * draw.value, partial, true)
            drawPath(
                partial,
                stroke,
                style = Stroke(width = 2.4f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

@Composable
fun MasteryDots(level: MasteryLevel, color: Color, modifier: Modifier = Modifier) {
    val c = Itera.colors
    val reached = level.ordinal
    Column(
        modifier.semantics(mergeDescendants = true) {
        },
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(4) { i ->
                Box(
                    Modifier.size(7.dp).clip(CircleShape).background(
                        if (i <
                            reached
                        ) {
                            color
                        } else {
                            c.line
                        }
                    )
                )
            }
        }
        Text(
            stringResource(level.title),
            style = Itera.type.caption.copy(
                fontSize =
                Itera.type.caption.fontSize * 0.85f
            ),
            color = c.ink2
        )
    }
}
