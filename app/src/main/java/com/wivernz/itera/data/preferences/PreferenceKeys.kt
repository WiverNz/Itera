package com.wivernz.itera.data.preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
internal val onboardingCompletedKey = booleanPreferencesKey("onboarding_completed")
internal val focusAreasKey = stringSetPreferencesKey("focus_areas")
internal val morningTimeKey = intPreferencesKey("morning_time_minutes")
internal val eveningTimeKey = intPreferencesKey("evening_time_minutes")
internal val timeBudgetKey = stringPreferencesKey("time_budget")
internal val paceKey = stringPreferencesKey("pace")
internal val themeKey = stringPreferencesKey("theme")
internal val notifyMorningKey = booleanPreferencesKey("notify_morning")
internal val notifyFocusKey = booleanPreferencesKey("notify_focus")
internal val notifyReviewsKey = booleanPreferencesKey("notify_reviews")
internal val notifyEveningKey = booleanPreferencesKey("notify_evening")
internal val aiCoachEnabledKey = booleanPreferencesKey("ai_coach_enabled")
internal val programStartedOnKey = longPreferencesKey("program_started_on")
internal val currentProgramDayKey = intPreferencesKey("current_program_day")
internal val contentVersionKey = intPreferencesKey("content_version")
internal val lastSeenDayCompleteKey = longPreferencesKey("last_seen_day_complete")
