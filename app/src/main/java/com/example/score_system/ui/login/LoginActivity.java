package com.example.score_system.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.score_system.data.AttendanceRepository;
import com.example.score_system.ui.common.Ui;
import com.example.score_system.ui.main.MainActivity;
import com.google.android.material.button.MaterialButton;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {
    private AttendanceRepository repository;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private boolean registering;
    private String lastUsername = "";
    private boolean teacher = true;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        repository = new AttendanceRepository(this);
        getSharedPreferences("score_system_prefs", MODE_PRIVATE).edit().remove("password").remove("save_pwd").apply();
        lastUsername = getSharedPreferences("attendance_login", MODE_PRIVATE).getString("username", "");
        if (state != null) {
            registering = state.getBoolean("registering");
            teacher = state.getBoolean("teacher", true);
        }
        render();
    }

    private void render() {
        LinearLayout body = Ui.page(this);
        Ui.hero(body, "班级点名系统", "轻轻一摇，记录每一次准时到达。");
        Ui.section(body, registering ? "创建你的账号" : "欢迎回来");
        RadioGroup roles = new RadioGroup(this);
        roles.setOrientation(LinearLayout.HORIZONTAL);
        RadioButton teacherChoice = new RadioButton(this);
        teacherChoice.setId(android.view.View.generateViewId());
        teacherChoice.setText("我是教师");
        RadioButton studentChoice = new RadioButton(this);
        studentChoice.setId(android.view.View.generateViewId());
        studentChoice.setText("我是学生");
        roles.addView(teacherChoice, new RadioGroup.LayoutParams(0, Ui.dp(this, 56), 1));
        roles.addView(studentChoice, new RadioGroup.LayoutParams(0, Ui.dp(this, 56), 1));
        roles.check(teacher ? teacherChoice.getId() : studentChoice.getId());
        body.addView(roles);
        EditText username = Ui.field(body, "登录账号", lastUsername, InputType.TYPE_CLASS_TEXT);
        username.setSingleLine(true);
        EditText password = Ui.field(body, "密码（至少 6 位）", "", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        LinearLayout extra = Ui.column(this);
        body.addView(extra);
        EditText name = null;
        EditText phone = null;
        EditText number = null;
        EditText className = null;
        EditText confirm = null;
        if (registering) {
            confirm = Ui.field(extra, "再次输入密码", "", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            name = Ui.field(extra, "姓名", "", InputType.TYPE_CLASS_TEXT);
            phone = Ui.field(extra, "手机号（选填）", "", InputType.TYPE_CLASS_PHONE);
            number = Ui.field(extra, "学号", "", InputType.TYPE_CLASS_TEXT);
            className = Ui.field(extra, "班级", "", InputType.TYPE_CLASS_TEXT);
        }
        final EditText nameField = name;
        final EditText phoneField = phone;
        final EditText numberField = number;
        final EditText classField = className;
        final EditText confirmField = confirm;
        if (registering) {
            Ui.showField(numberField, !teacher);
            Ui.showField(classField, !teacher);
        }
        roles.setOnCheckedChangeListener((group, checkedId) -> {
            teacher = checkedId == teacherChoice.getId();
            if (registering) {
                Ui.showField(numberField, !teacher);
                Ui.showField(classField, !teacher);
            }
        });
        MaterialButton submit = Ui.button(body, registering ? "注册账号" : "登录", () -> {});
        MaterialButton toggle = Ui.button(body, registering ? "已有账号，去登录" : "还没有账号？注册", true, () -> {
            lastUsername = username.getText().toString().trim();
            registering = !registering;
            render();
        });
        submit.setOnClickListener(view -> {
            String account = username.getText().toString().trim();
            String secret = password.getText().toString();
            if (account.isEmpty() || secret.isEmpty()) { toast("请输入账号和密码"); return; }
            if (registering && !secret.equals(confirmField.getText().toString())) { toast("两次密码输入不一致"); return; }
            String person = registering ? nameField.getText().toString().trim() : "";
            String mobile = registering ? phoneField.getText().toString().trim() : "";
            String studentNumber = registering ? numberField.getText().toString().trim() : "";
            String studentClass = registering ? classField.getText().toString().trim() : "";
            boolean registration = registering;
            boolean teacherRole = teacher;
            submit.setEnabled(false);
            toggle.setEnabled(false);
            teacherChoice.setEnabled(false);
            studentChoice.setEnabled(false);
            submit.setText(registration ? "正在创建…" : "正在登录…");
            worker.execute(() -> {
                try {
                    if (registration) repository.register(account, secret, person, mobile, teacherRole, studentNumber, studentClass);
                    else repository.login(account, secret, teacherRole);
                    runOnUiThread(() -> {
                        if (isDestroyed() || isFinishing()) return;
                        lastUsername = account;
                        getSharedPreferences("attendance_login", MODE_PRIVATE).edit().putString("username", account).apply();
                        if (registration) {
                            toast("注册成功，请登录");
                            registering = false;
                            render();
                        } else {
                            startActivity(new Intent(this, MainActivity.class));
                            finish();
                        }
                    });
                } catch (Exception exception) {
                    runOnUiThread(() -> {
                        if (isDestroyed() || isFinishing()) return;
                        toast(exception instanceof IllegalArgumentException ? exception.getMessage() : "操作失败，请检查填写信息后重试");
                        submit.setEnabled(true);
                        toggle.setEnabled(true);
                        teacherChoice.setEnabled(true);
                        studentChoice.setEnabled(true);
                        submit.setText(registration ? "注册账号" : "登录");
                    });
                }
            });
        });
    }

    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }

    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putBoolean("registering", registering);
        state.putBoolean("teacher", teacher);
    }

    @Override protected void onDestroy() {
        worker.execute(repository::close);
        worker.shutdown();
        super.onDestroy();
    }
}
