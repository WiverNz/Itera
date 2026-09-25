package com.wivernz.itera.data.preferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.wivernz.itera.core.common.Logger
import com.wivernz.itera.domain.model.ProgramPace
import com.wivernz.itera.domain.model.Skill
import com.wivernz.itera.domain.model.ThemePreference
import com.wivernz.itera.domain.model.TimeBudget
import com.wivernz.itera.domain.model.UserPreferences
import com.wivernz.itera.domain.repository.PreferencesRepository
import java.io.IOException
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
class DataStorePreferencesRepository @Inject constructor(
    private val store: DataStore<Preferences>,
    private val logger: Logger
) : PreferencesRepository {
    override val preferences: Flow<UserPreferences> = store.data.catch { e ->
        if (e is IOException) {
            logger.w(
                TAG,
                "Preferences unreadable; using defaults"
            )
            emit(emptyPreferences())
        } else {
            throw e
        }
    }.map(::read).distinctUntilChanged()
    override suspend fun update(transform: (UserPreferences) -> UserPreferences) {
        store.edit { prefs ->
            val value = transform(read(prefs))
            prefs[onboardingCompletedKey] = value.onboardingCompleted
            prefs[focusAreasKey] = value.focusAreas.map { it.name }.toSet()
            prefs[morningTimeKey] = value.morningTime.let { it.hour * 60 + it.minute }
            prefs[eveningTimeKey] = value.eveningTime.let { it.hour * 60 + it.minute }
            prefs[timeBudgetKey] = value.timeBudget.name
            prefs[paceKey] = value.pace.name
            prefs[themeKey] = value.theme.name
            prefs[notifyMorningKey] = value.notifyMorning
            prefs[notifyFocusKey] = value.notifyFocus
            prefs[notifyReviewsKey] = value.notifyReviews
            prefs[notifyEveningKey] = value.notifyEvening
            prefs[aiCoachEnabledKey] = value.aiCoachEnabled
            value.programStartedOn?.let {
                prefs[programStartedOnKey] =
                    it.toEpochDay()
            }
                ?: prefs.remove(programStartedOnKey)
            prefs[currentProgramDayKey] = value.currentProgramDay
            prefs[contentVersionKey] = value.contentVersion
            value.lastSeenDayComplete?.let {
                prefs[lastSeenDayCompleteKey] =
                    it.toEpochDay()
            }
                ?: prefs.remove(lastSeenDayCompleteKey)
        }
    }
    override suspend fun clear() {
        store.edit { it.clear() }
    }
    private fun read(prefs: Preferences): UserPreferences = UserPreferences(
        onboardingCompleted = prefs[onboardingCompletedKey] ?: false,
        focusAreas =
        (
            prefs[focusAreasKey]
                ?: emptySet()
            )
            .mapNotNull { raw ->
                Skill.entries.find {
                    it.name == raw
                }
                    ?: run {
                        logger.w(
                            TAG,
                            "Unknown focus area"
                        )
                        null
                    }
            }.toSet(),

        morningTime =
        LocalTime.ofSecondOfDay(
            (
                prefs[morningTimeKey]
                    ?: 510
                )
                .coerceIn(
                    0,
                    1439
                ) * 60L
        ),

        eveningTime =
        LocalTime.ofSecondOfDay(
            (
                prefs[eveningTimeKey]
                    ?: 1260
                )
                .coerceIn(
                    0,
                    1439
                ) * 60L
        ),

        timeBudget = enumValue(prefs[timeBudgetKey], TimeBudget.STANDARD),
        pace = enumValue(prefs[paceKey], ProgramPace.STANDARD),
        theme = enumValue(prefs[themeKey], ThemePreference.SYSTEM),
        notifyMorning = prefs[notifyMorningKey] ?: true,
        notifyFocus = prefs[notifyFocusKey] ?: true,
        notifyReviews = prefs[notifyReviewsKey] ?: true,
        notifyEvening = prefs[notifyEveningKey] ?: true,
        aiCoachEnabled = false,
        programStartedOn = prefs[programStartedOnKey]?.let(LocalDate::ofEpochDay),
        currentProgramDay = (prefs[currentProgramDayKey] ?: 1).coerceAtLeast(1),
        contentVersion = (prefs[contentVersionKey] ?: 0).coerceAtLeast(0),
        lastSeenDayComplete = prefs[lastSeenDayCompleteKey]?.let(LocalDate::ofEpochDay)
    )
    private inline fun <reified T : Enum<T>> enumValue(raw: String?, default: T): T {
        if (raw == null) return default
        return enumValues<T>()
            .find {
                it.name == raw
            }
            ?: run {
                logger.w(
                    TAG,
                    "Unknown preference enum ${T::class.java.simpleName}"
                )
                default
            }
    }
}
private const val TAG = "PreferencesRepository"
