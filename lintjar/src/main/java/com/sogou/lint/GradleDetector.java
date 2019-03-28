package com.sogou.lint;

import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;

import java.util.Map;


public class GradleDetector extends Detector implements Detector.GradleScanner {

    public static final Issue ISSUE = Issue.create(
            "GradleDetector",
            "GradleDetector",
            "需要放入很多依赖",
            Category.SECURITY, 5, Severity.ERROR,
            new Implementation(GradleDetector.class, Scope.GRADLE_SCOPE));

    private static int sCount = 0;


    @Override
    public void afterCheckProject(Context context) {
        super.afterCheckProject(context);
    }

    final String[] packages = {"io.grpc:grpc","com.google.auth","com.github.sogouspeech:asr-android-sdk","com.github.sogouspeech:common-android-sdk","org.conscrypt:conscrypt-android","com.github.sogouspeech:asr-audio-android-sdk"};

    @Override
    public void visitBuildScript(Context context, Map<String, Object> sharedData) {
        super.visitBuildScript(context, sharedData);
        if (context != null){
            if (!context.getProject().getPackage().equals("com.sogou.lint")){
                String content = context.getContents().toString();
                for (String s : packages){
                    if(!content.contains(s)){
                        context.report(ISSUE, Location.NONE, "缺乏包"+ s);

                    }
                }
            }


        }
    }
}