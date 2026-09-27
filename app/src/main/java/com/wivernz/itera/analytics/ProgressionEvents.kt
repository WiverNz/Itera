package com.wivernz.itera.analytics

import com.wivernz.itera.domain.model.MasteryLevel
import com.wivernz.itera.domain.model.Skill
import com.wivernz.itera.domain.model.SkillLevel
import com.wivernz.itera.domain.progress.ObserveProgressUseCase
import com.wivernz.itera.domain.progress.ObserveTechniqueProgressUseCase
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

data class ProgressionSnapshot(
    val mastery: Map<AnalyticsTechnique, MasteryLevel>,
    val skills: Map<Skill, SkillLevel>
)

/** Numeric/enumerated facts only. Diagnostic failures must not prevent completion. */
class ProgressionEvents @Inject constructor(
    private val techniques: ObserveTechniqueProgressUseCase,
    private val progress: ObserveProgressUseCase,
    private val analytics: Analytics
) {
    suspend fun snapshot(): ProgressionSnapshot? = try {
        ProgressionSnapshot(
            techniques().first().mapNotNull { item ->
                AnalyticsTechnique.of(item.techniqueId.value)?.let {
                    it to
                        item.level
                }
            }.toMap(),
            progress().first().skills.associate { it.skill to it.level }
        )
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

    suspend fun changed(before: ProgressionSnapshot?) {
        if (before == null) return
        val after = snapshot() ?: return
        after.mastery.forEach { (id, level) ->
            before.mastery[id]?.takeIf {
                it != level
            }?.let { analytics.append(Event.MasteryChanged(id, it, level)) }
        }
        after.skills.forEach { (skill, level) ->
            before.skills[skill]?.takeIf {
                it != level
            }?.let { analytics.append(Event.SkillLevelChanged(skill, it, level)) }
        }
    }
}
