// Copyright 2018 Sogou Inc. All rights reserved. 
// Use of this source code is governed by the Apache 2.0 
// license that can be found in the LICENSE file. 
package com.sogou.demo.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.sogou.audiosource.AudioRecordDataProviderFactory;
import com.sogou.audiosource.DefaultAudioSource;
import com.sogou.audiosource.IAudioDataProviderFactory;
import com.sogou.audiosource.IAudioSource;
import com.sogou.audiosource.IAudioSourceListener;
import com.sogou.common.activity.BaseActivity;
import com.sogou.sogocommon.ErrorIndex;
import com.sogou.sogocommon.utils.CommonSharedPreference;
import com.sogou.sogocommon.utils.CommonUtils;
import com.sogou.sogocommon.utils.FileUtils;
import com.sogou.sogocommon.utils.LogUtil;
import com.sogou.sogocommon.utils.RingBufferFlip;
import com.sogou.sogocommon.utils.SogoConstants;
import com.sogou.sogouspeech.EventListener;
import com.sogou.sogouspeech.SogoSpeech;
import com.sogou.sogouspeech.SogoSpeechSettings;
import com.sogou.sogouspeech.auth.TokenFetchTask;
import com.sogou.sogouspeech.paramconstants.SpeechConstants;
import com.sogou.speech.speechsdk.R;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * 说明：正式使用的过程中，建议关闭中间音频保存，也就是不要设置ASR_ONLINE_DEBUG_SAVE_VAD_PATH，ASR_ONLINE_DEBUG_SAVE_SPEEX_PATH，ASR_ONLINE_DEBUG_SAVE_REQUEST_DATA_PATH这三个变量，
 * 会影响识别速度和内存使用
 */
public class MainActivity extends BaseActivity implements View.OnClickListener, IAudioSourceListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private Button initButton, startButton, stopButton, releaseButton;

    private SogoSpeech sogoSpeech;
    private String token;

    private DefaultAudioSource mAudioSource;
    private Thread mAudioSourceThread = null;
    private long timeStampAtRecordStart;
    private RingBufferFlip resizeDataCache = null;

    private static final int REQUEST_EXTERNAL_STORAGE = 1;

    private static String[] PERMISSIONS_REQUEST = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_PHONE_STATE//如果不需要获取IMEI之类的参数可以去掉
    };

    private long timeAtButtonDown = 0;
    private long timeAtReciveRecordData = 0;
    private long timeAtSpeechStart = 0;
    private long timeAtFirstResult = 0;
    private long timeAtSpeechStop = 0;
    private long timeAtLastResult = 0;
    private long timeAtRecordStop = 0;

    private String resultBuffer = null;

    private TextView text = null;
    private TextView textMessage = null;
    private TextView settingEntrance;
    public static final int UPDATE_RESULT = 1;
    public static final int UPDATE_PART_RESULT = 2;
    public static final int UPDATE_MESSAGE = 3;
    public static final int WRITE_STRING_TO_FILE = 4;

    //标志是否已唤醒
    private boolean hasWakeup = false;

    //标志是否需要连续唤醒
    private boolean continiousWakup = false;
    private boolean needWakup = false;

    //todo： protential memory leaks
    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            String str = "";
            switch (msg.what) {
                case UPDATE_PART_RESULT:
                    if (resultBuffer == null) {
                        str = (String) msg.obj + "\n中间结果:";
                    } else {
                        str = resultBuffer + (String) msg.obj + "\n中间结果:";
                    }
                    text.setText(str);
                    break;
                case UPDATE_RESULT:
                    if (resultBuffer == null) {
                        resultBuffer = (String) msg.obj;
                    } else {
                        resultBuffer = resultBuffer + (String) msg.obj;
                    }

                    if (SogoSpeechSettings.shareInstance().enableVad) {
                        str = resultBuffer + "\n" + "断句:\n" +
                                "timeAtReciveRecordData - timeAtButtonDown = " + (timeAtReciveRecordData - timeAtButtonDown) + "\n" +
                                "timeAtSpeechStart - timeAtReciveRecordData = " + (timeAtSpeechStart - timeAtReciveRecordData) + "\n" +
                                "timeAtFirstResult - timeAtSpeechStart = " + (timeAtFirstResult - timeAtSpeechStart) + "\n" +
                                "timeAtLastResult - timeAtSpeechStop = " + (timeAtLastResult - timeAtSpeechStop) + "\n";
                    } else {
                        str = resultBuffer + "\n" + "断句:\n" +
                                "timeAtReciveRecordData - timeAtButtonDown = " + (timeAtReciveRecordData - timeAtButtonDown) + "\n" +
                                "timeAtFirstResult - timeAtReciveRecordData = " + (timeAtFirstResult - timeAtReciveRecordData) + "\n";
                    }
                    text.setText(str);
                    break;
                case UPDATE_MESSAGE:
                    str = (String) msg.obj;
//                    String stringBefore = textMessage.getText().toString();
//                    textMessage.setText(stringBefore + "\n" + str);
                    textMessage.setMovementMethod(new ScrollingMovementMethod());
                    textMessage.append(str + "\n");
                    break;
                case WRITE_STRING_TO_FILE:
                    str = sogoSpeech.mTimeStamp + ".pcm\n" + resultBuffer + "\n";
                    storeResultToFile(FileUtils.getSDPath() + "/SogoSpeechDebug/", "result.txt", str);
                    textMessage.setMovementMethod(new ScrollingMovementMethod());
                    textMessage.append("识别结果已写入文件\n");
                    break;
                default:
                    break;
            }
            return true;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        CommonSharedPreference.getInstance(MainActivity.this).setBoolean(CommonSharedPreference.CONTINIOUS_WAKEUP, false);
        CommonSharedPreference.getInstance(MainActivity.this).setBoolean(CommonSharedPreference.WAKEUP_IS_NEEDED, false);
        setTitleBar("主页");
        setImmerseLayout(findViewById(R.id.main_commonbar));

        verifyStoragePermissions(MainActivity.this);
        initViews();
        final IAudioDataProviderFactory factory = new AudioRecordDataProviderFactory(this);
        mAudioSource = new DefaultAudioSource(factory);
    }

    @Override
    protected void onResume() {
        super.onResume();
        continiousWakup = CommonSharedPreference.getInstance(MainActivity.this).getBoolean(CommonSharedPreference.CONTINIOUS_WAKEUP, false);
        needWakup = CommonSharedPreference.getInstance(MainActivity.this).getBoolean(CommonSharedPreference.WAKEUP_IS_NEEDED, false);
    }

    public static void verifyStoragePermissions(Activity activity) {
//        int permission = ActivityCompat.checkSelfPermission(activity,
//                Manifest.permission.WRITE_EXTERNAL_STORAGE);
//        int audioPermission = ActivityCompat.checkSelfPermission(activity,
//                Manifest.permission.RECORD_AUDIO);
//
//        if (permission != PackageManager.PERMISSION_GRANTED || audioPermission != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(activity, PERMISSIONS_REQUEST,
//                    REQUEST_EXTERNAL_STORAGE);
//        }

    }

    /**
     * 初始化引擎，必须保证SpeechConstants.Parameter.ASR_ONLINE_AUTH_TOKEN_STRING，SpeechConstants.Parameter.APPID，SpeechConstants.Parameter.UUID
     * 三个参数设置了正确的值
     * @param token
     */
    private void initEngine(String token) {

        sogoSpeech = new SogoSpeech(MainActivity.this);
        SogoSpeechSettings.shareInstance().setProperty(SpeechConstants.Parameter.ASR_ONLINE_AUTH_TOKEN_STRING, token)
                .setProperty(SpeechConstants.Parameter.APPID, CommonUtils.getApplicationMetaData(MainActivity.this, SogoConstants.APPID))
                .setProperty(SpeechConstants.Parameter.UUID, android.os.Build.SERIAL);

        sogoSpeech.registerListener(mEventListener);
        sogoSpeech.send(SpeechConstants.Command.ASR_ONLINE_CREATE, null, null, 0, 0);
    }


    private EventListener mEventListener = new EventListener() {
        @Override
        public void onEvent(String eventName, String param, byte[] data, int codeOrOffset, int length) {
            LogUtil.d("xq", "@onEvent eventName:" + eventName + " param:" + param);

            //检测到语音结束，需要停止录音，此时识别引擎不再接收数据，传入将报错。
            if (TextUtils.equals(eventName, SpeechConstants.Message.MSG_ASR_ONLINE_SPEECH_END)) {
                if (timeAtSpeechStop == 0) {
                    timeAtSpeechStop = System.currentTimeMillis();
                }
                //由于在之前唤醒成功的时候，会将SpeechConstants.Parameter.WAKEUP_IS_NEEDED设置为false，所以这里重置一下
                if(needWakup){
                    hasWakeup = false;
                    SogoSpeechSettings.shareInstance().setProperty(SpeechConstants.Parameter.WAKEUP_IS_NEEDED, true);
                }
                /**
                 * 连续唤醒+识别（也就是打开vad，vad来进行判断语音结束），需要调用resetWakeup。并且要保持音频的持续输入，如果不用唤醒的话，注释掉resetWakeup
                 */
                if (continiousWakup) {
                    resetWakeup();
                }
                if (!continiousWakup) {
                    stopRecordMic();
                }


                Message message = new Message();
                message.what = UPDATE_MESSAGE;
                message.obj = "检测到说话结束";
                handler.sendMessage(message);
            }
            //检测到有效声音
            else if (TextUtils.equals(eventName, SpeechConstants.Message.MSG_ASR_ONLINE_SPEECH_START)) {
                if (timeAtSpeechStart == 0) {
                    timeAtSpeechStart = System.currentTimeMillis();
                }
                Message message = new Message();
                message.what = UPDATE_MESSAGE;
                message.obj = "检测到说话开始";
                handler.sendMessage(message);
            }
            //处理中间结果
            else if (TextUtils.equals(eventName, SpeechConstants.Message.MSG_ASR_ONLINE_PART_RESULT)) {
                if (timeAtFirstResult == 0) {
                    timeAtFirstResult = System.currentTimeMillis();
                }
                Message message = new Message();
                message.what = UPDATE_PART_RESULT;
                message.obj = param;
                handler.sendMessage(message);
            }
            //处理断句
            else if (TextUtils.equals(eventName, SpeechConstants.Message.MSG_ASR_ONLINE_LAST_RESULT)) {
                if (timeAtLastResult == 0) {
                    timeAtLastResult = System.currentTimeMillis();
                }
                Message message = new Message();
                message.what = UPDATE_RESULT;
                message.obj = param;
                handler.sendMessage(message);
            } else if (TextUtils.equals(eventName, SpeechConstants.Message.MSG_ASR_ONLINE_READY)) {
                Message message = new Message();
                message.what = UPDATE_MESSAGE;
                message.obj = "ASR就绪";
                handler.sendMessage(message);
            } else if (TextUtils.equals(eventName, SpeechConstants.Message.MSG_ASR_ONLINE_WORKING)) {
                Message message = new Message();
                message.what = UPDATE_MESSAGE;
                message.obj = "ASR识别中";
                handler.sendMessage(message);
            } else if (TextUtils.equals(eventName, SpeechConstants.Message.MSG_ASR_ONLINE_COMPLETED)) {
                Message message = new Message();
                message.what = UPDATE_MESSAGE;
                message.obj = "ASR完成";
                handler.sendMessage(message);

                Message message1 = new Message();
                message1.what = WRITE_STRING_TO_FILE;
                handler.sendMessage(message1);
            } else if (TextUtils.equals(eventName, SpeechConstants.Message.MSG_ASR_ONLINE_TERMINATION)) {
                Message message = new Message();
                message.what = UPDATE_MESSAGE;
                message.obj = "ASR终止";
                handler.sendMessage(message);
            } else if (TextUtils.equals(eventName, SpeechConstants.Message.MSG_WAKEUP_SUCC)) {
                Message message = new Message();
                message.what = UPDATE_MESSAGE;
                message.obj = "唤醒成功，唤醒词：" + param;
                handler.sendMessage(message);
                hasWakeup = true;
            } else if (TextUtils.equals(eventName, SpeechConstants.Message.MSG_WAKEUP_INIT_SUCC)) {
                Message message = new Message();
                message.what = UPDATE_MESSAGE;
                message.obj = "唤醒初始化成功";
                handler.sendMessage(message);
            }
        }

        @Override
        public void onError(String errorDomain, int errorCode, String errorDescription, Object extra) {
            LogUtil.v("xq", "@onError " + errorDomain + " " + errorCode + " " + errorDescription);
            if (TextUtils.equals(errorDomain, SpeechConstants.ErrorDomain.ERR_ASR_ONLINE_PREPROCESS)) {
                //没有检测到有效声音，需要停止录音，此时引擎中的vad将不再接收数据，需要重新初始化。
                if (errorCode == ErrorIndex.ERROR_VAD_SPEECH_TIMEOUT) {

                }
                //VAD传入的音频长度超过了规定长度。
                else if (errorCode == ErrorIndex.ERROR_VAD_AUDIO_INPUT_TOO_LONG) {

                }
                //有效语音长度超过了规定的长度。
                else if (errorCode == ErrorIndex.ERROR_VAD_SPEECH_TOO_LONG) {

                }
            } else if (TextUtils.equals(errorDomain, SpeechConstants.ErrorDomain.ERR_ASR_ONLINE_SERVER)) {
                //服务端返回错误

            } else if (TextUtils.equals(errorDomain, SpeechConstants.ErrorDomain.ERR_ASR_ONLINE_NETWORK)) {
                //网络出错

            } else if (TextUtils.equals(errorDomain, SpeechConstants.ErrorDomain.ERR_WAKEUP_NOT_INIT)) {
                //离线唤醒没有初始化
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this,"请重新初始化！",Toast.LENGTH_SHORT).show();
                    }
                });
            }

            if(needWakup){
                hasWakeup = false;
                SogoSpeechSettings.shareInstance().setProperty(SpeechConstants.Parameter.WAKEUP_IS_NEEDED, true);
            }

            //如果连续唤醒的话，
            if (continiousWakup) {
                resetWakeup();
            }

            if (errorCode != ErrorIndex.ERROR_VAD_SPEECH_TIMEOUT || !continiousWakup) {
                stopRecordMic();
            } else {
                sogoSpeech.send(SpeechConstants.Command.ASR_ONLINE_CREATE, null, null, 0, 0);
                resetWakeup();
            }

            Message message = new Message();
            message.what = UPDATE_RESULT;
            message.obj = errorDomain + ":" + errorDescription;
            handler.sendMessage(message);
        }
    };

    private void initViews() {
        initButton = (Button) findViewById(R.id.initButton);
        startButton = (Button) findViewById(R.id.startButton);
        stopButton = (Button) findViewById(R.id.stopButton);
        releaseButton = (Button) findViewById(R.id.releaseButton);
        settingEntrance = (TextView) findViewById(R.id.tv_bar_right);
        settingEntrance.setText("设置");

        initButton.setOnClickListener(this);
        startButton.setOnClickListener(this);
        stopButton.setOnClickListener(this);
        releaseButton.setOnClickListener(this);
        settingEntrance.setOnClickListener(this);

        text = (TextView) findViewById(R.id.textView_result);
        textMessage = (TextView) findViewById(R.id.textView_message);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.initButton:
                //每次发送在线请求之前
                if(sogoSpeech != null){
                    sogoSpeech.send(SpeechConstants.Command.ASR_ONLINE_DESTROY,"",null,0,0);
                }
                long expTime = CommonSharedPreference.getInstance(MainActivity.this).getLong(CommonSharedPreference.TIMEOUT_STAMP, 0L);//获取token过期时间戳，单位是秒
                long timeGap = (expTime - 60 * 30) * 1000;//如果当前时间在过期前半小时范围内的话，那么刷新token
                if (timeGap - System.currentTimeMillis() < 0) {
                    fetchToken();
                } else {
                    token = CommonSharedPreference.getInstance(MainActivity.this).getString(CommonSharedPreference.TOKEN, "");
                    initEngine(token);
                }
                break;
            case R.id.startButton:
                timeAtButtonDown = 0;
                timeAtReciveRecordData = 0;
                timeAtSpeechStart = 0;
                timeAtFirstResult = 0;
                timeAtSpeechStop = 0;
                timeAtLastResult = 0;
                timeAtRecordStop = 0;
                if(resizeDataCache == null){
                    resizeDataCache = new RingBufferFlip(16000);
                }
                resizeDataCache.reset();


                if (TextUtils.isEmpty(token) || sogoSpeech == null) {
                    Toast.makeText(MainActivity.this, "请初始化！", Toast.LENGTH_SHORT).show();
                    return;
                }

                resultBuffer = null;
                text.setText("");

                timeAtButtonDown = System.currentTimeMillis();
                if (SogoSpeechSettings.shareInstance().needWakeup &&  CommonSharedPreference.getInstance(MainActivity.this).getBoolean(CommonSharedPreference.CONTINIOUS_WAKEUP,false)) {
                    resetWakeup();
                }

                hasWakeup = false;

                if (mAudioSourceThread == null) {
                    mAudioSource.addAudioSourceListener(this);
                    mAudioSourceThread = new Thread(mAudioSource, "audioRecordSource");
                    mAudioSourceThread.start();
                }

//                readWavFromFile(1);
                break;
            case R.id.stopButton:
                stopRecordMic();
                break;
            case R.id.releaseButton:
                if (sogoSpeech != null) {
                    sogoSpeech.send(SpeechConstants.Command.ASR_ONLINE_DESTROY, null, null, 0, 0);
                }
                break;
            case R.id.tv_bar_right:
                stopRecordMic();
                startActivity(new Intent(this, TestSettingActivity.class));
                break;
            default:
                break;
        }
    }

    private void fetchToken() {
        TokenFetchTask task = new TokenFetchTask(MainActivity.this, new TokenFetchTask.TokenFetchListener() {
            @Override
            public void onTokenFetchSucc(String result) {
                LogUtil.d("xq", "onTokenFetchSucc result " + result);
                if (TextUtils.isEmpty(result)) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "初始化失败", Toast.LENGTH_SHORT).show();
                        }
                    });
                    return;
                }
                token = result;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        initEngine(token);
                    }
                });
            }

            @Override
            public void onTokenFetchFailed(String errMsg) {
                LogUtil.d("xq", "onTokenFetchFailed errMsg " + errMsg);
            }
        });
        task.execute(null);
    }


    /**
     * IAudioSourceListener
     * ------------------------------------
     */
    @Override
    public void onBegin(IAudioSource audioSource) {
        LogUtil.i("audioSourceManager", "@onBegin 录音开始");

        if (SogoSpeechSettings.shareInstance().needWakeup) {
            /**
             * 注意！这里识别（ASR_ONLINE_START）和唤醒（WAKEUP_START）的指令不一样！其余地方指令都一样
             */
            sogoSpeech.send(SpeechConstants.Command.WAKEUP_START, "", null, 0, 0);
        } else {
            sogoSpeech.send(SpeechConstants.Command.ASR_ONLINE_START, "", null, 0, 0);
        }

        timeStampAtRecordStart = System.currentTimeMillis();
    }

    @Override
    public void onNewData(IAudioSource audioSource, Object dataArray, long packIndex, long sampleIndex, int flag) {
        final short[] data = (short[]) dataArray;

        if (timeAtReciveRecordData == 0) {
            timeAtReciveRecordData = System.currentTimeMillis();
        }
        LogUtil.v("xq", "@onNewData haswakeup " + hasWakeup);
        LogUtil.i("audioSourceManager", "@onNewData #audioSourceManager onReceived sn: " + packIndex + "  data length: " + data.length + " flag" + flag);

        if(data.length != 2048 && SogoSpeechSettings.shareInstance().enableVad){
            //由于vad要求输入的音频数据，必须是长度为2048的short数组，所以如果需要过vad，且拿到的音频长度不是2048的时候，需要过一下整形的过程，保证输出的音频长度是short[2048]
            resizeShortAudioData(data,data.length,(int)packIndex,false);
        }else {
            dealRawVoiceData(data, packIndex, sampleIndex, flag);
        }
    }

    @Override
    public void onEnd(IAudioSource audioSource, int status, Exception e, long sampleCount) {
        LogUtil.i("audioSourceManager", "@onEnd 录音结束");
        if (timeAtRecordStop == 0) {
            timeAtRecordStop = System.currentTimeMillis();
        }

        /**
         * 以下逻辑写在这里是为了保证拿到的音频数据都能发送到服务器去
         */
        if (sogoSpeech != null) {
            sogoSpeech.send(SpeechConstants.Command.ASR_ONLINE_STOP, null, null, 0, 0);
        }
        mAudioSource.removeAudioSourceListener(this);
        mAudioSourceThread = null;
    }

    private void resetWakeup() {
        if (sogoSpeech != null) {
            LogUtil.e("xq", "@resetWakeup");
            hasWakeup = false;
            SogoSpeechSettings.shareInstance().setProperty(SpeechConstants.Parameter.WAKEUP_IS_NEEDED, true);
            sogoSpeech.send(SpeechConstants.Command.WAKEUP_START, "", null, 0, 0);
        }
    }

    private void stopRecordMic(){
        if (mAudioSourceThread != null && mAudioSource != null) {
            mAudioSource.stop();
//            mAudioSource.removeAudioSourceListener(this);
//            mAudioSourceThread = null;
        }
    }

    private void dealRawVoiceData(short[] data, long packIndex, long sampleIndex, int flag){
        if (sogoSpeech != null) {
        if (hasWakeup) {//如果已经唤醒，则在这里转为在线识别
            SogoSpeechSettings.shareInstance().setProperty(SpeechConstants.Parameter.WAKEUP_IS_NEEDED, false);
            if(packIndex==1) {
                sogoSpeech.send(SpeechConstants.Command.ASR_ONLINE_START, "", null, 0, 0);
            }
        }

            sogoSpeech.send(SpeechConstants.Command.ASR_ONLINE_RECOGIZE, "", data, flag==1?-Math.abs((int)packIndex):(int) packIndex, 0);
//            byte[] temp = ShortByteUtil.shortArray2ByteArray(data);
//            FileUtils.writeByteArray2SDCard(FileUtils.getSDPath() + "/SogoSpeechDebug/", timeStampAtRecordStart + "_ori.pcm", temp, true);
//            temp = null;
//            data = null;
        }
    }

    /**
     * ------------------------------------
     * IAudioSourceListener
     */


    private void storeResultToFile(String filePath, String fileName, String result) {
//        FileUtils.writeByteArray2SDCard(mSettings.vadDataPath, mTimeStamp + "_vad.pcm" , ShortByteUtil.shortArray2ByteArray(voice),true);
        FileUtils.addString2SDCard(FileUtils.getSDPath() + "/SogoSpeechDebug/", "result.txt", result);
    }

    //todo 需要放到其他线程里测试
    private void readWavFromFile(int flag) {
        // use external audio
        InputStream inputStream = null;
        onBegin(null);
        try {
            inputStream = getAssets().open("open_alipay_twice.pcm");
            LogUtil.e("xq", "inputStream!=null " + (inputStream != null));
//            inputStream = new FileInputStream(wavFile);
            byte[] byteData = new byte[6400];
            int readLen = -1;
            int packageNum = 0;
//            inputStream.skip(44);
            while ((readLen = inputStream.read(byteData)) != -1) {
                byte[] data = new byte[readLen];
                System.arraycopy(byteData, 0, data, 0, readLen);
                short[] shortData = new short[data.length / 2];
                ByteBuffer.wrap(data).order(ByteOrder.nativeOrder()).asShortBuffer().get(shortData);
                packageNum++;
                LogUtil.e("VADERROR", "shortData LENTH" + shortData.length);
                if (shortData.length < 3200) {
//                    sogouAsrTTSEngine.feedAudioData(-packageNum, shortData);
                    onNewData(null, shortData, -packageNum, 0, 1);
                } else {
//                    sogouAsrTTSEngine.feedAudioData(packageNum, shortData);
                    onNewData(null, shortData, packageNum, 0, 1);
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
//            if ((readLen = inputStream.read(byteData)) == -1) {
////                LogUtil.log("xq", "eof");
//                short[] endData = new short[0];
//                sogouAsrTTSEngine.feedAudioData(-(packageNum++), endData);
//            }
            onEnd(null, 1, null, 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void resizeShortAudioData(short[] data, int length, int packageID, boolean needresize){
        LogUtil.d(TAG,"data to resizeShortAudioData size is "+data.length + " packageid = " + packageID + "  package size = " + 2048);
        if (null != data) {
            resizeDataCache.put(data, length);
        }
        boolean isLastPackage = (packageID<0);

        do {
            final int curLen = resizeDataCache.available();
            if (curLen < 2048 && !isLastPackage) {
                return;
            }
            short[] part = new short[Math.min(curLen, 2048)];
            resizeDataCache.take(part,  part.length);

            if (resizeDataCache.available() == 0 && isLastPackage) {
                packageID = -Math.abs(packageID);
            }else{
                packageID = Math.abs(packageID);
            }

            try {
                LogUtil.v(TAG,"bytes to recognize size is "+part.length +"  part size is "+part.length+"  package id is "+packageID);
//                onEvent(SpeechConstants.Message.MSG_ASR_ONLINE_AUDIO_DATA,"", ShortByteUtil.shortArray2ByteArray(part),-1,part.length*2);
//                if(mSettings.audioCoding == RecognitionConfig.AudioEncoding.LINEAR16_VALUE) {
//                    LogUtil.d("OnlineRecognizer", "recognizer.feedShortData ");
//                    if (mSettings.requestDataPath != null){
//                        FileUtils.writeByteArray2SDCard(mSettings.vadDataPath, mTimeStamp + "_data.pcm" , ShortByteUtil.shortArray2ByteArray(part),true);
//                    }
//                    mRecognizer.feedShortData(packageID,part);
//                }else if(mSettings.audioCoding == RecognitionConfig.AudioEncoding.SOGOU_SPEEX_VALUE){
//                    LogUtil.d("OnlineRecognizer", "recognizer.speex.feedShortData ");
//                    byte[] bytes = mSpeexEncoder.encode(part);
//                    if (mSettings.speexDataPath != null){
//                        FileUtils.writeByteArray2SDCard(mSettings.vadDataPath, mTimeStamp + ".spx" , bytes,true);
//                    }
//                    mRecognizer.feedShortData(packageID,ShortByteUtil.byteArray2ShortArray(bytes));
//                }else{
//                    LogUtil.e(TAG,"unsupported audio format");
//                }
                dealRawVoiceData(part, packageID, 16000, 0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } while (resizeDataCache.available() > 0);

        if(packageID<0){
            resizeDataCache.reset();
        }
    }

}