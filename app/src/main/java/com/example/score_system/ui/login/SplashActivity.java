package com.example.score_system.ui.login;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.score_system.data.AttendanceRepository;
import com.example.score_system.ui.common.Ui;
import com.example.score_system.ui.main.MainActivity;

public class SplashActivity extends AppCompatActivity {
    private static final String ANIMATED_TITLE = "班.级.点.名.系.统";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView title;
    private long started;
    private final Runnable animate = new Runnable() {
        @Override public void run() {
            long elapsed = android.os.SystemClock.elapsedRealtime() - started;
            int visibleLength = Math.min(
                    ANIMATED_TITLE.length(),
                    1 + (int) (elapsed / 220)
            );
            title.setText(ANIMATED_TITLE.substring(0, visibleLength));
            if (elapsed >= 2800) {
                boolean signedIn;
                try (AttendanceRepository repository = new AttendanceRepository(SplashActivity.this)) {
                    signedIn = repository.currentAccount() != null;
                }
                startActivity(new Intent(SplashActivity.this, signedIn ? MainActivity.class : LoginActivity.class));
                finish();
            } else handler.postDelayed(this, 100);
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = Ui.column(this);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Ui.BACKGROUND);
        setContentView(root);
        Ui.insets(root);
        TextView mark = Ui.text(this, "✓", 48, Ui.BLUE);
        mark.setGravity(Gravity.CENTER);
        android.graphics.drawable.GradientDrawable badge = new android.graphics.drawable.GradientDrawable();
        badge.setColor(Color.rgb(231, 238, 255));
        badge.setCornerRadius(Ui.dp(this, 30));
        mark.setBackground(badge);
        root.addView(mark, new LinearLayout.LayoutParams(Ui.dp(this, 100), Ui.dp(this, 100)));
        title = Ui.text(this, "班", 29, Ui.INK);
        title.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, Ui.dp(this, 32), 0, 0);
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));
        root.setAlpha(0);
        root.animate().alpha(1).setDuration(1400).start();
    }

    @Override protected void onStart() {
        super.onStart();
        started = android.os.SystemClock.elapsedRealtime();
        handler.post(animate);
    }

    @Override protected void onStop() {
        handler.removeCallbacksAndMessages(null);
        super.onStop();
    }
}
