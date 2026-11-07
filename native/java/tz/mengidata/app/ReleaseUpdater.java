package tz.mengidata.app;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.content.pm.PackageInstaller;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class ReleaseUpdater {
    private static final String RELEASE_URL =
            "https://api.github.com/repos/mengikimaro-debug/MengiData/releases/latest";
    private static final String EXPECTED_SIGNER =
            "1fc743c14db0a0fda9304e092e497c95fcb7388eec6e7b7dc55a20a491dfe8a7";
    private static final String PREFS = "home_satellite_updates";
    private static final String AUTO_UPDATES = "auto_updates";
    private static final String LAST_CHECK = "last_auto_check";
    private static final long MAX_APK_SIZE = 100L * 1024 * 1024;
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    interface CheckCallback {
        void onComplete(Update update, String error);
    }

    interface DownloadCallback {
        void onComplete(File file, String error);
    }

    interface InstallCallback {
        void onComplete(String error);
    }

    static final class Update {
        final String version;
        final String notes;
        final String downloadUrl;
        final long size;

        Update(String version, String notes, String downloadUrl, long size) {
            this.version = version;
            this.notes = notes;
            this.downloadUrl = downloadUrl;
            this.size = size;
        }
    }

    private ReleaseUpdater() {}

    static boolean autoUpdatesEnabled(Context context) {
        return preferences(context).getBoolean(AUTO_UPDATES, false);
    }

    static void setAutoUpdatesEnabled(Context context, boolean enabled) {
        preferences(context).edit().putBoolean(AUTO_UPDATES, enabled).apply();
    }

    static boolean shouldCheckOnLaunch(Context context) {
        if (!autoUpdatesEnabled(context)) return false;
        SharedPreferences prefs = preferences(context);
        long now = System.currentTimeMillis();
        if (now - prefs.getLong(LAST_CHECK, 0) < 12 * 60 * 60 * 1000L) return false;
        prefs.edit().putLong(LAST_CHECK, now).apply();
        return true;
    }

    static String currentVersion(Context context) {
        try {
            return normalizeVersion(context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0).versionName);
        } catch (Exception e) {
            return "—";
        }
    }

    static void check(Context context, CheckCallback callback) {
        EXECUTOR.execute(() -> {
            Update update = null;
            String error = null;
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(RELEASE_URL).openConnection();
                connection.setConnectTimeout(10_000);
                connection.setReadTimeout(10_000);
                connection.setRequestProperty("Accept", "application/vnd.github+json");
                connection.setRequestProperty("User-Agent", "Mengidata-Android");
                try {
                    int status = connection.getResponseCode();
                    if (status == HttpURLConnection.HTTP_NOT_FOUND) {
                        throw new IllegalStateException(
                                "Hakuna release ya umma iliyochapishwa kwenye GitHub bado.");
                    }
                    if (status < 200 || status >= 300) {
                        throw new IllegalStateException("GitHub haipatikani sasa (HTTP " + status + ").");
                    }
                    JSONObject release = new JSONObject(readAll(connection.getInputStream(), 2_000_000));
                    String version = normalizeVersion(release.optString("tag_name", ""));
                    if (version.isEmpty()) throw new IllegalStateException("Release haina namba ya toleo sahihi.");
                    if (compareVersions(version, installedVersion(context)) <= 0) {
                        throw new IllegalStateException("Unatumia toleo la hivi karibuni: "
                                + installedVersion(context) + ".");
                    }
                    JSONArray assets = release.optJSONArray("assets");
                    JSONObject apk = findApkAsset(assets);
                    if (apk == null) throw new IllegalStateException("Release mpya haina APK ya kusakinisha.");
                    String downloadUrl = apk.optString("browser_download_url", "");
                    Uri uri = Uri.parse(downloadUrl);
                    long size = apk.optLong("size", 0);
                    if (!"https".equals(uri.getScheme()) || !"github.com".equals(uri.getHost())
                            || size <= 0 || size > MAX_APK_SIZE) {
                        throw new IllegalStateException("APK ya release ina anwani au ukubwa usio salama.");
                    }
                    update = new Update(version, release.optString("body", ""), downloadUrl, size);
                } finally {
                    connection.disconnect();
                }
            } catch (Exception e) {
                error = e.getMessage() == null ? "Imeshindwa kuangalia update." : e.getMessage();
            }
            Update result = update;
            String message = error;
            MAIN.post(() -> callback.onComplete(result, message));
        });
    }

    static void download(Context context, Update update, DownloadCallback callback) {
        EXECUTOR.execute(() -> {
            File apk = null;
            String error = null;
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(update.downloadUrl).openConnection();
                connection.setConnectTimeout(15_000);
                connection.setReadTimeout(30_000);
                connection.setInstanceFollowRedirects(true);
                connection.setRequestProperty("User-Agent", "Mengidata-Android");
                try {
                    int status = connection.getResponseCode();
                    if (status < 200 || status >= 300) {
                        throw new IllegalStateException("Imeshindwa kupakua APK (HTTP " + status + ").");
                    }
                    long expectedLength = connection.getContentLengthLong();
                    if (expectedLength > MAX_APK_SIZE || (expectedLength > 0 && expectedLength != update.size)) {
                        throw new IllegalStateException("Ukubwa wa APK hauendani na taarifa ya release.");
                    }
                    apk = new File(context.getCacheDir(), "home-satellite-tv-update.apk");
                    long written = copyBounded(connection.getInputStream(), apk, MAX_APK_SIZE);
                    if (written != update.size) throw new IllegalStateException("APK haikupakuliwa yote.");
                    verifyApk(context, apk, update.version);
                } finally {
                    connection.disconnect();
                }
            } catch (Exception e) {
                if (apk != null) apk.delete();
                error = e.getMessage() == null ? "Imeshindwa kupakua update." : e.getMessage();
                apk = null;
            }
            File result = apk;
            String message = error;
            MAIN.post(() -> callback.onComplete(result, message));
        });
    }

    static boolean canInstallPackages(Context context) {
        return Build.VERSION.SDK_INT < 26 || context.getPackageManager().canRequestPackageInstalls();
    }

    static void openInstallPermissionSettings(Activity activity) {
        if (Build.VERSION.SDK_INT < 26) return;
        Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:" + activity.getPackageName()));
        activity.startActivity(intent);
    }

    static void install(Context context, File apk, InstallCallback callback) {
        EXECUTOR.execute(() -> {
            String error = null;
            PackageInstaller installer = context.getPackageManager().getPackageInstaller();
            int sessionId = -1;
            try {
                PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                        PackageInstaller.SessionParams.MODE_FULL_INSTALL);
                params.setAppPackageName(context.getPackageName());
                sessionId = installer.createSession(params);
                try (PackageInstaller.Session session = installer.openSession(sessionId);
                     InputStream input = new FileInputStream(apk);
                     OutputStream output = session.openWrite("home-satellite-tv.apk", 0, apk.length())) {
                    byte[] buffer = new byte[16 * 1024];
                    int count;
                    while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
                    session.fsync(output);
                    int flags = PendingIntent.FLAG_UPDATE_CURRENT;
                    if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
                    PendingIntent status = PendingIntent.getBroadcast(context, sessionId,
                            new Intent(context, UpdateInstallReceiver.class), flags);
                    session.commit(status.getIntentSender());
                }
            } catch (Exception e) {
                if (sessionId >= 0) installer.abandonSession(sessionId);
                error = e.getMessage() == null ? "Imeshindwa kuanzisha usakinishaji." : e.getMessage();
            }
            String message = error;
            MAIN.post(() -> callback.onComplete(message));
        });
    }

    private static JSONObject findApkAsset(JSONArray assets) throws Exception {
        if (assets == null) return null;
        for (int i = 0; i < assets.length(); i++) {
            JSONObject item = assets.getJSONObject(i);
            if ("Mengidata.apk".equalsIgnoreCase(item.optString("name"))) return item;
        }
        for (int i = 0; i < assets.length(); i++) {
            JSONObject item = assets.getJSONObject(i);
            if (item.optString("name").toLowerCase(java.util.Locale.ROOT).endsWith(".apk")) return item;
        }
        return null;
    }

    private static String installedVersion(Context context) throws Exception {
        PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        return info.versionName == null ? "0" : normalizeVersion(info.versionName);
    }

    private static String normalizeVersion(String value) {
        String version = value.trim().replaceFirst("^[vV]", "");
        return version.matches("[0-9]+(\\.[0-9]+){1,3}") ? version : "";
    }

    private static int compareVersions(String left, String right) {
        String[] a = left.split("\\.");
        String[] b = right.split("\\.");
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            int av = i < a.length ? Integer.parseInt(a[i]) : 0;
            int bv = i < b.length ? Integer.parseInt(b[i]) : 0;
            if (av != bv) return Integer.compare(av, bv);
        }
        return 0;
    }

    private static void verifyApk(Context context, File apk, String version) throws Exception {
        PackageManager manager = context.getPackageManager();
        int flags = Build.VERSION.SDK_INT >= 28
                ? PackageManager.GET_SIGNING_CERTIFICATES : PackageManager.GET_SIGNATURES;
        PackageInfo archive = manager.getPackageArchiveInfo(apk.getAbsolutePath(), flags);
        if (archive == null || !context.getPackageName().equals(archive.packageName)
                || archive.versionName == null
                || compareVersions(normalizeVersion(archive.versionName), version) != 0) {
            throw new IllegalStateException("APK si ya Mengidata au toleo lake halilingani.");
        }
        android.content.pm.Signature[] signatures;
        if (Build.VERSION.SDK_INT >= 28 && archive.signingInfo != null) {
            signatures = archive.signingInfo.getApkContentsSigners();
        } else {
            signatures = archive.signatures;
        }
        if (signatures == null || signatures.length != 1
                || !EXPECTED_SIGNER.equals(hex(MessageDigest.getInstance("SHA-256")
                .digest(signatures[0].toByteArray())))) {
            throw new SecurityException("Saini ya APK haijatambuliwa. Update imezuiwa.");
        }
    }

    private static long copyBounded(InputStream input, File file, long maxBytes) throws Exception {
        long total = 0;
        try (InputStream source = input; OutputStream output = new java.io.FileOutputStream(file)) {
            byte[] buffer = new byte[16 * 1024];
            int count;
            while ((count = source.read(buffer)) != -1) {
                total += count;
                if (total > maxBytes) throw new IllegalStateException("APK imezidi ukubwa unaoruhusiwa.");
                output.write(buffer, 0, count);
            }
        }
        return total;
    }

    private static String readAll(InputStream input, int maxBytes) throws Exception {
        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        try (InputStream source = input) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = source.read(buffer)) != -1) {
                if (output.size() + count > maxBytes) throw new IllegalStateException("Jibu la release ni kubwa mno.");
                output.write(buffer, 0, count);
            }
        }
        return output.toString(java.nio.charset.StandardCharsets.UTF_8.name());
    }

    private static String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) result.append(String.format(java.util.Locale.ROOT, "%02x", value & 0xff));
        return result.toString();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
