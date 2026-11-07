package tz.mengidata.app;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public final class WebActivity extends Activity {
    private static final int REQUEST_SAVE_FILE = 4101;
    private static final int REQUEST_OPEN_FILE = 4102;
    private static final String APP_ORIGIN = "https://appassets.androidplatform.net";
    private static final String APP_URL = APP_ORIGIN + "/assets/www/index.html";

    private WebView webView;
    private ValueCallback<Uri[]> fileChooserCallback;
    private byte[] pendingFile;
    private boolean appReady;
    private boolean locked;
    private boolean autoUpdateCheckStarted;
    private volatile ReleaseUpdater.Update availableUpdate;
    private volatile JSONObject updateStatus = new JSONObject();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Ui.applyTheme(this);
        configureSystemBars();
        if (AppLock.isEnabled(this)) {
            showPinScreen();
        } else {
            openApp();
        }
    }

    private void configureSystemBars() {
        int systemBarColor = Ui.isDark() ? 0xFF071A14 : 0xFF0C2B22;
        getWindow().setStatusBarColor(systemBarColor);
        getWindow().setNavigationBarColor(systemBarColor);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getWindow().getDecorView().setSystemUiVisibility(0);
        }
    }

    private void openApp() {
        locked = false;
        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(243, 239, 229));
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setTextZoom(Math.round(getResources().getConfiguration().fontScale * 100f));
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setSupportMultipleWindows(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settings.setSafeBrowsingEnabled(true);
        }
        webView.addJavascriptInterface(new NativeBridge(), "MengiNative");
        webView.setWebViewClient(new LocalAssetClient());
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                             FileChooserParams params) {
                if (fileChooserCallback != null) fileChooserCallback.onReceiveValue(null);
                fileChooserCallback = callback;
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                try {
                    startActivityForResult(intent, REQUEST_OPEN_FILE);
                    return true;
                } catch (android.content.ActivityNotFoundException e) {
                    fileChooserCallback = null;
                    callback.onReceiveValue(null);
                    return false;
                }
            }
        });
        setContentView(webView, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        webView.loadUrl(APP_URL);
    }

    private final class LocalAssetClient extends WebViewClient {
        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            if (!autoUpdateCheckStarted && APP_URL.equals(url)
                    && ReleaseUpdater.shouldCheckOnLaunch(WebActivity.this)) {
                autoUpdateCheckStarted = true;
                new NativeBridge().checkForLatestUpdate();
            }
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            Uri uri = request.getUrl();
            if (!APP_ORIGIN.equals(uri.getScheme() + "://" + uri.getAuthority())) {
                return new WebResourceResponse("text/plain", "UTF-8",
                        new ByteArrayInputStream(new byte[0]));
            }
            String path = uri.getPath();
            if (path == null || !path.startsWith("/assets/www/")) {
                return new WebResourceResponse("text/plain", "UTF-8",
                        new ByteArrayInputStream(new byte[0]));
            }
            String assetPath = path.substring("/assets/".length());
            try {
                InputStream stream = getAssets().open(assetPath);
                return new WebResourceResponse(mimeType(assetPath), "UTF-8", stream);
            } catch (FileNotFoundException e) {
                return new WebResourceResponse("text/plain", "UTF-8",
                        new ByteArrayInputStream("Asset not found".getBytes(StandardCharsets.UTF_8)));
            } catch (IOException e) {
                return new WebResourceResponse("text/plain", "UTF-8",
                        new ByteArrayInputStream("Unable to load app asset".getBytes(StandardCharsets.UTF_8)));
            }
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            return !APP_ORIGIN.equals(request.getUrl().getScheme() + "://" + request.getUrl().getAuthority());
        }
    }

    private String mimeType(String path) {
        String lower = path.toLowerCase(java.util.Locale.ROOT);
        if (lower.endsWith(".html")) return "text/html";
        if (lower.endsWith(".js")) return "text/javascript";
        if (lower.endsWith(".css")) return "text/css";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".woff2")) return "font/woff2";
        if (lower.endsWith(".woff")) return "font/woff";
        if (lower.endsWith(".ttf")) return "font/ttf";
        if (lower.endsWith(".json")) return "application/json";
        return "application/octet-stream";
    }

    private void showPinScreen() {
        locked = true;
        LinearLayout panel = Ui.column(this);
        panel.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        Ui.pad(panel, this, 24, 28, 24, 28);
        panel.setBackground(Ui.rounded(this, Ui.CARD, 20, Ui.LINE, 1));

        TextView title = Ui.display(this, "Weka PIN kufungua app", 22, Ui.INK);
        title.setGravity(android.view.Gravity.CENTER);
        panel.addView(title, Ui.matchWrap());
        EditText pin = Ui.number(this, "PIN yako");
        pin.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        pin.setGravity(android.view.Gravity.CENTER);
        pin.setTextSize(22);
        LinearLayout.LayoutParams pinParams = Ui.matchWrap();
        pinParams.topMargin = Ui.dp(this, 18);
        panel.addView(pin, pinParams);
        TextView feedback = Ui.hint(this, "Ingiza PIN yako ili kuendelea.");
        feedback.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams feedbackParams = Ui.matchWrap();
        feedbackParams.topMargin = Ui.dp(this, 10);
        panel.addView(feedback, feedbackParams);
        TextView unlock = Ui.accentButton(this, "Fungua app");
        LinearLayout.LayoutParams unlockParams = Ui.matchWrap();
        unlockParams.topMargin = Ui.dp(this, 16);
        panel.addView(unlock, unlockParams);

        Runnable submit = () -> {
            try {
                if (AppLock.verify(this, pin.getText().toString())) {
                    openApp();
                } else {
                    feedback.setText("PIN si sahihi au muda wa kusubiri bado haujaisha.");
                    pin.setText("");
                }
            } catch (java.security.GeneralSecurityException e) {
                feedback.setText("Hitilafu ya usalama. Funga na ufungue app tena.");
            }
        };
        unlock.setOnClickListener(v -> submit.run());
        pin.setOnEditorActionListener((view, actionId, event) -> {
            submit.run();
            return true;
        });
        setContentView(panel, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!locked && AppLock.isEnabled(this)) showPinScreen();
    }

    @Override
    protected void onResume() {
        super.onResume();
        notifyWebSystemSettings();
        ReleaseUpdater.Update update = availableUpdate;
        if (update != null && "permission-required".equals(updateStatus.optString("state"))
                && ReleaseUpdater.canInstallPackages(this)) {
            new NativeBridge().publishUpdateStatus("available", update.version, update.notes, null);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Ui.applyTheme(this);
        configureSystemBars();
        if (webView != null) {
            webView.getSettings().setTextZoom(
                    Math.round(getResources().getConfiguration().fontScale * 100f));
            webView.setBackgroundColor(Ui.BG);
            notifyWebSystemSettings();
        }
    }

    private void notifyWebSystemSettings() {
        if (webView == null) return;
        String script = "window.dispatchEvent(new CustomEvent('mengi-system-theme-change',"
                + "{detail:{dark:" + Ui.isSystemDark(this)
                + ",animationsEnabled:" + Ui.animationsEnabled(this) + "}}));";
        webView.evaluateJavascript(script, null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OPEN_FILE) {
            if (fileChooserCallback != null) {
                Uri[] result = resultCode == RESULT_OK && data != null && data.getData() != null
                        ? new Uri[]{data.getData()} : null;
                fileChooserCallback.onReceiveValue(result);
                fileChooserCallback = null;
            }
            return;
        }
        if (requestCode != REQUEST_SAVE_FILE) return;
        if (resultCode == RESULT_OK && data != null && data.getData() != null && pendingFile != null) {
            try (OutputStream output = getContentResolver().openOutputStream(data.getData())) {
                if (output == null) throw new IOException("Imeshindwa kufungua faili.");
                output.write(pendingFile);
                runOnUiThread(() -> android.widget.Toast.makeText(this,
                        "Faili limehifadhiwa.", android.widget.Toast.LENGTH_SHORT).show());
            } catch (IOException e) {
                android.util.Log.e("MengiData", "Imeshindwa kuhifadhi faili", e);
                runOnUiThread(() -> android.widget.Toast.makeText(this,
                        "Imeshindwa kuhifadhi faili.", android.widget.Toast.LENGTH_LONG).show());
            }
        }
        pendingFile = null;
    }

    @Override
    public void onBackPressed() {
        if (webView != null) {
            webView.evaluateJavascript(
                    "(function(){return typeof window.__androidBack==='function' ? window.__androidBack() : false})()",
                    result -> {
                        if (!"true".equals(result)) finish();
                    });
            return;
        }
        super.onBackPressed();
    }

    public final class NativeBridge {
        @JavascriptInterface
        public boolean isNativeApp() {
            return true;
        }

        @JavascriptInterface
        public void appReady() {
            appReady = true;
        }

        @JavascriptInterface
        public String getVersion() {
            return BuildConfig.VERSION_NAME;
        }

        @JavascriptInterface
        public String getSystemTheme() {
            return Ui.isSystemDark(WebActivity.this) ? "dark" : "light";
        }

        @JavascriptInterface
        public boolean animationsEnabled() {
            return Ui.animationsEnabled(WebActivity.this);
        }

        @JavascriptInterface
        public void setThemeMode(String mode) {
            if (!"system".equals(mode) && !"light".equals(mode) && !"dark".equals(mode)) {
                android.util.Log.w("MengiData", "Chaguo la mandhari si sahihi");
                return;
            }
            runOnUiThread(() -> {
                Ui.saveThemeMode(WebActivity.this, mode);
                Ui.applyTheme(WebActivity.this);
                configureSystemBars();
                if (webView != null) webView.setBackgroundColor(Ui.BG);
            });
        }

        @JavascriptInterface
        public boolean getAutoUpdatesEnabled() {
            return ReleaseUpdater.autoUpdatesEnabled(WebActivity.this);
        }

        @JavascriptInterface
        public void setAutoUpdatesEnabled(boolean enabled) {
            ReleaseUpdater.setAutoUpdatesEnabled(WebActivity.this, enabled);
        }

        @JavascriptInterface
        public void checkForUpdate() {
            checkForLatestUpdate();
        }

        @JavascriptInterface
        public String getUpdateStatus() {
            return updateStatus.toString();
        }

        @JavascriptInterface
        public void downloadAndInstallUpdate() {
            runOnUiThread(() -> downloadAndInstallUpdateInternal());
        }

        @JavascriptInterface
        public void openUpdateInstallSettings() {
            runOnUiThread(() -> {
                try {
                    ReleaseUpdater.openInstallPermissionSettings(WebActivity.this);
                } catch (RuntimeException error) {
                    android.util.Log.e("MengiData", "Imeshindwa kufungua ruhusa ya kusakinisha update", error);
                    publishUpdateStatus("error", null, null, "Imeshindwa kufungua mipangilio ya ruhusa.");
                }
            });
        }

        private void checkForLatestUpdate() {
            publishUpdateStatus("checking", null, null, null);
            ReleaseUpdater.check(WebActivity.this, (update, error) -> {
                availableUpdate = update;
                if (update != null) {
                    publishUpdateStatus("available", update.version, update.notes, null);
                } else if (error != null && error.startsWith("Unatumia toleo la hivi karibuni:")) {
                    publishUpdateStatus("up-to-date", null, null, error);
                } else {
                    publishUpdateStatus("error", null, null,
                            error == null ? "Imeshindwa kuangalia update." : error);
                }
            });
        }

        private void downloadAndInstallUpdateInternal() {
            ReleaseUpdater.Update update = availableUpdate;
            if (update == null) {
                publishUpdateStatus("error", null, null, "Angalia update tena kabla ya kuipakua.");
                return;
            }
            if (!ReleaseUpdater.canInstallPackages(WebActivity.this)) {
                publishUpdateStatus("permission-required", update.version, null,
                        "Ruhusu kusakinisha app kutoka chanzo hiki ili kuendelea.");
                return;
            }
            publishUpdateStatus("downloading", update.version, null, null);
            ReleaseUpdater.download(WebActivity.this, update, (apk, error) -> {
                if (apk == null) {
                    publishUpdateStatus("error", update.version, null,
                            error == null ? "Imeshindwa kupakua update." : error);
                    return;
                }
                ReleaseUpdater.install(WebActivity.this, apk, installError -> {
                    apk.delete();
                    if (installError != null) {
                        publishUpdateStatus("error", update.version, null, installError);
                    } else {
                        publishUpdateStatus("installing", update.version, null,
                                "Thibitisha dirisha la Android ili kukamilisha usakinishaji.");
                    }
                });
            });
        }

        private void publishUpdateStatus(String state, String version, String notes, String message) {
            JSONObject status = new JSONObject();
            try {
                status.put("state", state);
                if (version != null) status.put("version", version);
                if (notes != null) status.put("notes", notes);
                if (message != null) status.put("message", message);
            } catch (org.json.JSONException error) {
                android.util.Log.e("MengiData", "Imeshindwa kuunda hali ya update", error);
                return;
            }
            updateStatus = status;
            runOnUiThread(() -> {
                if (webView != null) {
                    webView.evaluateJavascript(
                            "window.dispatchEvent(new CustomEvent('mengi-update-status',{detail:"
                                    + status.toString() + "}))", null);
                }
            });
        }

        @JavascriptInterface
        public String getLegacyData() {
            try {
                Db db = Db.get(WebActivity.this);
                db.migrateBrandSettings();
                return db.exportJson();
            } catch (RuntimeException e) {
                android.util.Log.e("MengiData", "Imeshindwa kuleta data ya Android iliyopo", e);
                return "";
            }
        }

        @JavascriptInterface
        public void saveBase64(String filename, String mime, String base64) {
            try {
                byte[] bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT);
                runOnUiThread(() -> requestSaveFile(filename, mime, bytes));
            } catch (IllegalArgumentException e) {
                android.util.Log.e("MengiData", "Maudhui ya faili si sahihi", e);
                runOnUiThread(() -> android.widget.Toast.makeText(WebActivity.this,
                        "Faili si sahihi; jaribu tena.", android.widget.Toast.LENGTH_LONG).show());
            }
        }

        @JavascriptInterface
        public void printHtml(String html, String title) {
            runOnUiThread(() -> printDocument(html, title));
        }

        @JavascriptInterface
        public void setStatusBarColor(String hex) {
            try {
                int color = Color.parseColor(hex);
                runOnUiThread(() -> getWindow().setStatusBarColor(color));
            } catch (IllegalArgumentException e) {
                android.util.Log.w("MengiData", "Rangi ya status bar si sahihi", e);
            }
        }

        @JavascriptInterface
        public void exitApp() {
            runOnUiThread(WebActivity.this::finish);
        }
    }

    private void requestSaveFile(String filename, String mime, byte[] bytes) {
        pendingFile = bytes;
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mime == null || mime.isEmpty() ? "application/octet-stream" : mime);
        intent.putExtra(Intent.EXTRA_TITLE, filename);
        try {
            startActivityForResult(intent, REQUEST_SAVE_FILE);
        } catch (android.content.ActivityNotFoundException e) {
            pendingFile = null;
            android.util.Log.e("MengiData", "Hakuna faili manager ya kuhifadhi faili", e);
            android.widget.Toast.makeText(this, "Hakuna programu ya kuhifadhi faili.", android.widget.Toast.LENGTH_LONG).show();
        }
    }

    private void printDocument(String html, String title) {
        WebView printView = new WebView(this);
        printView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                PrintManager manager = (PrintManager) getSystemService(PRINT_SERVICE);
                if (manager == null) {
                    android.util.Log.e("MengiData", "Huduma ya kuchapisha haipatikani");
                    view.destroy();
                    return;
                }
                manager.print(title, view.createPrintDocumentAdapter(title),
                        new PrintAttributes.Builder().build());
            }
        });
        printView.loadDataWithBaseURL(APP_URL, html, "text/HTML", "UTF-8", null);
    }
}
