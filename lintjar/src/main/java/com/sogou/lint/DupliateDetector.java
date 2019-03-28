package com.sogou.lint;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiNewExpression;

import java.util.Collections;
import java.util.List;


public class DupliateDetector extends Detector implements Detector.JavaPsiScanner {

    public static final Issue ISSUE = Issue.create(
            "LogUsage",
            "避免调用TEST",
            "避免调用TEST，应该使用统一工具类",
            Category.SECURITY, 5, Severity.ERROR,
            new Implementation(DupliateDetector.class, Scope.JAVA_FILE_SCOPE));

    private static int sCount = 0;

    @Override
    public List<String> getApplicableConstructorTypes() {
        return Collections.singletonList("com.sogou.sogouspeech.SogoSpeech");
    }

    @Override
    public void afterCheckProject(Context context) {
        super.afterCheckProject(context);
        sCount = 0;
    }

    @Override
    public void beforeCheckProject(Context context) {
        super.beforeCheckProject(context);
        sCount = 0;
    }

    @Override
    public void visitConstructor(@NonNull JavaContext context, @Nullable JavaElementVisitor visitor,
                                 @NonNull PsiNewExpression node, @NonNull PsiMethod constructor) {
        String name =  context.getProject().getName();

            sCount ++;

            if (sCount > 1) {
                context.report(ISSUE, node, context.getLocation(node), "请勿多次调用sogoSpeech");
                sCount = 0;
            }

    }
}