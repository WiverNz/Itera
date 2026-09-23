package com.wivernz.itera.data.mapper
import com.wivernz.itera.core.common.Logger
import com.wivernz.itera.domain.model.ActivityResult
import java.time.DateTimeException
import javax.inject.Inject
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
class ResultPayloadCodec @Inject constructor(private val logger: Logger) {
    private val json = Json {
        ignoreUnknownKeys = true
        serializersModule = SerializersModule {
            polymorphic(ActivityResultDto::class) {
                subclass(ActivityResultDto.Template::class)
                subclass(ActivityResultDto.Focus::class)
                subclass(ActivityResultDto.Eisenhower::class)
                subclass(ActivityResultDto.Feynman::class)
                subclass(ActivityResultDto.Premortem::class)
                subclass(ActivityResultDto.HabitStack::class)
                subclass(ActivityResultDto.Reflection::class)
                subclass(ActivityResultDto.Review::class)
                subclass(ActivityResultDto.Combination::class)
            }
        }
    }
    fun encode(result: ActivityResult): String =
        json.encodeToString(ActivityResultDto.serializer(), result.toDto())
    fun decode(payload: String?, rowId: Long, type: String): ActivityResult? {
        if (payload == null) return null
        return try {
            json.decodeFromString(ActivityResultDto.serializer(), payload).toDomain()
        } catch (_: DateTimeException) {
            warn(rowId, type)
            null
        } catch (_: IllegalArgumentException) {
            warn(rowId, type)
            null
        }
    }
    private fun warn(rowId: Long, type: String) {
        // Do not pass the exception: serialization errors can embed user-authored payload text.
        logger.w(TAG, "activity $rowId ($type) payload undecodable")
    }
}
private const val TAG = "ResultPayloadCodec"
