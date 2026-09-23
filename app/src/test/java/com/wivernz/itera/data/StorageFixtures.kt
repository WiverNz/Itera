package com.wivernz.itera.data
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.wivernz.itera.TestLogger
import com.wivernz.itera.core.common.FakeClock
import com.wivernz.itera.data.copy.CopyResolver
import com.wivernz.itera.data.database.IteraDatabase
import com.wivernz.itera.data.database.SeedCallback
import com.wivernz.itera.data.database.entity.PlanActivityEntity
import com.wivernz.itera.data.database.entity.TrainingDayEntity
import com.wivernz.itera.data.mapper.ResultPayloadCodec
import com.wivernz.itera.domain.model.ActivityResult
import com.wivernz.itera.domain.model.BlockValue
import com.wivernz.itera.domain.model.ChecklistItem
import com.wivernz.itera.domain.model.CombinationStepResult
import com.wivernz.itera.domain.model.EisenhowerItem
import com.wivernz.itera.domain.model.Likelihood
import com.wivernz.itera.domain.model.PremortemReason
import com.wivernz.itera.domain.model.Quadrant
import com.wivernz.itera.domain.model.RecallGrade
import com.wivernz.itera.domain.model.TechniqueId
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
val fixtureDate: LocalDate = LocalDate.of(2026, 3, 28)
fun dayEntity(date: LocalDate = fixtureDate) = TrainingDayEntity(
    programDay = 1,
    date = date.toEpochDay(),
    status = "PLANNED",
    generatorVersion = 1,
    createdAt = 0
)
fun activityEntity(
    day: Long,
    state: String =
        "AVAILABLE",
    optional: Boolean =
        false
) = PlanActivityEntity(
    trainingDayId = day,
    techniqueId = "two_minute_rule",
    exerciseType = "TEMPLATE",
    source = "PROGRAM",
    orderIndex = 0,
    dayPart = "MORNING",
    copyKey = "activity_program",
    copyArgs = "{\"minutes\":5}",
    estimatedMinutes = 5,
    optional = optional,
    state = state,
    practiceDate = -1
)
fun testDatabase(): IteraDatabase = Room.inMemoryDatabaseBuilder(
    ApplicationProvider.getApplicationContext(),
    IteraDatabase::class.java
).addCallback(SeedCallback(FakeClock())).build()
fun testCopy(): CopyResolver = CopyResolver(
    ApplicationProvider.getApplicationContext<Context>(),
    FakeClock(),
    TestLogger()
)
fun resultFixtures(): List<ActivityResult> = listOf(
    ActivityResult.Template(
        mapOf(
            "text" to BlockValue.Text("private"),
            "items" to BlockValue.Items(
                listOf(
                    ChecklistItem(
                        "1",
                        "task",
                        true,
                        30
                    )
                )
            ),
            "choice" to BlockValue.Choice(
                listOf(
                    "a",
                    "b"
                ),
                1
            ),
            "lists" to BlockValue.Lists(
                listOf("a"),
                listOf("b")
            ),
            "chips" to BlockValue.Chips(
                listOf("a"),
                "custom"
            )
        )
    ),
    ActivityResult.Focus("task", 1500, 1800, 300, true),
    ActivityResult.Eisenhower(
        listOf(
            EisenhowerItem(
                "1",
                "task",
                Quadrant.DO_NOW
            )
        ),
        "1"
    ),

    ActivityResult.Feynman(1, "topic", "explanation", 1, listOf("hard"), "note"),
    ActivityResult.Premortem(
        "project",
        listOf(
            PremortemReason(
                "reason",
                Likelihood.LIKELY
            )
        ),
        "action",
        true
    ),
    ActivityResult.HabitStack("anchor", "habit", true, LocalTime.of(8, 30)),
    ActivityResult.Reflection(
        "well",
        listOf("a"),
        "poorly",
        listOf("b"),
        "tomorrow"
    ),

    ActivityResult.Review(1, "answer", RecallGrade.PARTIAL, "before"),
    ActivityResult.Combination(
        listOf(
            CombinationStepResult(
                TechniqueId("deep_work"),
                "summary",
                Instant.EPOCH
            )
        )
    )
)
