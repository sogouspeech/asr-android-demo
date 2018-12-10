// Copyright 2018 Sogou Inc. All rights reserved. 
// Use of this source code is governed by the Apache 2.0 
// license that can be found in the LICENSE file. 
package com.sogou.common.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.sogou.common.utils.ScreenUtil;
import com.sogou.speech.speechsdk.R;

/**
 * @author xuq
 *
 */
public class BaseActivity extends Activity {
    private static final String TAG = BaseActivity.class.getSimpleName();
    protected int statusBarHeight;
    private int startMode = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            //透明状态栏
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            //透明导航栏
//            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            statusBarHeight = ScreenUtil.getStatusBarHeight(this);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    /**
     * 自定义处理
     *
     * @param intent
     * @param mode   1：从左向右  2：从下向上
     */
    public void startActivity(Intent intent, int mode) {
        startMode = mode;
        startActivity(intent);
    }

    /**
     * 自定义处理
     *
     * @param intent
     * @param mode
     */
    public void startActivityForResult(Intent intent, int requestCode, Bundle options, int mode) {
        startMode = mode;
        startActivityForResult(intent, requestCode, options);
    }

    @Override
    public void startActivity(Intent intent) {
        super.startActivity(intent);
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode, Bundle options) {
        super.startActivityForResult(intent, requestCode, options);
        switch (startMode) {
            case 1:
                overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
                break;
            case 2:
                overridePendingTransition(R.anim.push_bottom_in, R.anim.push_bottom_out);
                break;
            default:
                overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
        }
        startMode = 0;
    }

    @Override
    public void finish() {
        super.finish();
        if (this instanceof LogoActivity) {
            return;
        }
        overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
    }

    /**
     * 沉浸式布局
     *
     * @param view
     */
    protected void setImmerseLayout(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            int statusBarHeight = ScreenUtil.getStatusBarHeight(this);
            view.setPadding(0, statusBarHeight, 0, 0);
        }
    }

    public void setTitleBar(String title) {
        TextView tvName = (TextView) findViewById(R.id.tv_bar_title);
        tvName.setText(title);
    }


}