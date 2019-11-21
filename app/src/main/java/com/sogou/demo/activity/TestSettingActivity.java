// Copyright 2018 Sogou Inc. All rights reserved. 
// Use of this source code is governed by the Apache 2.0 
// license that can be found in the LICENSE file. 
package com.sogou.demo.activity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.Switch;

import com.sogou.common.activity.BaseActivity;
import com.sogou.sogocommon.utils.CommonSharedPreference;
import com.sogou.sogocommon.utils.LogUtil;
import com.sogou.sogouspeech.SogoSpeechSettings;
import com.sogou.sogouspeech.paramconstants.LanguageCode;
import com.sogou.sogouspeech.paramconstants.SpeechConstants;
import com.sogou.speech.speechsdk.R;

public class TestSettingActivity extends BaseActivity implements View.OnClickListener {

    public static final String KEY_Enable_AGC = "enable_agc";
    public static final String KEY_Enable_Record = "enable_record";
    public static final String KEY_Packge_length = "package_length";

    private static final String[] mIntervals = new String[]{"200ms", "300ms", "500ms"};
    private static final String[] mVoices = new String[]{"PCM", "flac", "speex"};
    private static final String[] mEnvironment = new String[]{"default","search"};

    private Switch agcSwitch;
    private Switch recordSwitch;
    private Button deleteLogButton;
    private Spinner spinnerLanguage;
    private Spinner environmentalSpinner;
    private Spinner voiceEncodeSpinner;
    private EditText vadTimeText;
    private EditText vadEosText;
    private EditText vadBosText;
    private EditText longRecordTimeText;

    private Button save;


    private Switch vadSwitch,wakeupSwitch,continiousWakeupSwitch,enableLongTimeSwitch;
    String[] languageArr = {"汉语-普通话", "English", "日本语", "한국어"};
    private Spinner sendSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_setting);
        setTitleBar("设置");
        setImmerseLayout(findViewById(R.id.test_commonbar));
        initViews();

        findViewById(R.id.iv_back).setVisibility(View.GONE);
        SogoSpeechSettings settings = SogoSpeechSettings.shareInstance();
        SogoSpeechSettings.shareInstance()
                .setProperty(SpeechConstants.Parameter.ASR_ONLINE_AUDIO_CODING_INT, 1)
//                .setProperty(SpeechConstants.Parameter.ASR_ONLINE_VAD_ENABLE_BOOLEAN, false)
//                .setProperty(SpeechConstants.Parameter.ASR_ONLINE_VAD_LONGMODE_BOOLEAN, false)
                .setProperty(SpeechConstants.Parameter.ASR_ONLINE_ENABLE_DEBUG_LOG_BOOLEAN, true);

        //SogoSpeechSettings.shareInstance().setProperty(SpeechConstants.Parameter.TRANSLATION_ENABLED,true);
//        settings.setProperty(SpeechConstants.Parameter.ASR_ONLINE_DEBUG_SAVE_VAD_PATH, FileUtils.getSDPath() + "/SogoSpeechDebug/");
//        settings.setProperty(SpeechConstants.Parameter.ASR_ONLINE_DEBUG_SAVE_SPEEX_PATH, FileUtils.getSDPath() + "/SogoSpeechDebug/");
//        settings.setProperty(SpeechConstants.Parameter.ASR_ONLINE_DEBUG_SAVE_REQUEST_DATA_PATH, FileUtils.getSDPath() + "/SogoSpeechDebug/");

//        settings.setProperty(SpeechConstants.Parameter.WAKEUP_IS_NEEDED,false);

        settings.setProperty(SpeechConstants.Parameter.ASR_ONLINE_MODEL, SpeechConstants.ASR_MODEL_SEARCH);


    }



    private void initViews(){
        vadTimeText = findViewById(R.id.vadTimeText);
        vadEosText = findViewById(R.id.vadEosText);
        vadBosText = findViewById(R.id.vadBosText);
        longRecordTimeText = findViewById(R.id.longRecordTimeText);
        save = findViewById(R.id.save);
        save.setOnClickListener(this);

        spinnerLanguage = (Spinner) findViewById(R.id.spinner);
        vadSwitch = findViewById(R.id.switch_vad);
        wakeupSwitch = findViewById(R.id.switch_wakeup);
        continiousWakeupSwitch = findViewById(R.id.switch_continious_wakeup);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, R.layout.support_simple_spinner_dropdown_item, languageArr);
        spinnerLanguage.setAdapter(arrayAdapter);

        spinnerLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {//当改变下拉框的时候会触发
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {//改变内容的时候
//                Toast.makeText(TestSettingActivity.this, language[position], Toast.LENGTH_LONG).show();//打印所选中的东西arrayList.get(position)--position--数组中第几个是选中的
                String language = null;
                switch (position) {
                    case 0:
                        language = LanguageCode.ASRLanguageCode.CHINESE;
                        break;
                    case 1:
                        language = LanguageCode.ASRLanguageCode.ENGLISH;
                        break;
                    case 2:
                        language = LanguageCode.ASRLanguageCode.JAPANESE;
                        break;
                    case 3:
                        language = LanguageCode.ASRLanguageCode.KOREAN;
                        break;
                    default:
                        language = LanguageCode.ASRLanguageCode.CHINESE;
                        break;
                }
                SogoSpeechSettings.shareInstance().setProperty(SpeechConstants.Parameter.ASR_ONLINE_LANGUAGE_STRING, language);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {//没有改变的时候


            }

        });

        sendSpinner = (Spinner) findViewById(R.id.sendGapSpinner);
        ArrayAdapter<String> sendSpinnerAdapter = new ArrayAdapter<String>(this, R.layout.support_simple_spinner_dropdown_item, mIntervals);
        sendSpinner.setAdapter(sendSpinnerAdapter);
        int selected1 = 0;
        switch (SogoSpeechSettings.shareInstance().packageSize){
            case SpeechConstants.LENGTH_200MS_SHORT:
                selected1 = 0;
                break;
            case SpeechConstants.LENGTH_300MS_SHORT:
                selected1 = 1;
                break;
            case SpeechConstants.LENGTH_500MS_SHORT:
                selected1 = 2;
                break;

        }
        sendSpinner.setSelection(selected1);
        sendSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int size = -1;
                switch (position){
                    case 0:
                        size = SpeechConstants.LENGTH_200MS_SHORT;
                        break;
                    case 1:
                        size = SpeechConstants.LENGTH_300MS_SHORT;
                        break;
                    case 2:
                        size = SpeechConstants.LENGTH_500MS_SHORT;
                        break;
                }
                if (size >= 0) {
                    SogoSpeechSettings.shareInstance().setProperty(SpeechConstants.Parameter.ASR_ONLINE_SEND_PACK_LEN_INT, size);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        voiceEncodeSpinner = findViewById(R.id.voiceEncodeSpinner);
        ArrayAdapter<String> voiceEncodeAdapter = new ArrayAdapter<String>(this, R.layout.support_simple_spinner_dropdown_item, mVoices);
        voiceEncodeSpinner.setAdapter(voiceEncodeAdapter);
        int selected = 0;
        switch (SogoSpeechSettings.shareInstance().audioCoding){
            case 1:
                selected = 0;
                break;
            case 2:
                selected = 1;
                break;
            case 100:
                selected = 2;
                break;

        }
        voiceEncodeSpinner.setSelection(selected);
        voiceEncodeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int encode = -1;
                switch (position){
                    case 0:
                        encode = 1;
                        break;
                    case 1:
                        encode = 2;
                        break;
                    case 2:
                        encode = 100;
                        break;
                }

                if (encode > 0){
                    SogoSpeechSettings.shareInstance().setProperty(SpeechConstants.Parameter.ASR_ONLINE_AUDIO_CODING_INT, encode);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        environmentalSpinner = findViewById(R.id.environmentalSpinner);
        ArrayAdapter<String> environmentalAdapter = new ArrayAdapter<String>(this, R.layout.support_simple_spinner_dropdown_item, mEnvironment);
        environmentalSpinner.setAdapter(environmentalAdapter);
        setSpinnerItemSelectedByValue(environmentalSpinner,SogoSpeechSettings.shareInstance().model);

        environmentalSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String model = null;
                switch (position){
                    case 0:
                        model = "default";
                        break;
                    case 1:
                        model = "search";
                        break;

                }
                if (!TextUtils.isEmpty(model)){
                    SogoSpeechSettings.shareInstance().setProperty(SpeechConstants.Parameter.ASR_ONLINE_MODEL, model);

                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        boolean vad = SogoSpeechSettings.shareInstance().enableVad;
        vadSwitch.setChecked(vad);
        vadSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    vadSwitch.setSwitchTextAppearance(TestSettingActivity.this,R.style.s_true);
                }else{
                    vadSwitch.setSwitchTextAppearance(TestSettingActivity.this,R.style.s_false);
                }
                SogoSpeechSettings.shareInstance().setProperty(SpeechConstants.Parameter.ASR_ONLINE_VAD_ENABLE_BOOLEAN, isChecked);
                if (!isChecked){
                    enableLongTimeSwitch.setChecked(false);
                }
            }
        });


        boolean wakeup = SogoSpeechSettings.shareInstance().needWakeup;
        wakeupSwitch.setChecked(wakeup);
        wakeupSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    wakeupSwitch.setSwitchTextAppearance(TestSettingActivity.this,R.style.s_true);
                }else{
                    wakeupSwitch.setSwitchTextAppearance(TestSettingActivity.this,R.style.s_false);
                    continiousWakeupSwitch.setChecked(isChecked);
                }
                SogoSpeechSettings.shareInstance().setProperty(SpeechConstants.Parameter.WAKEUP_IS_NEEDED,isChecked);
                CommonSharedPreference.getInstance(TestSettingActivity.this).setBoolean(CommonSharedPreference.WAKEUP_IS_NEEDED,isChecked);
            }
        });

        boolean continiousWakeup = CommonSharedPreference.getInstance(TestSettingActivity.this).getBoolean(CommonSharedPreference.CONTINIOUS_WAKEUP,false);
        continiousWakeupSwitch.setChecked(continiousWakeup);
        if(continiousWakeup){
            wakeupSwitch.setChecked(continiousWakeup);
        }
        continiousWakeupSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    continiousWakeupSwitch.setSwitchTextAppearance(TestSettingActivity.this,R.style.s_true);
                    wakeupSwitch.setChecked(isChecked);
                }else{
                    continiousWakeupSwitch.setSwitchTextAppearance(TestSettingActivity.this,R.style.s_false);
                }
                CommonSharedPreference.getInstance(TestSettingActivity.this).setBoolean(CommonSharedPreference.CONTINIOUS_WAKEUP,isChecked);
            }
        });
        enableLongTimeSwitch = findViewById(R.id.enableLongTime);
        enableLongTimeSwitch.setChecked(SogoSpeechSettings.shareInstance().isVadLongMode);
        enableLongTimeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    enableLongTimeSwitch.setSwitchTextAppearance(TestSettingActivity.this,R.style.s_true);
                }else{
                    enableLongTimeSwitch.setSwitchTextAppearance(TestSettingActivity.this,R.style.s_false);
                }
                SogoSpeechSettings.shareInstance().setProperty(SpeechConstants.Parameter.ASR_ONLINE_VAD_LONGMODE_BOOLEAN,isChecked);
            }
        });
    }

    @Override
    protected void onResume() {

        String currentAsrlanguage = SogoSpeechSettings.shareInstance().asrlanguage;
        if (TextUtils.equals(currentAsrlanguage, LanguageCode.ASRLanguageCode.CHINESE)) {
            spinnerLanguage.setSelection(0);
        } else if (TextUtils.equals(currentAsrlanguage, LanguageCode.ASRLanguageCode.ENGLISH)) {
            spinnerLanguage.setSelection(1);
        } else if (TextUtils.equals(currentAsrlanguage, LanguageCode.ASRLanguageCode.JAPANESE)) {
            spinnerLanguage.setSelection(2);
        } else if (TextUtils.equals(currentAsrlanguage, LanguageCode.ASRLanguageCode.KOREAN)) {
            spinnerLanguage.setSelection(3);
        }

        vadEosText.setText(""+SogoSpeechSettings.shareInstance().eos);
        vadBosText.setText(""+SogoSpeechSettings.shareInstance().bos);
        vadTimeText.setText(""+SogoSpeechSettings.shareInstance().maxSpeechInterval);
        longRecordTimeText.setText(""+SogoSpeechSettings.shareInstance().maxRecordInterval);
        super.onResume();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.save){
            if (vadBosText.getText() != null){
                float vadBos = castTofloat(vadBosText.getText().toString());
                if (vadBos > 0 && vadBos < 4){
                    SogoSpeechSettings.shareInstance().setProperty(SpeechConstants.Parameter.ASR_ONLINE_VAD_BOS_FLOAT,vadBos);
                }

            }

            if (vadEosText.getText() != null){
                float vadEos = castTofloat(vadEosText.getText().toString());
                if (vadEos > 0 && vadEos < 5){
                    SogoSpeechSettings.shareInstance().setProperty(SpeechConstants.Parameter.ASR_ONLINE_VAD_EOS_FLOAT,vadEos);

                }
            }

            if (vadTimeText.getText() != null){
                int vadTime = castToInt(vadTimeText.getText().toString());
                if (vadTime > 0){
                    SogoSpeechSettings.shareInstance().setProperty(SpeechConstants.Parameter.ASR_ONLINE_VAD_MAX_INTERVAL_INT,vadTime);
                }
            }

            if (longRecordTimeText.getText() != null){
                int longRecordTime = castToInt(longRecordTimeText.getText().toString());
                if (longRecordTime > 0){
                    SogoSpeechSettings.shareInstance().setProperty(SpeechConstants.Parameter.ASR_ONLINE_VAD_MAX_AUDIO_LENGTH_INT,longRecordTime);
                }
            }
            LogUtil.v("SogoSpeech","SogoSpeechSettings "+SogoSpeechSettings.shareInstance().paramToString());
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private float castTofloat(String str){
        float result = -1f;
        try {
            result = Float.parseFloat(str);
        }catch (Exception e){}

        return result;
    }

    private int castToInt(String str){
        int result = -1;
        try {
            result = Integer.parseInt(str);
        }catch (Exception e){}

        return result;
    }

    public static void setSpinnerItemSelectedByValue(Spinner spinner,String value){
        SpinnerAdapter apsAdapter= spinner.getAdapter(); //得到SpinnerAdapter对象
        int k= apsAdapter.getCount();
        for(int i=0;i<k;i++){
            if(value.equals(apsAdapter.getItem(i).toString())){
                spinner.setSelection(i,true);// 默认选中项
                break;
            }
        }
    }





}