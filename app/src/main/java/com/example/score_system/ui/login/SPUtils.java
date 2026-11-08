package com.example.score_system.ui.login;

import android.content.Context;
import android.content.SharedPreferences;

public class SPUtils {
    private static final String SP_NAME = "score_system_prefs";
    private static final String KEY_ACCOUNT = "account";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_SAVE_PWD = "save_pwd";

    private static SharedPreferences getSP(Context context) {
        return context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
    }

    // 保存密码
    public static void savePassword(Context context, String account, String password, boolean isSave) {
        SharedPreferences.Editor editor = getSP(context).edit();
        editor.putString(KEY_ACCOUNT, account);
        editor.putString(KEY_PASSWORD, password);
        editor.putBoolean(KEY_SAVE_PWD, isSave);
        editor.apply();
    }

    // 获取账号
    public static String getAccount(Context context) {
        return getSP(context).getString(KEY_ACCOUNT, "");
    }

    // 获取密码
    public static String getPassword(Context context) {
        return getSP(context).getString(KEY_PASSWORD, "");
    }

    // 是否保存密码
    public static boolean isSavePwd(Context context) {
        return getSP(context).getBoolean(KEY_SAVE_PWD, false);
    }
}