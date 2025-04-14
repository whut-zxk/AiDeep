package com.example.aideep.application;

import android.app.Application;
import android.util.Log;

import com.example.aideep.R;
import com.hjq.toast.Toaster;
import com.iflytek.sparkchain.core.SparkChain;
import com.iflytek.sparkchain.core.SparkChainConfig;

public class MyApplication extends Application {
    private String TAG = "MyApplication";
    @Override
    public void onCreate() {
        super.onCreate();
        // 初始化 Toast 框架
        Toaster.init(this);
        SDKInit();
    }

    private void SDKInit(){
        Log.d(TAG,"initSDK");
        // 初始化SDK，Appid等信息在清单中配置
        SparkChainConfig sparkChainConfig = SparkChainConfig.builder();
        sparkChainConfig.appID(getResources().getString(R.string.appid))
                .apiKey(getResources().getString(R.string.apikey))
                .apiSecret(getResources().getString(R.string.apiSecret))//应用申请的appid三元组
                //   .uid("")
                .logLevel(666);

        int ret = SparkChain.getInst().init(getApplicationContext(),sparkChainConfig);
        String result;
        if(ret == 0){
            result = "SDK初始化成功,请选择相应的功能点击体验。";
        }else{
            result = "SDK初始化失败,错误码:" + ret;
        }
        Log.d(TAG,result);
    }
}
