package com.example.aideep.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.aideep.R;
import com.example.aideep.dphelper.DeepSeekClient;
import com.hjq.toast.Toaster;
import com.iflytek.sparkchain.core.asr.ASR;
import com.iflytek.sparkchain.core.asr.AsrCallbacks;
import com.iflytek.sparkchain.core.asr.Segment;
import com.iflytek.sparkchain.core.asr.Transcription;
import com.iflytek.sparkchain.core.asr.Vad;


import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {
    //语音识别麦克风的权限请求码
    private String TAG = "MainActivityzzz";
    private int speechRecognizerRequestCode = 1000;
    //语音识别类
    private ASR mAsr = null;
    //语音识别回调
    private AsrCallbacks mAsrCallbacks = null;
    //语音识别使用
    private int count = 0;

    //语音功能正在运行标志位
    private boolean isrun = false;
    private boolean isdws = false;
    //语音识别语言
    private String language = "zh_cn";
    //展示缓存
    private String cacheInfo = "";

    private TextView tv_speech;
    private Button btn_audio_start;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DeepSeekClient.getInstance(this).requestDSApi(new DeepSeekClient.DeepSeekClientCallback() {
            @Override
            public void onSuccess(String str) {
                Toaster.show(str);
            }

            @Override
            public void onFail(String str) {
                Toaster.show(str);
            }
        });
        initView();
        initASR();
    }

    //初始化view
    private void initView() {
        tv_speech = findViewById(R.id.tv_speech);
        btn_audio_start = findViewById(R.id.ai_asr_audio_start_btn);
        String[] perms = {Manifest.permission.RECORD_AUDIO};
        btn_audio_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (EasyPermissions.hasPermissions(MainActivity.this, perms)) {
                    // 已经拥有权限，进行操作
                    runAsr_Audio();
                } else {
                    // 未获得权限，现在请求它们
                    EasyPermissions.requestPermissions(MainActivity.this, "为了语音识别功能的正常进行，需获取麦克风权限", speechRecognizerRequestCode, perms);
                }
            }
        });
    }

    //初始化语音识别SDK
    private void initASR() {
        mAsrCallbacks = new AsrCallbacks() {
            @Override
            public void onResult(ASR.ASRResult asrResult, Object o) {
                //以下信息需要开发者根据自身需求，如无必要，可不需要解析执行。
                int begin = asrResult.getBegin();         //识别结果所处音频的起始点
                int end = asrResult.getEnd();           //识别结果所处音频的结束点
                int status = asrResult.getStatus();        //结果数据状态，0：识别的第一块结果,1：识别中间结果,2：识别最后一块结果
                String result = asrResult.getBestMatchText(); //识别结果
                String sid = asrResult.getSid();           //sid

                List<Vad> vads = asrResult.getVads();
                List<Transcription> transcriptions = asrResult.getTranscriptions();
                int vad_begin = -1;
                int vad_end = -1;
                String word = null;
                for (Vad vad : vads) {
                    vad_begin = vad.getBegin();
                    vad_end = vad.getEnd();                   //VAD结果
                    Log.d(TAG, "vad={begin:" + vad_begin + ",end:" + vad_end + "}");
                }
                for (Transcription transcription : transcriptions) {
                    List<Segment> segments = transcription.getSegments();
                    for (Segment segment : segments) {
                        word = segment.getText();              //分词结果
//                    Log.d(TAG,"word={word:"+word+"}");
                    }
                }
                String info = "result={begin:" + begin + ",end:" + end + ",status:" + status + ",result:" + result + ",sid:" + sid + "}";
                Log.d(TAG, info);
                /****************************此段为为了UI展示结果，开发者可根据自己需求改动*****************************************/
                if (status == 0) {
                    //开始
                    if (isdws) {
                        cacheInfo = tv_speech.getText().toString(); //获取信息记录
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tv_speech.setText(cacheInfo + "识别结果：" + result);
                            }
                        });
                    }
                } else if (status == 2) {
                    //结束
                    if (isdws) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tv_speech.setText(cacheInfo + "识别结果：" + result + "");
                            }
                        });
                    } else {
                        Toaster.show(result + "");
                    }
                    stopAsr();
                } else {
                    //中间
                    if (isdws) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tv_speech.setText(cacheInfo + "识别结果：" + result);
                            }
                        });
                    }
                }
                toend();
                /*********************************************************************/
            }

            @Override
            public void onError(ASR.ASRError asrError, Object o) {
                int code = asrError.getCode();
                String msg = asrError.getErrMsg();
                String sid = asrError.getSid();
                String info = "error={code:" + code + ",msg:" + msg + ",sid:" + sid + "}";
                Log.d(TAG, info);
                Toaster.show("识别出错!错误码：" + code + ",错误信息：" + msg + ",sid:" + sid + "");

                stopAsr();
            }

            @Override
            public void onBeginOfSpeech() {

            }

            @Override
            public void onEndOfSpeech() {

            }
        };
        if (mAsr == null) {
            mAsr = new ASR();
            mAsr.registerCallbacks(mAsrCallbacks);
        }
    }

    //开始麦克风语音识别
    private void runAsr_Audio() {
        if (isrun) {
            Toaster.show("正在识别中，请勿重复开启。");
            return;
        }
        if (mAsr == null) {
            initASR();
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toaster.show("语音开始识别！！！");
                btn_audio_start.setText(R.string.asring_button);
                btn_audio_start.setEnabled(false);
            }
        });
        isdws = false;
        mAsr.language(language);//语种，zh_cn:中文，en_us:英文。其他语种参见集成文档
        mAsr.domain("iat");//应用领域,iat:日常用语。其他领域参见集成文档
        mAsr.accent("mandarin");//方言，mandarin:普通话。方言仅当language为中文时才会生效。其他方言参见集成文档。
        mAsr.vinfo(true);//返回子句结果对应的起始和结束的端点帧偏移值。
        if ("zh_cn".equals(language)) {
            mAsr.dwa("wpgs");//动态修正
            isdws = true;
        }

        count++;
        //入参为用户自定义标识，用户关联onResult结果。
        int ret = mAsr.startListener(count + "");
        if (ret != 0) {
            Toaster.show("识别开启失败，错误码:" + ret + "");
        } else {
            isrun = true;
        }
    }

    //停止语音识别
    private void stopAsr() {
        if (isrun) {
            Toaster.show("语音停止识别！！！");
//            if ("AUDIO".equals(startMode)) {
            if (mAsr != null) {
                mAsr.stopListener(false);
                Toaster.show("已停止录音。");
            }
//            } else {
//                if (mAsr != null) {
//                    mAsr.stop(true);//取消。true:立即结束，false:等大模型返回最后一帧数据后结束
//                    showInfo("\n已停止识别。\n");
//                }
//            }
//            startMode = "NONE";
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    btn_audio_start.setText(R.string.asr_button);
                    btn_audio_start.setEnabled(true);
//                    btn_file_start.setEnabled(true);
                }
            });
            isrun = false;
        } else {
            Toaster.show("已停止识别。");
        }
    }

    //显示控件自动下移
    public void toend() {
//        int scrollAmount = tv_result.getLayout().getLineTop(tv_result.getLineCount()) - tv_result.getHeight();
//        if (scrollAmount > 0) {
//            tv_result.scrollTo(0, scrollAmount+10);
//        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // 将结果转发给 EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        if (requestCode == speechRecognizerRequestCode) {
            runAsr_Audio();
        }
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        if (requestCode == speechRecognizerRequestCode) {
            Toaster.show("为了语音识别功能的正常进行，需获取麦克风权限，请前往设置中打开。");
        }
    }
}
