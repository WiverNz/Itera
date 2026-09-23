package com.wivernz.itera.lint;

import static com.android.tools.lint.checks.infrastructure.TestFiles.kotlin;
import static com.android.tools.lint.checks.infrastructure.TestLintTask.lint;
import org.junit.Test;

public class BootstrapDetectorTest {
    private static final String STUB = """
            package androidx.compose.material3
            fun Text(text: String) {}
            fun Button(modifier: Any) {}
            """;

    @Test public void rejectsLiteralAndConstantText() {
        lint().allowMissingSdk().files(kotlin(STUB), kotlin("""
                package sample
                import androidx.compose.material3.Text
                private const val greeting = "hello"
                fun content() { Text("hello"); Text(text = greeting) }
                """)).issues(BootstrapDetector.TEXT).run().expectErrorCount(2);
    }

    @Test public void acceptsResourceTextAndUnrelatedTextFunction() {
        lint().allowMissingSdk().files(kotlin(STUB), kotlin("""
                package sample
                fun resource(id: Int): String = id.toString()
                fun Text(text: String) {}
                fun content() { androidx.compose.material3.Text(resource(1)); Text("internal") }
                """)).issues(BootstrapDetector.TEXT).run().expectClean();
    }

    @Test public void rejectsSmallControlButAcceptsMinimumSize() {
        lint().allowMissingSdk().files(kotlin(STUB), kotlin("""
                package sample
                import androidx.compose.material3.Button
                val Int.dp: Int get() = this
                object Modifier { fun size(value: Int): Any = value }
                fun content() { Button(Modifier.size(24.dp)); Button(Modifier.size(44.dp)) }
                """)).issues(BootstrapDetector.TOUCH).run().expectErrorCount(1);
    }
}
