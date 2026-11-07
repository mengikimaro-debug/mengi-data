package tz.mengidata.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;

final class AppLock {
    private static final String PREFS = "home_satellite_security";
    private static final String KEY_ALIAS = "home_satellite_pin_verifier";
    private static final String SALT = "pin_salt";
    private static final String VERIFIER = "pin_verifier";
    private static final String FAILURES = "pin_failures";
    private static final String LOCKOUTS = "pin_lockouts";
    private static final String LOCKED_UNTIL = "pin_locked_until";
    private static final int MAX_ATTEMPTS = 5;

    private AppLock() {}

    static boolean isEnabled(Context context) {
        SharedPreferences prefs = preferences(context);
        return prefs.contains(SALT) && prefs.contains(VERIFIER);
    }

    static boolean isValidPin(String pin) {
        if (pin == null || !pin.matches("[0-9]{6,12}")) return false;
        boolean ascending = true;
        boolean descending = true;
        boolean identical = true;
        for (int i = 1; i < pin.length(); i++) {
            int difference = pin.charAt(i) - pin.charAt(i - 1);
            ascending &= difference == 1;
            descending &= difference == -1;
            identical &= difference == 0;
        }
        return !ascending && !descending && !identical;
    }

    static void setPin(Context context, String pin) throws GeneralSecurityException {
        byte[] salt = new byte[32];
        new SecureRandom().nextBytes(salt);
        SharedPreferences.Editor editor = preferences(context).edit()
                .putString(SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
                .putString(VERIFIER, Base64.encodeToString(verifier(pin, salt), Base64.NO_WRAP))
                .remove(FAILURES)
                .remove(LOCKOUTS)
                .remove(LOCKED_UNTIL);
        if (!editor.commit()) throw new GeneralSecurityException("Imeshindwa kuhifadhi PIN.");
    }

    static void disable(Context context) {
        preferences(context).edit()
                .remove(SALT)
                .remove(VERIFIER)
                .remove(FAILURES)
                .remove(LOCKOUTS)
                .remove(LOCKED_UNTIL)
                .apply();
    }

    static long remainingLockoutMillis(Context context) {
        return Math.max(0, preferences(context).getLong(LOCKED_UNTIL, 0) - System.currentTimeMillis());
    }

    static boolean verify(Context context, String pin) throws GeneralSecurityException {
        SharedPreferences prefs = preferences(context);
        if (!isEnabled(context) || remainingLockoutMillis(context) > 0) return false;

        byte[] salt = Base64.decode(prefs.getString(SALT, ""), Base64.NO_WRAP);
        byte[] expected = Base64.decode(prefs.getString(VERIFIER, ""), Base64.NO_WRAP);
        if (MessageDigest.isEqual(expected, verifier(pin, salt))) {
            prefs.edit().remove(FAILURES).remove(LOCKOUTS).remove(LOCKED_UNTIL).apply();
            return true;
        }

        int failures = prefs.getInt(FAILURES, 0) + 1;
        SharedPreferences.Editor editor = prefs.edit();
        if (failures >= MAX_ATTEMPTS) {
            int lockouts = prefs.getInt(LOCKOUTS, 0) + 1;
            int exponent = Math.min(lockouts - 1, 4);
            long duration = Math.min(30_000L << exponent, 15 * 60_000L);
            editor.putInt(FAILURES, 0)
                    .putInt(LOCKOUTS, lockouts)
                    .putLong(LOCKED_UNTIL, System.currentTimeMillis() + duration);
        } else {
            editor.putInt(FAILURES, failures);
        }
        editor.apply();
        return false;
    }

    private static byte[] verifier(String pin, byte[] salt) throws GeneralSecurityException {
        byte[] pinBytes = pin.getBytes(StandardCharsets.UTF_8);
        byte[] input = new byte[salt.length + pinBytes.length];
        System.arraycopy(salt, 0, input, 0, salt.length);
        System.arraycopy(pinBytes, 0, input, salt.length, pinBytes.length);
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(getOrCreateKey());
        return mac.doFinal(input);
    }

    private static SecretKey getOrCreateKey() throws GeneralSecurityException {
        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            java.security.Key existing = keyStore.getKey(KEY_ALIAS, null);
            if (existing instanceof SecretKey) return (SecretKey) existing;

            KeyGenerator generator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_HMAC_SHA256, "AndroidKeyStore");
            generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                    KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .build());
            return generator.generateKey();
        } catch (java.io.IOException | java.security.UnrecoverableKeyException e) {
            throw new GeneralSecurityException("Android Keystore haipatikani.", e);
        }
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
