package com.wivernz.itera.core.navigation
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
sealed interface AppRoute

@Serializable
@SerialName("Welcome")
data object Welcome : AppRoute

@Serializable
@SerialName("Goals")
data object Goals : AppRoute

@Serializable
@SerialName("Rhythm")
data object Rhythm : AppRoute

@Serializable
@SerialName("FirstWeek")
data object FirstWeek : AppRoute

@Serializable
@SerialName("Today")
data object Today : AppRoute

@Serializable
@SerialName("Train")
data object Train : AppRoute

@Serializable
@SerialName("Progress")
data object Progress : AppRoute

@Serializable
@SerialName("You")
data object You : AppRoute

@Serializable
@SerialName("Library")
data object Library : AppRoute

@Serializable
@SerialName("History")
data object History : AppRoute

@Serializable
@SerialName("HistoryDate")
data class HistoryDate(val selectedDate: String) : AppRoute

@Serializable
@SerialName("TwoMinute")
data class TwoMinute(val activityId: Long) : AppRoute

@Serializable
@SerialName("Eisenhower")
data class Eisenhower(val activityId: Long) : AppRoute

@Serializable
@SerialName("Feynman")
data class Feynman(val activityId: Long) : AppRoute

@Serializable
@SerialName("FeynmanFeedback")
data class FeynmanFeedback(val activityId: Long) : AppRoute

@Serializable
@SerialName("Review")
data class Review(val activityId: Long) : AppRoute

@Serializable
@SerialName("Premortem")
data class Premortem(val activityId: Long) : AppRoute

@Serializable
@SerialName("HabitStack")
data class HabitStack(val activityId: Long) : AppRoute

@Serializable
@SerialName("Combination")
data class Combination(val activityId: Long) : AppRoute

@Serializable
@SerialName("Reflection")
data class Reflection(val activityId: Long) : AppRoute

@Serializable
@SerialName("TechniqueDetail")
data class TechniqueDetail(val technique: String) : AppRoute

@Serializable
@SerialName("ExerciseIntro")
data class ExerciseIntro(val activityId: Long, val technique: String) : AppRoute

/** The template body's run step (ADR-0007); the 2-minute rule's `TwoMinute` route renders the same body. */
@Serializable
@SerialName("ExerciseRun")
data class ExerciseRun(val activityId: Long, val technique: String) : AppRoute

@Serializable
@SerialName("ExerciseResult")
data class ExerciseResult(val activityId: Long, val technique: String, val readOnly: Boolean = false) : AppRoute

@Serializable
@SerialName("FocusSession")
data class FocusSession(
    val activityId: Long,
    val minutes: Int,
    val technique: String? = null,
    // a voice-requested length (milestone 012); 0 when none
    val requested: Int = 0
) : AppRoute

@Serializable
@SerialName("DayComplete")
data class DayComplete(val dayId: Long) : AppRoute

object RouteCodec {
    const val EXTRA = "itera.deeplink"
    fun encode(route: AppRoute): String = Json.encodeToString(AppRoute.serializer(), route)
    fun decode(value: String): AppRoute? =
        runCatching { Json.decodeFromString(AppRoute.serializer(), value) }.getOrNull()
}
