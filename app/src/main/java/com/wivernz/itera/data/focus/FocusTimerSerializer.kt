package com.wivernz.itera.data.focus
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.wivernz.itera.domain.model.FocusTimerState
import com.wivernz.itera.domain.model.TechniqueId
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
@Serializable
internal data class FocusTimerDto(
    val activityId: Long,
    val techniqueId: String,
    val taskLabel: String,
    val plannedSeconds: Int,
    val extendedSeconds: Int,
    val startedAt: Long,
    val endsAt: Long,
    val pausedAt: Long?,
    val accumulatedPauseMs: Long
)
object FocusTimerSerializer : Serializer<FocusTimerState?> {
    override val defaultValue: FocusTimerState? = null
    private val json = Json { ignoreUnknownKeys = true }
    override suspend fun readFrom(input: InputStream): FocusTimerState? = try {
        val text = input.readBytes().decodeToString()
        if (text == "null") {
            null
        } else {
            json.decodeFromString<FocusTimerDto>(text).let {
                FocusTimerState(
                    it.activityId,
                    TechniqueId(it.techniqueId),
                    it.taskLabel,
                    it.plannedSeconds,
                    it.extendedSeconds,
                    Instant.ofEpochMilli(it.startedAt),
                    Instant.ofEpochMilli(it.endsAt),
                    it.pausedAt?.let(Instant::ofEpochMilli),
                    it.accumulatedPauseMs
                )
            }
        }
    } catch (_: IllegalArgumentException) {
        throw CorruptionException("Unreadable focus timer")
    }
    override suspend fun writeTo(t: FocusTimerState?, output: OutputStream) {
        val text = t?.let {
            json.encodeToString(
                FocusTimerDto.serializer(),
                FocusTimerDto(
                    it.activityId,
                    it.techniqueId.value,
                    it.taskLabel,
                    it.plannedSeconds,
                    it.extendedSeconds,
                    it.startedAt.toEpochMilli(),
                    it.endsAt.toEpochMilli(),
                    it.pausedAt?.toEpochMilli(),
                    it.accumulatedPauseMs
                )
            )
        } ?: "null"
        output.write(text.encodeToByteArray())
    }
}
