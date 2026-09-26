@file:Suppress("ktlint:standard:max-line-length")

package com.wivernz.itera.feature.train

import androidx.lifecycle.SavedStateHandle
import com.wivernz.itera.domain.EngineHarness
import com.wivernz.itera.domain.model.ActivityResult
import com.wivernz.itera.domain.model.ActivitySource
import com.wivernz.itera.domain.model.ActivityState
import com.wivernz.itera.domain.model.MasteryLevel
import com.wivernz.itera.domain.model.PracticeKind
import com.wivernz.itera.domain.model.PracticeSummary
import com.wivernz.itera.domain.model.RecallGrade
import com.wivernz.itera.domain.model.ReviewItem
import com.wivernz.itera.domain.model.ReviewState
import com.wivernz.itera.domain.model.Skill
import com.wivernz.itera.domain.model.Technique
import com.wivernz.itera.domain.model.TechniqueId
import com.wivernz.itera.domain.model.practiceSummary
import com.wivernz.itera.domain.repository.TechniqueCatalogRepository
import com.wivernz.itera.feature.MainDispatcherRule
import com.wivernz.itera.feature.await
import com.wivernz.itera.feature.awaitFirst
import com.wivernz.itera.feature.exercise.runner.ExerciseBody
import com.wivernz.itera.feature.runnerViewModel
import com.wivernz.itera.feature.subscribe
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

open class TrainTestBase {
    @get:Rule val main = MainDispatcherRule()
    protected lateinit var h: EngineHarness

    @Before fun prepare() {
        h = EngineHarness()
        runBlocking { h.ensureToday() }
    }

    @After fun close() {
        main.clearViewModels()
        h.close()
    }
    protected fun train(catalog: TechniqueCatalogRepository = h.catalog) = main.track(
        TrainViewModel(
            h.plans,
            catalog,
            h.reviews,
            h.prefs,
            h.observeTechniques,
            h.ensureToday,
            h.analytics,
            h.clock,
            com.wivernz.itera.domain.review.OpenDueReviewUseCase(
                h.ensureToday,
                h.plans,
                h.reviews,
                h.tx,
                h.clock
            )
        )
    )
    protected fun library(saved: SavedStateHandle = SavedStateHandle()) =
        main.track(LibraryViewModel(saved, h.catalog, h.observeTechniques, h.analytics))
    protected fun detail(id: String) = main.track(
        TechniqueDetailViewModel(
            SavedStateHandle(mapOf("technique" to id)),
            h.catalog,
            h.observeTechniques,
            h.progress,
            h.addManual,
            h.analytics
        )
    )
}

@RunWith(RobolectricTestRunner::class)
class TrainViewModelTest : TrainTestBase() {
    @Test fun completingTodayDoesNotPointItsNodeAtTomorrowsExercise() = runBlocking {
        val vm = train()
        val stop = vm.state.subscribe()
        try {
            vm.state.await { !it.loading }
            h.trainFullDay()
            assertEquals(2, h.programDay())
            assertEquals(1, vm.state.value.programDay)
            h.nextMorning()
            vm.onEvent(TrainUiEvent.Refresh)
            assertEquals(2, vm.state.await { it.programDay == 2 }.programDay)
        } finally {
            stop()
        }
    }

    @Test fun reviewAddedAfterPlanGenerationOpensOnceAndCompletes() = runBlocking {
        h.unlock(10)
        val day = h.ensureToday()
        val id = h.reviews.insert(
            ReviewItem(
                0,
                TechniqueId(
                    "spaced_repetition"
                ),
                null, "", day.activities.first().id, "answer", 0, h.today, null, ReviewState.DUE
            ),
            h.clock.instant()
        )
        val vm = train()
        vm.state.await { !it.loading && it.review?.id == id }
        vm.onEvent(TrainUiEvent.Review)
        val first = (vm.effects.awaitFirst() as TrainEffect.Exercise).activity
        assertEquals(id, first.reviewItemId)
        vm.onEvent(TrainUiEvent.Review)
        assertEquals(first.id, (vm.effects.awaitFirst() as TrainEffect.Exercise).activity.id)
        h.submitReview(first.id, "answer", RecallGrade.SOLID).getOrThrow()
        assertNull(vm.state.await { !it.loading && it.review == null }.review)
    }

    @Test fun combinationNodeOpensParent() = runBlocking {
        h.prefs.update { it.copy(currentProgramDay = 14) }
        h.nextMorning()
        val vm = train()
        vm.state.await { !it.loading && it.programDay == 14 }
        vm.onEvent(TrainUiEvent.OpenDay(14))
        assertEquals(
            com.wivernz.itera.domain.model.ExerciseType.COMBINATION,
            (vm.effects.awaitFirst() as TrainEffect.Exercise).activity.exerciseType
        )
    }

    @Test fun weekBoundariesAndNodeStates() = runBlocking {
        val vm = train()
        val stop = vm.state.subscribe()
        try {
            for (day in listOf(1, 7, 8, 14, 15, 30)) {
                h.prefs.update { it.copy(currentProgramDay = day) }
                h.nextMorning()
                vm.onEvent(TrainUiEvent.Refresh)
                val s = vm.state.await { !it.loading && it.programDay == day }
                assertEquals((day - 1) / 7 + 1, s.week)
                assertEquals(7, s.nodes.size)
                assertEquals(1, s.nodes.count { it.state == TrainNodeState.TODAY })
                assertTrue(
                    s.nodes.filter {
                        it.day > day
                    }.all { it.state == TrainNodeState.FUTURE }
                )
                assertTrue(s.nodes.filter { it.day < day }.all { it.state == TrainNodeState.PAST })
            }
        } finally {
            stop()
        }
    }

    @Test fun dueCardOrdersReviewsAndCountsExtras() = runBlocking {
        val vm = train()
        val stop = vm.state.subscribe()
        try {
            assertNull(vm.state.await { !it.loading }.review)
            val day = h.ensureToday()
            val activity = day.activities.first()
            fun review(n: Int) = ReviewItem(
                0,
                TechniqueId(
                    "spaced_repetition"
                ),
                null, "", activity.id, "", n,
                h.today.minusDays(
                    n.toLong()
                ),
                null, ReviewState.SCHEDULED
            )
            val first = h.reviews.insert(review(0), h.clock.instant())
            assertEquals(first, vm.state.await { it.review != null }.review!!.id)
            h.reviews.insert(review(1), h.clock.instant())
            val highest = h.reviews.insert(review(2), h.clock.instant())
            val s = vm.state.await { it.moreReviews == 2 }
            assertEquals(highest, s.review!!.id)
            assertEquals(2, s.unlockedCount)
        } finally {
            stop()
        }
    }

    @Test fun errorLeavesLibraryNavigationAvailable() {
        val bad = object : TechniqueCatalogRepository by h.catalog {
            override suspend fun catalog(): List<Technique> = throw IllegalStateException()
        }
        val vm = train(bad)
        assertTrue(vm.state.await { it.failed }.failed)
        vm.onEvent(TrainUiEvent.Library)
        assertEquals(TrainEffect.Library, vm.effects.awaitFirst())
    }

    @Test fun todayAndPastNavigateButFutureDoesNotMutatePlan() = runBlocking {
        val vm = train()
        vm.state.await { !it.loading }
        vm.onEvent(TrainUiEvent.OpenDay(1))
        assertTrue(vm.effects.awaitFirst() is TrainEffect.Exercise)
        val before = h.ensureToday().activities.size
        vm.onEvent(TrainUiEvent.OpenDay(2))
        assertEquals(before, h.ensureToday().activities.size)
        vm.onEvent(TrainUiEvent.OpenDay(0))
        assertEquals(TrainEffect.History, vm.effects.awaitFirst())
    }
}

@RunWith(RobolectricTestRunner::class)
class LibraryViewModelTest : TrainTestBase() {
    @Test fun allTechniquesAndEveryFilterIncludeLockedRows() {
        val vm = library()
        val stop = vm.state.subscribe()
        try {
            val all = vm.state.await { it.rows.size == 14 && it.unlockedCount == 2 }
            assertEquals(12, all.rows.count { !it.unlocked })
            for ((skill, count) in Skill.entries.zip(listOf(4, 3, 2, 3, 2))) {
                vm.onEvent(LibraryUiEvent.Filter(skill))
                val s = vm.state.await { it.filter == skill }
                assertEquals(count, s.rows.size)
                assertTrue(s.rows.all { it.technique.skill == skill })
            }
        } finally {
            stop()
        }
    }

    @Test fun dayNinePrototypeOrderAndDayTwentyUnlocks() = runBlocking {
        h.unlock(9)
        val vm = library()
        val stop = vm.state.subscribe()
        try {
            val s = vm.state.await { it.rows.size == 14 && it.unlockedCount == 10 }
            assertEquals(
                listOf("daily_reflection", "two_minute_rule", "pomodoro", "eisenhower_matrix", "five_second_rule", "habit_stacking", "feynman_technique", "two_list_strategy", "deep_work", "pareto_principle", "spaced_repetition", "information_diet", "premortem", "one_percent_improvement"),
                s.rows.map {
                    it.technique.id.value
                }
            )
            h.unlock(20)
            assertEquals(14, vm.state.await { it.unlockedCount == 14 }.rows.size)
        } finally {
            stop()
        }
    }

    @Test fun filterSurvivesRecreationAndLockedTapEmitsDetail() {
        val saved = SavedStateHandle(mapOf("filter" to Skill.LEARNING.name))
        val vm = library(saved)
        val s = vm.state.await { it.rows.size == 2 }
        assertEquals(Skill.LEARNING, s.filter)
        vm.onEvent(LibraryUiEvent.Open(s.rows.last().technique.id))
        assertEquals(s.rows.last().technique.id, vm.effects.awaitFirst())
    }

    @Test fun allLockedOrderUsesIntroDay() = runBlocking {
        val rows = libraryRows(h.catalog.catalog(), emptyList(), null)
        assertTrue(rows.all { !it.unlocked && it.level == MasteryLevel.NONE })
        assertEquals(
            rows.map { it.technique.introDay ?: 0 }.sorted(),
            rows.map {
                it.technique.introDay
                    ?: 0
            }
        )
    }
}

@RunWith(RobolectricTestRunner::class)
class TechniqueDetailViewModelTest : TrainTestBase() {
    @Test fun lockedPeekAndRelatedDoNotCreatePractice() = runBlocking {
        val before = h.ensureToday().activities.size
        val vm = detail("premortem")
        val s = vm.state.await { !it.loading }
        assertFalse(s.progress!!.unlocked)
        assertTrue(s.technique!!.explanation.isNotBlank())
        assertTrue(s.related.isNotEmpty())
        vm.onEvent(TechniqueDetailUiEvent.Practice)
        assertEquals(before, h.ensureToday().activities.size)
        vm.onEvent(TechniqueDetailUiEvent.Related(s.related.first().id))
        assertEquals(TechniqueDetailEffect.Related(s.related.first().id), vm.effects.awaitFirst())
    }

    @Test fun repeatedPracticeTapCreatesOneAndSkipsIntro() = runBlocking {
        val vm = detail("two_minute_rule")
        vm.state.await { !it.loading }
        val before = h.ensureToday().activities.size
        repeat(5) { vm.onEvent(TechniqueDetailUiEvent.Practice) }
        val effect = vm.effects.awaitFirst() as TechniqueDetailEffect.Practice
        assertEquals(ExerciseBody.Template, effect.body)
        assertEquals(before + 1, h.ensureToday().activities.size)
        assertEquals(ActivitySource.MANUAL, h.plans.activity(effect.activityId)!!.source)
    }

    @Test fun timerUsesActualDurationAndEmptyTemplateStillRequiresCompletion() = runBlocking {
        h.unlock(9)
        for ((id, expected) in listOf(
            "pomodoro" to ExerciseBody.Focus(25),
            "five_second_rule" to ExerciseBody.Template
        )) {
            val vm = detail(id)
            vm.state.await { !it.loading }
            vm.onEvent(TechniqueDetailUiEvent.Practice)
            val effect = vm.effects.awaitFirst() as TechniqueDetailEffect.Practice
            assertEquals(expected, effect.body)
            assertEquals(ActivityState.AVAILABLE, h.plans.activity(effect.activityId)!!.state)
        }
    }

    @Test fun recentPracticeIsCappedNewestFirstAcrossMonthsAndCorruptionIsLocal() = runBlocking {
        val ids = mutableListOf<Long>()
        repeat(7) {
            val id = h.addManual(TechniqueId("two_minute_rule")).getOrThrow()
            h.complete(id, h.resultFor(h.plans.activity(id)!!), note = "note $it").getOrThrow()
            ids += id
            h.clock.setDate(h.today.plusDays(10))
        }
        h.db.openHelper.writableDatabase.execSQL(
            "UPDATE plan_activity SET resultPayload = 'broken' WHERE id = ?",
            arrayOf(ids.last())
        )
        val vm = detail("two_minute_rule")
        val s = vm.state.await { !it.loading }
        assertEquals(ids.takeLast(5).reversed(), s.history.map { it.id })
        assertEquals(PracticeSummary.Text(s.technique!!.name), s.history.first().summary)
        assertEquals("note 6", s.history.first().note)
        assertEquals(7, s.progress!!.totalUses)
        assertEquals(MasteryLevel.APPLIED, s.progress.level)
    }

    @Test fun everyResultVariantHasASummaryAndNullFallsBack() = runBlocking {
        val fallback = "Technique"
        val variants =
            listOf<ActivityResult>(
                ActivityResult.Template(
                    emptyMap()
                ),
                ActivityResult.Focus(
                    "task",
                    1500,
                    120,
                    0,
                    false
                ),
                ActivityResult.Eisenhower(
                    emptyList(),
                    null
                ),
                ActivityResult.Feynman(
                    1,
                    "topic",
                    "body",
                    1,
                    emptyList(),
                    null
                ),
                ActivityResult.Premortem(
                    "project",
                    emptyList(),
                    null,
                    false
                ),
                ActivityResult.HabitStack(
                    "coffee",
                    "read",
                    false,
                    null
                ),
                ActivityResult.Reflection(
                    null,
                    emptyList(),
                    null,
                    emptyList(),
                    "tomorrow"
                ),
                ActivityResult.Review(
                    1,
                    "",
                    RecallGrade.SOLID,
                    ""
                ),
                ActivityResult.Combination(emptyList())
            )
        variants.forEach { assertNotNull(practiceSummary(it, fallback)) }
        assertEquals(PracticeSummary.Text(fallback), practiceSummary(null, fallback))
        assertEquals(
            PracticeSummary.Count(PracticeKind.MINUTES, 2),
            practiceSummary(variants[1], fallback)
        )
        assertEquals(PracticeSummary.Topic("topic"), practiceSummary(variants[3], fallback))
    }
}

@RunWith(RobolectricTestRunner::class)
class PracticeNowTest : TrainTestBase() {
    @Test fun directTemplateRunStartsOnlyOnceAndAbandonsWithoutCredit() = runBlocking {
        val id = h.addManual(TechniqueId("two_minute_rule")).getOrThrow()
        val runner = h.runnerViewModel(id)
        runner.state.await { !it.loading }
        runner.enterRun()
        com.wivernz.itera.feature.eventually {
            h.plans.activity(id)?.takeIf {
                it.state ==
                    ActivityState.IN_PROGRESS
            }
        }
        runner.enterRun()
        runner.leave()
        runner.effects.awaitFirst()
        assertEquals(ActivityState.AVAILABLE, h.plans.activity(id)!!.state)
        assertEquals(MasteryLevel.NONE, h.mastery("two_minute_rule"))
    }

    @Test fun manualActivityAppearsOnTodayAndCompletionCountsTowardMastery() = runBlocking {
        val vm = detail("two_minute_rule")
        vm.state.await { !it.loading }
        vm.onEvent(TechniqueDetailUiEvent.Practice)
        val effect = vm.effects.awaitFirst() as TechniqueDetailEffect.Practice
        val today = h.ensureToday()
        val row = today.activities.single { it.id == effect.activityId }
        assertEquals(ActivitySource.MANUAL, row.source)
        assertTrue(row.optional)
        assertEquals(MasteryLevel.NONE, h.mastery("two_minute_rule"))
        h.complete(row.id, h.resultFor(row)).getOrThrow()
        assertEquals(MasteryLevel.MET, h.mastery("two_minute_rule"))
        assertEquals(
            1,
            h.observeTechniques().awaitFirst().single {
                it.techniqueId ==
                    row.techniqueId
            }.totalUses
        )
    }
}
