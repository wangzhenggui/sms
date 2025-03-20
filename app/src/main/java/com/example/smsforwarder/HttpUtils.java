package com.example.smsforwarder;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class HttpUtils {
    private static final String TAG = "HttpUtils";

    public static boolean sendSmsToServer(String serverUrl, String sender, String message) {
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(serverUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setRequestProperty("Accept", "application/json");
            urlConnection.setDoOutput(true);
            urlConnection.setConnectTimeout(10000);
            urlConnection.setReadTimeout(10000);

            // 创建JSON数据
            JSONObject jsonParam = new JSONObject();
            jsonParam.put("sender", sender);
            jsonParam.put("message", message);
            jsonParam.put("timestamp", System.currentTimeMillis());

            // 发送数据
            try (OutputStream os = urlConnection.getOutputStream()) {
                byte[] input = jsonParam.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // 获取响应
            int responseCode = urlConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(urlConnection.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    Log.d(TAG, "服务器响应: " + response.toString());
                }
                return true;
            } else {
                Log.e(TAG, "HTTP错误码: " + responseCode);
                return false;
            }
        } catch (IOException | JSONException e) {
            Log.e(TAG, "发送短信到服务器失败", e);
            return false;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }
}