package com.example.score_system.ui.settings;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.score_system.R;
import com.example.score_system.data.PreferencesManager;

public class EmailConfigActivity extends AppCompatActivity {
    private PreferencesManager prefsManager;

    private EditText etEmail;
    private EditText etPassword;
    private Button btnSave;
    private Button btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_config);

        prefsManager = new PreferencesManager(this);

        initViews();
        loadConfig();
    }

    private void initViews() {
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnSave = findViewById(R.id.btn_save);
        btnBack = findViewById(R.id.btn_back);

        btnSave.setOnClickListener(v -> saveConfig());
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadConfig() {
        etEmail.setText(prefsManager.getEmail());
        etPassword.setText(prefsManager.getEmailPassword());
    }

    private void saveConfig() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty()) {
            Toast.makeText(this, "请填写邮箱地址", Toast.LENGTH_SHORT).show();
            return;
        }

        // 保存邮箱和密码（用于系统邮件应用）
        prefsManager.saveEmailConfig(email, password);
        Toast.makeText(this, "保存成功\n发送邮件时将使用系统邮件应用", Toast.LENGTH_SHORT).show();
        finish();
    }
}
