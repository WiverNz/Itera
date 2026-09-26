package com.wivernz.itera.core.navigation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
class RouteSerializationTest {
    @Test fun everyRouteRoundTripsWithArguments() {
        val routes = listOf(
            Welcome,
            Goals,
            Rhythm,
            FirstWeek,
            Today,
            Train,
            Progress,
            You,
            Library,
            History,
            TwoMinute(42),
            Eisenhower(42),
            Feynman(42),
            FeynmanFeedback(42),
            Review(42),
            Premortem(42),
            HabitStack(42),
            Combination(42),
            Reflection(42),
            TechniqueDetail("two_minute_rule"),
            ExerciseIntro(
                42,
                "two_minute_rule"
            ),
            ExerciseRun(42, "pareto_principle"),
            ExerciseResult(
                42,
                "two_minute_rule"
            ),
            FocusSession(
                42,
                50,
                "deep_work"
            ),
            FocusSession(
                42,
                25
            ),
            DayComplete(9)
        )
        routes.forEach { assertEquals(it, RouteCodec.decode(RouteCodec.encode(it))) }
    }

    @Test fun malformedOrUnknownInputDoesNotCrash() {
        listOf(
            "",
            "null",
            "{}",
            "{\"type\":\"Future\"}",
            "{\"type\":\"ExerciseIntro\",\"activityId\":\"oops\"}"
        ).forEach {
            assertNull(RouteCodec.decode(it))
        }
    }
}
