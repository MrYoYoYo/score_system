package com.example.score_system.data;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesManager {
    private static final String PREF_NAME = "score_system_prefs";
    private static final String KEY_EMAIL = "backup_email";
    private static final String KEY_EMAIL_PASSWORD = "email_password";

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    public PreferencesManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public void saveEmailConfig(String email, String password) {
        editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_EMAIL_PASSWORD, password);
        editor.apply();
    }

    public String getEmail() {
        return prefs.getString(KEY_EMAIL, "");
    }

    public String getEmailPassword() {
        return prefs.getString(KEY_EMAIL_PASSWORD, "");
    }

}
