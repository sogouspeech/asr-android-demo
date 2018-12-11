// Copyright 2018 Sogou Inc. All rights reserved. 
// Use of this source code is governed by the Apache 2.0 
// license that can be found in the LICENSE file. 
package com.sogou.demo.activity;

import android.support.multidex.MultiDexApplication;

import com.sogou.sogouspeech.SogoSpeech;
import com.sogou.sogouspeech.ZhiyinInitInfo;

public class App extends MultiDexApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        ZhiyinInitInfo.Builder builder = new ZhiyinInitInfo.Builder();
        // 传入从知音平台申请获得的域名，从知音平台获取的appid，设备的uuid。如果需要刷新token则传入appkey，否则传入token
        ZhiyinInitInfo initInfo = builder.setBaseUrl("").setUuid("").setAppid("").setAppkey("").setToken("").create();

        SogoSpeech.initZhiyinInfo(this,initInfo);

    }
}