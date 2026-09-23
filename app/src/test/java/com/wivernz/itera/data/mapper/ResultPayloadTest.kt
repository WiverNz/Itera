package com.wivernz.itera.data.mapper
import com.wivernz.itera.TestLogger
import com.wivernz.itera.data.resultFixtures
import com.wivernz.itera.domain.model.ActivityResult
import com.wivernz.itera.domain.model.BlockValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
class ResultPayloadTest {
    @Test fun everyVariantRoundTripsAndIgnoresUnknownKeys() {
        val codec = ResultPayloadCodec(TestLogger())
        resultFixtures().forEach { result ->
            val payload = codec.encode(result)
            assertEquals(result, codec.decode(payload, 42, "TEMPLATE"))
            assertEquals(
                result,
                codec.decode(
                    payload.dropLast(1) + ",\"future\":true}",
                    42,
                    "TEMPLATE"
                )
            )
        }
        listOf(
            ActivityResult.HabitStack(
                "a",
                "b",
                false,
                null
            ),
            ActivityResult.Reflection(
                null,
                emptyList(),
                null,
                emptyList(),
                null
            ),
            ActivityResult.Eisenhower(
                emptyList(),
                null
            ),
            ActivityResult.Template(
                mapOf(
                    "x" to BlockValue.Chips(
                        emptyList(),
                        null
                    )
                )
            )
        ).forEach { assertEquals(it, codec.decode(codec.encode(it), 1, "TEMPLATE")) }
    }

    @Test fun malformedAndUnknownTypesNeverLogPayload() {
        val logger = TestLogger()
        val codec = ResultPayloadCodec(logger)
        listOf(
            "PRIVATE",
            "{\"type\":\"future\",\"note\":\"PRIVATE\"}",
            "{\"type\":\"review\",\"grade\":\"PRIVATE\"}"
        ).forEach { assertNull(codec.decode(it, 412, "FEYNMAN")) }
        val invalidTime = """
            {"type":"habit_stack","anchor":"PRIVATE","habit":"PRIVATE",
             "nudgeEnabled":true,"nudgeTime":1500}
        """.trimIndent()
        assertNull(codec.decode(invalidTime, 412, "FEYNMAN"))
        assertNull(codec.decode(null, 1, "TEMPLATE"))
        assertEquals(4, logger.warnings.size)
        assertTrue(
            logger.warnings.all {
                it.contains("412") && it.contains("FEYNMAN") && !it.contains("PRIVATE")
            }
        )
    }
}
