package com.example.smsforwarder;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 123;
    private static final String PREFS_NAME = "SMSForwarderPrefs";
    private static final String SERVER_URL_KEY = "server_url";
    private static final String KEYWORDS_KEY = "keywords";
    private static final String SERVICE_ENABLED_KEY = "service_enabled";

    private EditText serverUrlEditText;
    private EditText keywordsEditText;
    private Switch serviceSwitch;
    private TextView statusTextView;
    private TextView logTextView;
    private Button saveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化视图
        serverUrlEditText = findViewById(R.id.serverUrlEditText);
        keywordsEditText = findViewById(R.id.keywordsEditText);
        serviceSwitch = findViewById(R.id.serviceSwitch);
        statusTextView = findViewById(R.id.statusTextView);
        logTextView = findViewById(R.id.logTextView);
        saveButton = findViewById(R.id.saveButton);

        // 加载保存的设置
        loadSettings();

        // 设置保存按钮点击事件
        saveButton.setOnClickListener(v -> saveSettings());

        // 设置服务开关事件
        serviceSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // 检查权限
                if (checkAndRequestPermissions()) {
                    enableService();
                }
            } else {
                disableService();
            }
        });

        // 检查权限
        checkAndRequestPermissions();
    }

    private boolean checkAndRequestPermissions() {
        String[] permissions = {
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS,
                Manifest.permission.INTERNET
        };

        boolean allPermissionsGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                break;
            }
        }

        if (!allPermissionsGranted) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                if (serviceSwitch.isChecked()) {
                    enableService();
                }
            } else {
                Toast.makeText(this, "需要所有权限才能正常运行", Toast.LENGTH_LONG).show();
                serviceSwitch.setChecked(false);
            }
        }
    }

    private void loadSettings() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        serverUrlEditText.setText(prefs.getString(SERVER_URL_KEY, ""));
        keywordsEditText.setText(prefs.getString(KEYWORDS_KEY, ""));
        boolean serviceEnabled = prefs.getBoolean(SERVICE_ENABLED_KEY, false);
        serviceSwitch.setChecked(serviceEnabled);
        
        if (serviceEnabled) {
            statusTextView.setText("服务状态: 已启动");
        } else {
            statusTextView.setText("服务状态: 未启动");
        }
    }

    private void saveSettings() {
        String serverUrl = serverUrlEditText.getText().toString().trim();
        String keywords = keywordsEditText.getText().toString().trim();

        if (serverUrl.isEmpty()) {
            Toast.makeText(this, "请输入服务器地址", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(SERVER_URL_KEY, serverUrl);
        editor.putString(KEYWORDS_KEY, keywords);
        editor.apply();

        Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show();
        addLog("设置已更新: 服务器地址=" + serverUrl + ", 关键词=" + keywords);
    }

    private void enableService() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(SERVICE_ENABLED_KEY, true);
        editor.apply();

        statusTextView.setText("服务状态: 已启动");
        addLog("短信监听服务已启动");
        Toast.makeText(this, "短信监听服务已启动", Toast.LENGTH_SHORT).show();
    }

    private void disableService() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(SERVICE_ENABLED_KEY, false);
        editor.apply();

        statusTextView.setText("服务状态: 未启动");
        addLog("短信监听服务已停止");
        Toast.makeText(this, "短信监听服务已停止", Toast.LENGTH_SHORT).show();
    }

    public void addLog(String message) {
        String currentLog = logTextView.getText().toString();
        logTextView.setText(currentLog + "\n" + message);
    }
}