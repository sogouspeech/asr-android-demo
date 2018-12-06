package com.sogou.demo.activity;

import android.support.multidex.MultiDexApplication;

import com.sogou.sogouspeech.SogoSpeech;

public class App extends MultiDexApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        // 传入从知音平台申请的域名，不需要scheme，例如*.*.sogou.com
        SogoSpeech.initAuth(this,"");
    }
}
