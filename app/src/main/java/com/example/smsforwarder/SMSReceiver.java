package com.example.smsforwarder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class SMSReceiver extends BroadcastReceiver {
    private static final String TAG = "SMSReceiver";
    private static final String PREFS_NAME = "SMSForwarderPrefs";
    private static final String SERVER_URL_KEY = "server_url";
    private static final String KEYWORDS_KEY = "keywords";
    private static final String SERVICE_ENABLED_KEY = "service_enabled";

    private final Executor executor = Executors.newSingleThreadExecutor();

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean serviceEnabled = prefs.getBoolean(SERVICE_ENABLED_KEY, false);

        if (!serviceEnabled) {
            Log.d(TAG, "服务未启用，不处理短信");
            return;
        }

        String serverUrl = prefs.getString(SERVER_URL_KEY, "");
        String keywordsStr = prefs.getString(KEYWORDS_KEY, "");
        List<String> keywords = Arrays.asList(keywordsStr.split(","));

        if (serverUrl.isEmpty()) {
            Log.e(TAG, "服务器地址未设置");
            return;
        }

        if ("android.provider.Telephony.SMS_RECEIVED".equals(intent.getAction())) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                Object[] pdus = (Object[]) bundle.get("pdus");
                if (pdus != null) {
                    for (Object pdu : pdus) {
                        SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
                        String sender = smsMessage.getDisplayOriginatingAddress();
                        String messageBody = smsMessage.getMessageBody();
                        
                        Log.d(TAG, "收到短信: " + messageBody);
                        
                        // 检查短信内容是否包含关键词
                        boolean containsKeyword = keywords.isEmpty(); // 如果没有设置关键词，则处理所有短信
                        
                        for (String keyword : keywords) {
                            if (!keyword.trim().isEmpty() && messageBody.contains(keyword.trim())) {
                                containsKeyword = true;
                                break;
                            }
                        }
                        
                        if (containsKeyword) {
                            // 转发短信到服务器
                            final String finalSender = sender;
                            final String finalMessageBody = messageBody;
                            
                            executor.execute(() -> {
                                boolean success = HttpUtils.sendSmsToServer(serverUrl, finalSender, finalMessageBody);
                                Log.d(TAG, "短信转发" + (success ? "成功" : "失败"));
                                
                                // 如果MainActivity正在运行，更新日志
                                if (context instanceof MainActivity) {
                                    ((MainActivity) context).runOnUiThread(() -> {
                                        ((MainActivity) context).addLog("收到短信: " + finalSender + "\n" + 
                                                                       "内容: " + finalMessageBody + "\n" + 
                                                                       "转发" + (success ? "成功" : "失败"));
                                    });
                                }
                            });
                        } else {
                            Log.d(TAG, "短信不包含关键词，不转发");
                        }
                    }
                }
            }
        }
    }
}