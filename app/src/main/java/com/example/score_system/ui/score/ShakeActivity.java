package com.example.score_system.ui.score;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.score_system.data.AttendanceRepository;
import com.example.score_system.data.AttendanceRepository.Task;
import com.example.score_system.ui.common.Ui;
import java.util.Locale;

public class ShakeActivity extends AppCompatActivity implements SensorEventListener {
    private AttendanceRepository repository;
    private SensorManager sensors;
    private Sensor accelerometer;
    private long taskId;
    private long accountId;
    private Task task;
    private TextView status;
    private TextView remaining;
    private TextView symbol;
    private boolean finished;
    private long lastImpulse;
    private int impulses;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable countdown = new Runnable() {
        @Override public void run() {
            if (finished) return;
            long seconds = Math.max(0, (task.end - System.currentTimeMillis() + 999) / 1000);
            remaining.setText(String.format(Locale.CHINA, "距离结束 %02d:%02d", seconds / 60, seconds % 60));
            if (seconds <= 0) stop("签到已结束，请返回查看考勤记录");
            else handler.postDelayed(this, 1000);
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        repository = new AttendanceRepository(this);
        taskId = getIntent().getLongExtra("task_id", 0);
        AttendanceRepository.Account account = repository.currentAccount();
        if (account == null || account.isTeacher()) { finish(); return; }
        accountId = account.id;
        for (Task candidate : repository.tasks()) if (candidate.id == taskId) task = candidate;
        if (task == null) { finish(); return; }
        LinearLayout body = Ui.page(this);
        Ui.heading(body, "摇一摇签到", task.courseName + " · " + task.room);
        Ui.hero(body, "让到达被记录", "握稳手机，连续轻摇几下\n识别成功后会自动完成签到。");
        symbol = Ui.text(this, "↔", 100, Ui.BLUE);
        symbol.setGravity(Gravity.CENTER);
        symbol.setPadding(0, Ui.dp(this, 35), 0, Ui.dp(this, 20));
        body.addView(symbol);
        status = Ui.text(this, "正在等待摇动…", 20, Ui.INK);
        status.setGravity(Gravity.CENTER);
        body.addView(status);
        remaining = Ui.text(this, "", 14, Ui.MUTED);
        remaining.setGravity(Gravity.CENTER);
        remaining.setPadding(0, Ui.dp(this, 16), 0, Ui.dp(this, 24));
        body.addView(remaining);
        Ui.button(body, "返回考勤", true, this::finish);
        sensors = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensors == null ? null : sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (!task.active() || !AttendanceRepository.WAITING.equals(task.ownStatus)) stop("本次签到已结束或已有考勤结果");
        else if (accelerometer == null) stop("此设备没有加速度传感器，请使用支持摇一摇的手机");
    }

    @Override protected void onResume() {
        super.onResume();
        if (task == null || finished || accelerometer == null) return;
        impulses = 0;
        lastImpulse = 0;
        sensors.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        handler.post(countdown);
    }

    @Override protected void onPause() {
        if (sensors != null) sensors.unregisterListener(this);
        handler.removeCallbacksAndMessages(null);
        super.onPause();
    }

    @Override public void onSensorChanged(SensorEvent event) {
        if (finished) return;
        float horizontal = event.values[0] / SensorManager.GRAVITY_EARTH;
        float vertical = event.values[1] / SensorManager.GRAVITY_EARTH;
        float depth = event.values[2] / SensorManager.GRAVITY_EARTH;
        double acceleration = Math.sqrt(horizontal * horizontal + vertical * vertical + depth * depth);
        long now = SystemClock.elapsedRealtime();
        if (acceleration < 2.3 || now - lastImpulse < 180) return;
        if (now - lastImpulse > 1400) impulses = 0;
        lastImpulse = now;
        impulses++;
        symbol.animate().rotationBy(18).setDuration(100).withEndAction(() -> symbol.animate().rotation(0).setDuration(100).start()).start();
        if (impulses < 2) { status.setText("识别到了，再摇一下"); return; }
        try {
            AttendanceRepository.Account current = repository.currentAccount();
            if (current == null || current.id != accountId) { stop("账号已切换，请重新进入签到"); return; }
            repository.signByShake(taskId);
            symbol.setText("✓");
            symbol.setTextColor(Ui.GREEN);
            symbol.performHapticFeedback(HapticFeedbackConstants.CONFIRM);
            stop("签到成功，准时到达！");
            remaining.setText("签到记录已保存");
        } catch (IllegalArgumentException exception) {
            stop(exception.getMessage());
        } catch (android.database.SQLException exception) {
            stop("保存失败，请返回后重试");
        }
    }

    private void stop(String message) {
        finished = true;
        status.setText(message);
        if (sensors != null) sensors.unregisterListener(this);
        handler.removeCallbacksAndMessages(null);
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (repository != null) repository.close();
        super.onDestroy();
    }
}
