package com.example.aideep.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.aideep.R;
import com.example.aideep.dphelper.DeepSeekClient;
import com.example.aideep.tts.TTSParams;
import com.hjq.toast.Toaster;
import com.iflytek.sparkchain.core.asr.ASR;
import com.iflytek.sparkchain.core.asr.AsrCallbacks;
import com.iflytek.sparkchain.core.asr.Segment;
import com.iflytek.sparkchain.core.asr.Transcription;
import com.iflytek.sparkchain.core.asr.Vad;
import com.iflytek.sparkchain.core.tts.OnlineTTS;
import com.iflytek.sparkchain.core.tts.TTS;
import com.iflytek.sparkchain.core.tts.TTSCallbacks;


import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {
    //语音识别：
    //麦克风的权限请求码
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
    //语音播报：
    private TTSCallbacks mTTSCallback = null;
    private int sampleRate = 16000;//合成音频的采样率，支持8K 16K音频，具体参见集成文档
    private TTSParams mTTSParams = new TTSParams();
    private OnlineTTS mOnlineTTS;
    private Button ai_asr_audio_start_btn;
    /**
     * 播放器，用于播报合成的音频。
     * 注意：当前Demo中的播放器仅实现了播放PCM格式的音频，如果客户合成的是其他格式的音频，需自行实现播放功能。
     */
    private static final int AUDIOPLAYER_INIT = 0x0000;
    private static final int AUDIOPLAYER_START = 0x0001;
    private static final int AUDIOPLAYER_WRITE = 0x0002;
    private static final int AUDIOPLAYER_END = 0x0003;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO; // 单声道输出
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT; // PCM 16位编码
    private AudioTrack audioTrack;
    private Handler mAudioPlayHandler;
    private boolean isPlaying = false;
    private int ttsCount = 0;
    private Thread mAudioPlayThread = null;
    private Button ai_tts_audio_start_btn;


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
        initTTS();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("WXW", "onDestory");
        if (isPlaying) {
            isPlaying = false;
            mAudioPlayHandler.removeCallbacksAndMessages(null);
            mAudioPlayHandler.sendEmptyMessage(AUDIOPLAYER_END);
        }
    }

    //初始化view
    private void initView() {
        tv_speech = findViewById(R.id.tv_speech);
        ai_asr_audio_start_btn = findViewById(R.id.ai_asr_audio_start_btn);
        ai_tts_audio_start_btn = findViewById(R.id.ai_tts_audio_start_btn);
        String[] perms = {Manifest.permission.RECORD_AUDIO};
        ai_asr_audio_start_btn.setOnClickListener(new View.OnClickListener() {
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
        ai_tts_audio_start_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSpeak();
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

    //初始化语音播报
    private void initTTS() {
        mTTSCallback = new TTSCallbacks() {
            @Override
            public void onResult(TTS.TTSResult result, Object o) {
                //解析获取的交互结果，示例展示所有结果获取，开发者可根据自身需要，选择获取。
                byte[] audio = result.getData();//音频数据
                int len = result.getLen();//音频数据长度
                int status = result.getStatus();//数据状态
                String ced = result.getCed();//进度
                String sid = result.getSid();//sid

                Bundle bundle = new Bundle();
                bundle.putByteArray("audio", audio);
                Message msg = mAudioPlayHandler.obtainMessage();
                msg.what = AUDIOPLAYER_WRITE;
                msg.obj = bundle;
                mAudioPlayHandler.sendMessage(msg);

                if (status == 2) {
                    //音频合成回调结束状态，注意，此状态不是播报完成状态
                    mAudioPlayHandler.sendEmptyMessage(AUDIOPLAYER_END);
                }

            }


            @Override
            public void onError(TTS.TTSError ttsError, Object o) {
                int errCode = ttsError.getCode();//错误码
                String errMsg = ttsError.getErrMsg();//错误信息
                String sid = ttsError.getSid();//sid
                String msg = "合成出错！code:" + errCode + ",msg:" + errMsg + ",sid:" + sid;
                Log.d(TAG, "onError:errCode:" + errCode + ",errMsg:" + errMsg);
                Toaster.show(msg);
//                showInfo(msg);
                if (isPlaying) {
                    //如果此时已经播报，则停止播报
                    stopSpeak();
                }
            }
        };
        mAudioPlayThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                mAudioPlayHandler = new Handler(Looper.myLooper()) {

                    @Override
                    public void handleMessage(@NonNull Message msg) {
                        super.handleMessage(msg);
                        switch (msg.what) {
                            case AUDIOPLAYER_INIT:
                                Log.d(TAG, "audioInit");
                                int minBufferSize = AudioTrack.getMinBufferSize(sampleRate, CHANNEL_CONFIG, AUDIO_FORMAT);
                                audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, CHANNEL_CONFIG, AUDIO_FORMAT, minBufferSize, AudioTrack.MODE_STREAM);
                                mAudioPlayHandler.sendEmptyMessage(AUDIOPLAYER_START);
                                break;
                            case AUDIOPLAYER_START:
                                Log.d(TAG, "audioStart");
                                if (audioTrack != null) {
                                    isPlaying = true;
                                    audioTrack.play();
                                }
                                break;
                            case AUDIOPLAYER_WRITE:
                                ttsCount++;
                                if (ttsCount % 5 == 0) {
                                    Log.d(TAG, "audioWrite");
                                    ttsCount = 0;
                                }
                                Bundle bundle = (Bundle) msg.obj;
                                byte[] audioData = bundle.getByteArray("audio");
                                if (audioTrack != null && audioData.length > 0) {
                                    audioTrack.write(audioData, 0, audioData.length);
                                }
                                break;
                            case AUDIOPLAYER_END:
                                Log.d(TAG, "audioEnd");
                                if (audioTrack != null) {
                                    audioTrack.stop();
                                    isPlaying = false;
                                }
                                break;

                        }
                    }
                };
                Looper.loop();
            }
        });
        mAudioPlayThread.start();
    }

    //开始播报
    private void startSpeak() {
        Log.d(TAG, "start-->");
        Log.d(TAG, "vcn = " + mTTSParams.vcn);     //发音人
        Log.d(TAG, "pitch = " + mTTSParams.pitch); //语调
        Log.d(TAG, "speed = " + mTTSParams.speed); //语速
        Log.d(TAG, "volume = " + mTTSParams.volume);//音量
        String text = tv_speech.getText().toString();
        Log.d(TAG, "text = " + text);
        if (audioTrack == null) {
            mAudioPlayHandler.sendEmptyMessage(AUDIOPLAYER_INIT);
        } else {
            if (isPlaying) {
                stopSpeak();
            }
            mAudioPlayHandler.sendEmptyMessage(AUDIOPLAYER_START);
        }

        /******************
         * 在线合成发音人设置接口，发音人可从构造方法中设入，也可通过功能参数动态修改。
         * xiaoyan，晓燕，⼥：中⽂
         * *******************/
        mOnlineTTS = new OnlineTTS(mTTSParams.vcn);
//        mOnlineTTS.vcn(mTTSParams.vcn);
        /********************
         * aue(必填):
         * 音频编码，可选值：raw：未压缩的pcm
         * lame：mp3 (当aue=lame时需传参sfl=1)
         * speex-org-wb;7： 标准开源speex（for speex_wideband，即16k）数字代表指定压缩等级（默认等级为8）
         * speex-org-nb;7： 标准开源speex（for speex_narrowband，即8k）数字代表指定压缩等级（默认等级为8）
         * speex;7：压缩格式，压缩等级1~10，默认为7（8k讯飞定制speex）
         * speex-wb;7：压缩格式，压缩等级1~10，默认为7（16k讯飞定制speex）
         * ****************************/
        mOnlineTTS.aue("raw");
        mOnlineTTS.auf("audio/L16;rate=" + sampleRate);
        mOnlineTTS.speed(mTTSParams.speed);//语速：0对应默认语速的1/2，100对应默认语速的2倍。最⼩值:0, 最⼤值:100
        mOnlineTTS.pitch(mTTSParams.pitch);//语调：0对应默认语速的1/2，100对应默认语速的2倍。最⼩值:0, 最⼤值:100
        mOnlineTTS.volume(mTTSParams.volume);//音量：0是静音，1对应默认音量1/2，100对应默认音量的2倍。最⼩值:0, 最⼤值:100
        mOnlineTTS.bgs(0);//合成音频的背景音 0:无背景音（默认值） 1:有背景音
        mOnlineTTS.registerCallbacks(mTTSCallback);
        int ret = mOnlineTTS.aRun(text);
        if (ret != 0) {
            Toaster.show("合成出错!ret=" + ret);
        }
    }

    //停止播报
    private void stopSpeak() {
        if (mOnlineTTS != null) {
            mAudioPlayHandler.removeCallbacksAndMessages(null);
            mAudioPlayHandler.sendEmptyMessage(AUDIOPLAYER_END);
            mOnlineTTS.stop();
            mOnlineTTS = null;
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
                ai_asr_audio_start_btn.setText(R.string.asring_button);
                ai_asr_audio_start_btn.setEnabled(false);
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
                    ai_asr_audio_start_btn.setText(R.string.asr_button);
                    ai_asr_audio_start_btn.setEnabled(true);
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
