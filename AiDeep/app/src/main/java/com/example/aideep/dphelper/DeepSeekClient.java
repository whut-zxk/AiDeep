package com.example.aideep.dphelper;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DeepSeekClient {
    public interface DeepSeekClientCallback {
        void onSuccess(String str);

        void onFail(String str);

    }

    private String TAG = "DeepSeekClient";
    private static final String API_URL = "https://api.deepseek.com/chat/completions";
    private static final String NAME = "EmoRob";
    private static final String API_KEY = "sk-7f947c5c24e54304b1bf359370c21c1c";

    private volatile static DeepSeekClient instance;
    private Activity activity;

    private DeepSeekClient(Activity activity) {
        this.activity = activity;

    }

    public static DeepSeekClient getInstance(Activity activity) {
        if (instance == null) {
            synchronized (DeepSeekClient.class) {
                if (instance == null) {
                    instance = new DeepSeekClient(activity);
                }
            }
        }
        return instance;
    }

    public void requestDSApi(DeepSeekClientCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                OkHttpClient client = new OkHttpClient();
                // 创建请求体
                MediaType mediaType = MediaType.parse("application/json");
                String jsonBody = "{\n" +
                        "        \"model\": \"deepseek-chat\",\n" +
                        "        \"messages\": [\n" +
                        "          {\"role\": \"system\", \"content\": \"You are a helpful assistant.\"},\n" +
                        "          {\"role\": \"user\", \"content\": \"$message\"}\n" +
                        "        ],\n" +
                        "        \"stream\": false\n" +
                        "      }";
                RequestBody body = RequestBody.create(jsonBody, mediaType);

                // 创建请求
                Request request = new Request.Builder()
                        .url(API_URL)
                        .post(body)
                        .addHeader("Authorization", "Bearer " + API_KEY)
                        .addHeader("Content-Type", "application/json")
                        .build();
                String result = "";
                // 发送请求
                try {
                    Response response = client.newCall(request).execute();
                    if (response.isSuccessful()) {
                        result = response.body().string();
                        Log.d(TAG, "Response success: " + result);
                        String finalResult = result;
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(finalResult);
                            }
                        });

                    } else {
                        result = response.code() + " " + response.message();
                        Log.d(TAG, "Request failed: " + result);
                        String finalResult1 = result;
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                callback.onFail(finalResult1);
                            }
                        });

                    }
                } catch (Exception e) {
                    Log.e(TAG, "Response Exception: " + result, e);
                }
            }
        }).start();

    }
}
