package com.sogou.common.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;

import com.sogou.demo.activity.MainActivity;
import com.sogou.speech.speechsdk.R;

public class LogoActivity extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logo);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                toMainActivity();
            }
        },1000);
    }

    private void toMainActivity(){
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

}
