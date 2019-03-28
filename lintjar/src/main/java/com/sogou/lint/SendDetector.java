package com.sogou.lint;

import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;

import java.util.Arrays;
import java.util.List;


public class SendDetector extends Detector implements Detector.JavaPsiScanner {

    public static final Issue ISSUE = Issue.create(
            "release",
            "请调用release方法",
            "请调用release方法",
            Category.SECURITY, 5, Severity.ERROR,
            new Implementation(SendDetector.class, Scope.JAVA_FILE_SCOPE));

    private boolean hasReleaseUsed = false;
    private boolean hasStopUsed = false;

    @Override
    public List<String> getApplicableMethodNames() {
        return Arrays.asList("send");
    }

    @Override
    public void afterCheckProject(Context context) {
        super.afterCheckProject(context);
        if (!hasReleaseUsed){
            context.report(ISSUE, Location.NONE,"没有使用release");
        }

        if (!hasStopUsed){
            context.report(ISSUE, Location.NONE,"没有使用stop");

        }

    }

    @Override
    public void visitMethod(JavaContext context, JavaElementVisitor visitor, PsiMethodCallExpression call, PsiMethod method) {
        if (context.getEvaluator().isMemberInClass(method, "com.sogou.sogouspeech.SogoSpeech")) {


//            System.out.println("==== has send  ====" +method.toString());
//            System.out.println("==== has send 2 ====" +visitor.toString());
//            System.out.println("==== has send 3 ====" +call.toString());
            for(PsiExpression psi : call.getArgumentList().getExpressions()){
                if (psi.textMatches("SpeechConstants.Command.ASR_ONLINE_DESTROY")){
                    hasReleaseUsed = true;
                }

                if (psi.textMatches("SpeechConstants.Command.ASR_ONLINE_STOP")){
                    hasStopUsed = true;
                    boolean hasFind = findParentByMethodName("onEnd",call);
                    if (!hasFind){
                        context.report(ISSUE, context.getLocation(call.getMethodExpression()),"onEnd里面没有调用ASR_ONLINE_STOP");
                    }
                }
            }

        }
    }

    private boolean findParentByMethodName(String parentsName,PsiElement method){
        if (method.getParent() != null){
            if (method.getParent() instanceof  PsiMethod){
                System.out.println("==== stop  ====" +((PsiMethod) method.getParent()).getName());

                if (parentsName.equalsIgnoreCase(((PsiMethod) method.getParent()).getName())){
                    return  true;
                }
            }
            return findParentByMethodName(parentsName,method.getParent());
        }else {
            return false;
        }
    }

}