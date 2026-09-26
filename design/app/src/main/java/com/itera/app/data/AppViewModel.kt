package com.itera.app.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.itera.app.model.DayStep
import com.itera.app.model.Feeling
import com.itera.app.model.Mastery
import com.itera.app.model.Program
import com.itera.app.model.Skill
import com.itera.app.model.Technique
import com.itera.app.model.ThemeMode
import java.time.LocalDate
import java.time.LocalTime

/** One logged practice. [note] is the user's own text and is never translated. */
data class LogEntry(val date: LocalDate, val technique: Technique, val note: String = "")

/**
 * In-memory app state for the UX prototype. No backend and no persistence yet:
 * replace with a repository (Room / DataStore) when the product logic is built.
 */
class AppViewModel : ViewModel() {

    // ---- onboarding & settings
    var onboarded by mutableStateOf(false)
        private set
    val focusSkills = mutableStateListOf(Skill.Focus, Skill.Learning)
    var morningTime by mutableStateOf(LocalTime.of(8, 30))
    var eveningTime by mutableStateOf(LocalTime.of(21, 0))
    var dailyMinutes by mutableStateOf(15)
    var themeMode by mutableStateOf(ThemeMode.System)
    var pace by mutableStateOf(1) // 0 gentle, 1 standard, 2 intense
    val notifications = mutableStateListOf(true, true, true, true)

    fun toggleFocusSkill(skill: Skill) {
        if (skill in focusSkills) {
            focusSkills.remove(skill)
        } else {
            focusSkills.add(skill)
            while (focusSkills.size > 2) focusSkills.removeAt(0)
        }
    }

    fun resetProgram(erase: Boolean = false) {
        log.clear(); practiceDays.clear(); integrated.clear(); completed.clear()
        programDay = 1; startDate = LocalDate.now(); lastFeeling = null; lastNote = ""; tomorrowChange = ""; carriedChange = ""
        if (erase) {
            onboarded = false; focusSkills.clear(); morningTime = LocalTime.of(8, 30); eveningTime = LocalTime.of(21, 0)
            dailyMinutes = 15; pace = 1; themeMode = ThemeMode.System
        }
    }

    fun finishOnboarding() {
        onboarded = true
    }

    // ---- program & today
    var startDate: LocalDate by mutableStateOf(LocalDate.now())
        private set
    var programDay by mutableStateOf(1)
        private set
    val completed = mutableStateListOf<DayStep>()
    var lastFeeling by mutableStateOf<Feeling?>(null)
    var lastNote by mutableStateOf("")
    var tomorrowChange by mutableStateOf("")
    var carriedChange by mutableStateOf("")
        private set

    val todaysTechnique: Technique? get() = Program.techniqueFor(programDay)
    val isCombinationDay: Boolean get() = todaysTechnique == null
    val focusMinutes: Int
        get() = if (todaysTechnique == Technique.DeepWork || isCombinationDay || programDay > 8) 50 else 25

    fun complete(step: DayStep, technique: Technique? = null) {
        if (step !in completed) completed.add(step)
        technique?.let { record(it, if (step == DayStep.Exercise) lastNote else "") }
    }

    fun startNextDay() {
        carriedChange = tomorrowChange
        tomorrowChange = ""
        completed.clear()
        lastFeeling = null
        lastNote = ""
        programDay += 1
    }

    // ---- practice log & mastery
    val log = mutableStateListOf<LogEntry>()
    private val practiceDays = mutableStateMapOf<Technique, Set<LocalDate>>()

    fun record(t: Technique, note: String = "", date: LocalDate = LocalDate.now()) {
        log.add(0, LogEntry(date, t, note))
        practiceDays[t] = practiceDays[t].orEmpty() + date
    }

    fun practiceCount(t: Technique): Int = log.count { it.technique == t }

    /** Met: tried once. Practiced: 3 different days. Applied: 6 uses. Integrated: used on a combination day. */
    fun masteryOf(t: Technique): Mastery? {
        val uses = practiceCount(t)
        val days = practiceDays[t].orEmpty().size
        return when {
            uses == 0 -> null
            t in integrated -> Mastery.Integrated
            uses >= 6 -> Mastery.Applied
            days >= 3 -> Mastery.Practiced
            else -> Mastery.Met
        }
    }

    private val integrated = mutableStateListOf<Technique>()

    /** Techniques used together on a combination day reach "Integrated". */
    fun markIntegrated(vararg techniques: Technique) {
        techniques.filter { it !in integrated && practiceCount(it) > 0 }.forEach { integrated.add(it) }
    }

    fun isUnlocked(t: Technique): Boolean = t in Program.unlocked(programDay)

    fun practicedOn(date: LocalDate): List<Skill> =
        log.filter { it.date == date }.map { it.technique.skill }.distinct()

    // ---- demo data
    /** Jumps to Day 9 with a realistic history, so every screen has something to show. */
    fun loadDemo() {
        val today = LocalDate.now()
        startDate = today.minusDays(9)
        log.clear(); practiceDays.clear(); integrated.clear(); completed.clear()
        val plan = Program.days.take(8)
        var day = 0
        for (offset in 9 downTo 1) {
            if (offset == 3) continue // a rest day: nothing resets
            val date = today.minusDays(offset.toLong())
            val t = plan[day]
            record(t, date = date)
            if (day >= 1) record(Technique.Pomodoro, date = date)
            if (day >= 2 && day % 2 == 0) record(Technique.TwoMinute, date = date)
            record(Technique.DailyReflection, date = date)
            day++
        }
        programDay = 9
        onboarded = true
        carriedChange = ""
    }
}
