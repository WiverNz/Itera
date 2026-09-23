package com.wivernz.itera.lint;

import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.ConstantEvaluator;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.SourceCodeScanner;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import java.util.List;
import java.util.Map;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.UQualifiedReferenceExpression;
import org.jetbrains.uast.UParenthesizedExpression;
import org.jetbrains.uast.UNamedExpression;
import org.jetbrains.uast.visitor.AbstractUastVisitor;

/** Bootstrap gates only; full hit-region verification belongs to Compose UI tests. */
public final class BootstrapDetector extends Detector implements SourceCodeScanner {
    private static final Implementation IMPLEMENTATION =
            new Implementation(BootstrapDetector.class, Scope.JAVA_FILE_SCOPE);
    public static final Issue TEXT = Issue.create(
            "ComposeHardcodedText", "Hardcoded Compose text",
            "Put user-facing text in all four localized string resources and use stringResource.",
            Category.I18N, 8, Severity.ERROR, IMPLEMENTATION);
    public static final Issue TOUCH = Issue.create(
            "TouchTargetSizeCheck", "Undersized interactive control",
            "Explicit interactive-control dimensions must be at least 44 dp. Also verify actual hit regions in UI tests.",
            Category.A11Y, 8, Severity.ERROR, IMPLEMENTATION);

    @Override public List<String> getApplicableMethodNames() {
        return List.of("Text", "BasicText", "Button", "TextButton", "OutlinedButton",
                "FilledTonalButton", "IconButton", "FilledIconButton", "IconToggleButton");
    }

    @Override public void visitMethodCall(JavaContext context, UCallExpression call, PsiMethod method) {
        String owner = method.getContainingClass() == null ? "" : method.getContainingClass().getQualifiedName();
        if (owner == null || !owner.startsWith("androidx.compose.")) return;
        boolean text = call.getMethodName().equals("Text") || call.getMethodName().equals("BasicText");
        for (Map.Entry<UExpression, PsiParameter> entry :
                context.getEvaluator().computeArgumentMapping(call, method).entrySet()) {
            UExpression argument = entry.getKey();
            if (text && entry.getValue().getName().equals("text")) {
                Object value = ConstantEvaluator.evaluate(context, argument);
                if (value instanceof String && !((String) value).isBlank()) {
                    context.report(TEXT, argument, context.getLocation(argument),
                            "Use a localized string resource instead of constant Compose text");
                }
            } else if (!text && entry.getValue().getName().equals("modifier")) {
                boolean[] tooSmall = {false};
                argument.accept(new AbstractUastVisitor() {
                    @Override public boolean visitCallExpression(UCallExpression dimension) {
                        if (!List.of("requiredSize", "size", "width", "height", "requiredWidth", "requiredHeight")
                                .contains(dimension.getMethodName())) return false;
                        for (UExpression value : dimension.getValueArguments()) {
                            if (value instanceof UNamedExpression) value = ((UNamedExpression) value).getExpression();
                            while (value instanceof UParenthesizedExpression)
                                value = ((UParenthesizedExpression) value).getExpression();
                            if (value instanceof UQualifiedReferenceExpression) {
                                UQualifiedReferenceExpression qualified = (UQualifiedReferenceExpression) value;
                                if (!qualified.getSelector().asSourceString().trim().equals("dp")) continue;
                                Object number = ConstantEvaluator.evaluate(context, qualified.getReceiver());
                                if (number instanceof Number && ((Number) number).doubleValue() < 44) tooSmall[0] = true;
                            }
                        }
                        return false;
                    }
                });
                if (tooSmall[0]) context.report(TOUCH, argument, context.getLocation(argument),
                        "Interactive controls require dimensions of at least 44 dp");
            }
        }
    }
}
