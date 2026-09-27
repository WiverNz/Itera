package com.wivernz.itera.core.designsystem.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Stroke icons drawn on a 24×24 grid. Tint them with Icon(tint = …). */
object IteraIcons {
    private fun icon(name: String, vararg paths: String, filled: Boolean = false): ImageVector {
        val builder = ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        )
        paths.forEach { d ->
            builder.addPath(
                pathData = addPathNodes(d),
                fill = if (filled) SolidColor(Color.Black) else null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }
        return builder.build()
    }

    val TwoMinute: ImageVector by lazy {
        icon("TwoMinute", "M4,13 a8,8 0 1,0 16,0 a8,8 0 1,0 -16,0", "M12 9v4l2.5 1.5", "M9.5 3h5")
    }
    val Pomodoro: ImageVector by lazy {
        icon(
            "Pomodoro",
            "M5,14 a7,7 0 1,0 14,0 a7,7 0 1,0 -14,0",
            "M12 7c0-2 1-3.2 3-3.5",
            "M12 7c-1.4-1.2-3.2-1.3-4.5-.4"
        )
    }
    val Eisenhower: ImageVector by lazy {
        icon(
            "Eisenhower",
            "M5.5,4 h4 a1.5,1.5 0 0,1 1.5,1.5 v4 a1.5,1.5 0 0,1 -1.5,1.5 h-4 a1.5,1.5 0 0,1 -1.5,-1.5 v-4 a1.5,1.5 0 0,1 1.5,-1.5 z",
            "M14.5,4 h4 a1.5,1.5 0 0,1 1.5,1.5 v4 a1.5,1.5 0 0,1 -1.5,1.5 h-4 a1.5,1.5 0 0,1 -1.5,-1.5 v-4 a1.5,1.5 0 0,1 1.5,-1.5 z",
            "M5.5,13 h4 a1.5,1.5 0 0,1 1.5,1.5 v4 a1.5,1.5 0 0,1 -1.5,1.5 h-4 a1.5,1.5 0 0,1 -1.5,-1.5 v-4 a1.5,1.5 0 0,1 1.5,-1.5 z",
            "M14.5,13 h4 a1.5,1.5 0 0,1 1.5,1.5 v4 a1.5,1.5 0 0,1 -1.5,1.5 h-4 a1.5,1.5 0 0,1 -1.5,-1.5 v-4 a1.5,1.5 0 0,1 1.5,-1.5 z"
        )
    }
    val Feynman: ImageVector by lazy {
        icon(
            "Feynman",
            "M4 6.5A2.5 2.5 0 0 1 6.5 4h11A2.5 2.5 0 0 1 20 6.5v7a2.5 2.5 0 0 1-2.5 2.5H11l-4.5 4v-4A2.5 2.5 0 0 1 4 13.5z",
            "M8.5 8.5h7",
            "M8.5 11.5h4"
        )
    }
    val FiveSecond: ImageVector by lazy {
        icon("FiveSecond", "M5 5v14", "M9 12h10", "M15 7l5 5-5 5")
    }
    val InfoDiet: ImageVector by lazy { icon("InfoDiet", "M4 5h16l-6 7.5V18l-4 2v-7.5z") }
    val OnePercent: ImageVector by lazy { icon("OnePercent", "M4 19h4v-4h4v-4h4V7h4") }
    val DeepWork: ImageVector by lazy {
        icon(
            "DeepWork",
            "M3.5,12 a8.5,8.5 0 1,0 17,0 a8.5,8.5 0 1,0 -17,0",
            "M7.5,12 a4.5,4.5 0 1,0 9,0 a4.5,4.5 0 1,0 -9,0",
            "M11,12 a1,1 0 1,0 2,0 a1,1 0 1,0 -2,0"
        )
    }
    val Premortem: ImageVector by lazy {
        icon("Premortem", "M12 4l9 16H3z", "M12 10v4", "M12 17h.01")
    }
    val HabitStack: ImageVector by lazy {
        icon(
            "HabitStack",
            "M5.5,15 h13 a1.5,1.5 0 0,1 1.5,1.5 v2 a1.5,1.5 0 0,1 -1.5,1.5 h-13 a1.5,1.5 0 0,1 -1.5,-1.5 v-2 a1.5,1.5 0 0,1 1.5,-1.5 z",
            "M8,9 h8 a1.5,1.5 0 0,1 1.5,1.5 v2 a1.5,1.5 0 0,1 -1.5,1.5 h-8 a1.5,1.5 0 0,1 -1.5,-1.5 v-2 a1.5,1.5 0 0,1 1.5,-1.5 z",
            "M10.5,3.5 h3 a1.5,1.5 0 0,1 1.5,1.5 v1.5 a1.5,1.5 0 0,1 -1.5,1.5 h-3 a1.5,1.5 0 0,1 -1.5,-1.5 v-1.5 a1.5,1.5 0 0,1 1.5,-1.5 z"
        )
    }
    val Pareto: ImageVector by lazy {
        icon("Pareto", "M3.5,12 a8.5,8.5 0 1,0 17,0 a8.5,8.5 0 1,0 -17,0", "M12 3.5V12h8.5")
    }
    val TwoList: ImageVector by lazy {
        icon(
            "TwoList",
            "M4 6h6",
            "M4 10h6",
            "M4 14h6",
            "M14 6h6",
            "M14 10h6",
            "M14 14h6",
            "M13 19l8-16"
        )
    }
    val Reflection: ImageVector by lazy {
        icon("Reflection", "M19.5 14.5A8 8 0 1 1 9.5 4.5a6.5 6.5 0 0 0 10 10z")
    }
    val Spaced: ImageVector by lazy {
        icon(
            "Spaced",
            "M4.5 12a7.5 7.5 0 0 1 13-5.1",
            "M18 3v4h-4",
            "M19.5 12a7.5 7.5 0 0 1-13 5.1",
            "M6 21v-4h4"
        )
    }
    val Today: ImageVector by lazy {
        icon(
            "Today",
            "M8,12 a4,4 0 1,0 8,0 a4,4 0 1,0 -8,0",
            "M12 2.5v2M12 19.5v2M2.5 12h2M19.5 12h2M5.3 5.3l1.4 1.4M17.3 17.3l1.4 1.4M5.3 18.7l1.4-1.4M17.3 6.7l1.4-1.4"
        )
    }
    val Train: ImageVector by lazy {
        icon(
            "Train",
            "M4,18 a2,2 0 1,0 4,0 a2,2 0 1,0 -4,0",
            "M16,6 a2,2 0 1,0 4,0 a2,2 0 1,0 -4,0",
            "M8 18h5.5a3 3 0 0 0 0-6h-3a3 3 0 0 1 0-6H16"
        )
    }
    val Progress: ImageVector by lazy { icon("Progress", "M5 20v-6", "M12 20V9", "M19 20V4") }
    val You: ImageVector by lazy {
        icon(
            "You",
            "M8,8 a4,4 0 1,0 8,0 a4,4 0 1,0 -8,0",
            "M4.5 20.5c1.4-3.8 4.4-5 7.5-5s6.1 1.2 7.5 5"
        )
    }
    val Close: ImageVector by lazy { icon("Close", "M6 6l12 12M18 6L6 18") }
    val Back: ImageVector by lazy { icon("Back", "M15 5l-7 7 7 7") }
    val Chevron: ImageVector by lazy { icon("Chevron", "M9 5l7 7-7 7") }
    val Check: ImageVector by lazy { icon("Check", "M5 12.5l4.5 4.5L19 7.5") }
    val Plus: ImageVector by lazy { icon("Plus", "M12 5v14M5 12h14") }
    val Lock: ImageVector by lazy {
        icon(
            "Lock",
            "M7.5,10.5 h9 a2.5,2.5 0 0,1 2.5,2.5 v5 a2.5,2.5 0 0,1 -2.5,2.5 h-9 a2.5,2.5 0 0,1 -2.5,-2.5 v-5 a2.5,2.5 0 0,1 2.5,-2.5 z",
            "M8.5 10.5V8a3.5 3.5 0 0 1 7 0v2.5"
        )
    }
    val Play: ImageVector by lazy { icon("Play", "M8 5.5v13l10.5-6.5z", filled = true) }
    val Pause: ImageVector by lazy { icon("Pause", "M8.5 5.5v13M15.5 5.5v13") }
    val Spark: ImageVector by lazy {
        icon(
            "Spark",
            "M12 3.5c.6 4.2 2.3 5.9 6.5 6.5-4.2.6-5.9 2.3-6.5 6.5-.6-4.2-2.3-5.9-6.5-6.5 4.2-.6 5.9-2.3 6.5-6.5z",
            "M18.5 16v4M16.5 18h4"
        )
    }
    val Bell: ImageVector by lazy {
        icon("Bell", "M6 16.5V11a6 6 0 0 1 12 0v5.5l1.5 1.5h-15z", "M10 20.5a2 2 0 0 0 4 0")
    }
    val ArrowDown: ImageVector by lazy { icon("ArrowDown", "M12 5v14M6 13l6 6 6-6") }
    val Eye: ImageVector by lazy {
        icon(
            "Eye",
            "M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12z",
            "M9,12 a3,3 0 1,0 6,0 a3,3 0 1,0 -6,0"
        )
    }
    val Globe: ImageVector by lazy {
        icon(
            "Globe",
            "M3.5,12 a8.5,8.5 0 1,0 17,0 a8.5,8.5 0 1,0 -17,0",
            "M3.5 12h17",
            "M12 3.5c2.4 2.6 3.5 5.4 3.5 8.5s-1.1 5.9-3.5 8.5c-2.4-2.6-3.5-5.4-3.5-8.5s1.1-5.9 3.5-8.5z"
        )
    }
    val Mic: ImageVector by lazy {
        icon(
            "Mic",
            "M9 6.5a3 3 0 0 1 6 0v5a3 3 0 0 1-6 0z",
            "M5.5 11.5a6.5 6.5 0 0 0 13 0",
            "M12 18v2.5"
        )
    }
}
