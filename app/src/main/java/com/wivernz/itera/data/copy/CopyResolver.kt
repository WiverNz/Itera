package com.wivernz.itera.data.copy
import android.content.Context
import com.wivernz.itera.BuildConfig
import com.wivernz.itera.R
import com.wivernz.itera.core.common.Logger
import com.wivernz.itera.core.common.time.formatTime
import com.wivernz.itera.data.catalog.CatalogKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.DateTimeException
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
data class ResolvedCopy(val title: String, val subtitle: String, val instruction: String)
class CopyResolver @Inject constructor(
    @param:ApplicationContext private val application: Context,
    private val clock: Clock,
    private val logger: Logger
) {
    private val context: Context get() = androidx.core.content.ContextCompat.getContextForLanguage(application)
    fun resolve(key: String, args: String, techniqueId: String): ResolvedCopy = try {
        val values = Json.parseToJsonElement(args).jsonObject
        val technique = values["technique"]?.jsonPrimitive?.content ?: techniqueId
        val resources = techniqueResources(technique) ?: error("Unknown technique")
        val title = context.getString(resources.first)
        val instruction = context.getString(resources.second)
        val minutes = values["minutes"]?.jsonPrimitive?.int ?: 5
        fun time(name: String, fallback: LocalTime): LocalTime =
            values[name]?.jsonPrimitive?.let { p ->
                p.intOrNull?.let {
                    LocalTime.ofSecondOfDay(it * 60L)
                }
                    ?: LocalTime.parse(p.content)
            }
                ?: fallback
        fun formatted(time: LocalTime) = formatTime(
            time,
            context.resources.configuration.locales[0],
            context
        )
        val subtitle = when (key) {
            "activity_program", "activity_program_new_technique" -> context.getString(
                R.string.activity_program,
                minutes
            )
            "activity_combination" -> context.getString(R.string.activity_combination, minutes)
            "activity_review" -> context.getString(
                R.string.activity_review,
                minutes,
                values["sourceDay"]?.jsonPrimitive?.int ?: 1
            )
            "activity_focus", "activity_focus_generic" -> {
                val scheduled = time("time", LocalTime.of(11, 0))
                if (LocalTime.now(clock) >=
                    scheduled
                ) {
                    context.getString(R.string.activity_focus_now)
                } else {
                    context.getString(
                        R.string.activity_focus,
                        formatted(scheduled)
                    )
                }
            }
            "activity_practice" -> context.getString(R.string.activity_practice)
            "activity_mitigation" -> context.getString(R.string.activity_mitigation)
            "activity_reflection" -> context.getString(
                R.string.activity_reflection,
                formatted(
                    time(
                        "eveningTime",
                        LocalTime.of(
                            21,
                            0
                        )
                    )
                )
            )
            else -> error("Unknown copy key")
        }
        ResolvedCopy(
            when (key) {
                "activity_focus_generic" -> context.getString(
                    R.string.activity_focus_generic,
                    minutes
                )
                "activity_combination" -> context.getString(R.string.activity_combination_title)
                else -> title
            },
            subtitle,
            instruction
        )
    } catch (_: DateTimeException) {
        missing()
    } catch (_: IllegalArgumentException) {
        missing()
    } catch (_: IllegalStateException) {
        missing()
    }
    private fun missing(): ResolvedCopy {
        if (BuildConfig.DEBUG) error("Missing activity copy resource or invalid arguments")
        logger.w(TAG, "Activity copy unavailable")
        return ResolvedCopy(context.getString(R.string.error_content_missing), "", "")
    }
    private val ids = java.util.concurrent.ConcurrentHashMap<String, Int>()

    /** Name and task resources derived from the catalogue slug (docs/data/04 section 4). */
    @android.annotation.SuppressLint("DiscouragedApi")
    private fun techniqueResources(technique: String): Pair<Int, Int>? {
        if (technique !in CatalogKeys.SLUGS) return null
        fun id(key: String) = ids.getOrPut(key) {
            context.resources.getIdentifier(key, "string", context.packageName)
        }
        val name = id(CatalogKeys.name(technique))
        val task = id(CatalogKeys.task(technique))
        return if (name == 0 || task == 0) null else name to task
    }
}
private const val TAG = "CopyResolver"
