package com.wivernz.itera.analytics
import com.wivernz.itera.core.common.dispatchers.IoDispatcher
import com.wivernz.itera.data.database.dao.EventLogDao
import com.wivernz.itera.data.database.entity.EventLogEntity
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
@Singleton
class LocalAnalytics @Inject constructor(
    private val dao: EventLogDao,
    private val clock: Clock,
    @IoDispatcher io: CoroutineDispatcher
) : Analytics {
    private val scope = CoroutineScope(SupervisorJob() + io)
    override fun track(event: Event) {
        scope.launch { append(event) }
    }
    override suspend fun append(event: Event) {
        try {
            val params = JsonObject(
                event.params.mapValues { (_, value) ->
                    when (value) {
                        null -> JsonNull
                        is Boolean -> JsonPrimitive(value)
                        is Number -> JsonPrimitive(value)
                        is String -> JsonPrimitive(value)
                        else -> error("Unsupported analytics scalar")
                    }
                }
            )
            dao.insert(
                EventLogEntity(
                    timestamp = clock.millis(),
                    name = event.name,
                    params = params.toString()
                )
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            /* Analytics never changes application behavior. */
        }
    }
}
