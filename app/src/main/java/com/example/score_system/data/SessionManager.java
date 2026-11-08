package com.example.score_system.data;

import android.content.Context;
import android.content.SharedPreferences;

public final class SessionManager {
    private final SharedPreferences preferences;

    public SessionManager(Context context) {
        preferences = context.getSharedPreferences("attendance_session", Context.MODE_PRIVATE);
    }

    public long accountId() {
        return preferences.getLong("account_id", -1);
    }

    public void signIn(long accountId) {
        preferences.edit().putLong("account_id", accountId).apply();
    }

    public void signOut() {
        preferences.edit().remove("account_id").apply();
    }
}
