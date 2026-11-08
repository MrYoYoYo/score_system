package com.example.score_system.data;

import android.util.Base64;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordHasher {
    private PasswordHasher() {}

    public static String hash(String password) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return Base64.encodeToString(salt, Base64.NO_WRAP) + ":" + derive(password, salt);
    }

    public static boolean verify(String password, String encoded) {
        try {
            String[] parts = encoded.split(":");
            if (parts.length != 2) return false;
            byte[] actual = Base64.decode(derive(password, Base64.decode(parts[0], Base64.NO_WRAP)), Base64.NO_WRAP);
            return MessageDigest.isEqual(actual, Base64.decode(parts[1], Base64.NO_WRAP));
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static String derive(String password, byte[] salt) {
        PBEKeySpec specification = new PBEKeySpec(password.toCharArray(), salt, 30000, 256);
        try {
            return Base64.encodeToString(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(specification).getEncoded(), Base64.NO_WRAP);
        } catch (Exception exception) {
            throw new IllegalStateException("无法保存密码", exception);
        } finally {
            specification.clearPassword();
        }
    }
}
