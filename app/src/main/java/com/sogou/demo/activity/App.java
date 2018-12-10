// Copyright 2018 Sogou Inc. All rights reserved. 
// Use of this source code is governed by the Apache 2.0 
// license that can be found in the LICENSE file. 
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