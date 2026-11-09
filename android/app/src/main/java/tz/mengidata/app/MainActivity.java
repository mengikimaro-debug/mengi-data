package tz.mengidata.app;

import android.app.Activity;
import android.content.Context;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.text.InputType;

import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Native single-activity shell: custom top bar, bottom navigation and lazily
 * created native pages. Absolutely no WebView is used anywhere in this app.
 */
public class MainActivity extends Activity {

    private static final int REQUEST_SAVE_FILE = 2001;
    private static final int REQUEST_IMPORT_BACKUP = 2002;
    static final String[] TABS = {"dashboard", "transactions", "invoices", "bills", "saver", "reports", "notebook"};
    static final String[] TAB_LABELS = {"Dashibodi", "Miamala", "Ankara", "Bili", "Akiba", "Ripoti", "Daftari"};

    private Db db;
    private FrameLayout content;
    private TextView pageTitle;
    private TextView pageSubtitle;
    private ImageView addButton;
    private LinearLayout navBar;
    private final Map<String, Page> pages = new LinkedHashMap<>();
    private final Map<String, LinearLayout> tabViews = new LinkedHashMap<>();
    private String currentKey = "dashboard";
    private byte[] pendingFile;
    private boolean pendingShare;
    private boolean appLocked;
    private final Handler lockHandler = new Handler(Looper.getMainLooper());
    private Runnable lockCountdown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = Db.get(this);
        if (!getPreferences(MODE_PRIVATE).getBoolean("home_satellite_brand_migrated", false)) {
            db.migrateBrandSettings();
            getPreferences(MODE_PRIVATE).edit().putBoolean("home_satellite_brand_migrated", true).apply();
        }
        Ui.applyTheme(this);
        configureWindow();
        updateSecureWindow();
        if (AppLock.isEnabled(this)) {
            showLockScreen();
        } else {
            openApp();
        }
    }

    private void openApp() {
        appLocked = false;
        buildShell();
        showPage(currentKey);
        BillReminderReceiver.scheduleAll(this);
        if (ReleaseUpdater.shouldCheckOnLaunch(this)) checkForUpdates(true);
    }

    void checkForUpdates(boolean automatic) {
        ReleaseUpdater.check(this, (update, error) -> {
            if (isFinishing()) return;
            if (update == null) {
                if (!automatic && error != null) {
                    Ui.alert(this, "Updates", error);
                }
                return;
            }
            String message = "Toleo jipya " + update.version + " linapatikana."
                    + (update.notes.isEmpty() ? "" : "\n\n" + update.notes);
            Ui.dialog(this).setTitle("Update ya Mengidata")
                    .setMessage(message)
                    .setNegativeButton("Baadaye", null)
                    .setPositiveButton("Pakua na sakinisha", (dialog, which) -> downloadUpdate(update))
                    .show();
        });
    }

    private void downloadUpdate(ReleaseUpdater.Update update) {
        if (!ReleaseUpdater.canInstallPackages(this)) {
            Ui.dialog(this).setTitle("Ruhusu kusakinisha update")
                    .setMessage("Washa ruhusa ya kusakinisha app kutoka chanzo hiki, kisha rudi Mipangilio na ujaribu tena.")
                    .setNegativeButton("Ghairi", null)
                    .setPositiveButton("Fungua ruhusa", (dialog, which) ->
                            ReleaseUpdater.openInstallPermissionSettings(this))
                    .show();
            return;
        }
        ReleaseUpdater.download(this, update, (apk, error) -> {
            if (isFinishing()) return;
            if (apk == null) {
                Ui.alert(this, "Imeshindwa kupakua update", error == null ? "Jaribu tena." : error);
                return;
            }
            ReleaseUpdater.install(this, apk, installError -> {
                if (installError != null) {
                    apk.delete();
                    Ui.alert(this, "Imeshindwa kuanzisha usakinishaji", installError);
                } else {
                    Ui.toast(this, "APK imepakuliwa. Thibitisha dirisha la Android kusakinisha.");
                    apk.delete();
                }
            });
        });
    }

    void updateSecureWindow() {
        if (AppLock.isEnabled(this)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
    }

    private void showLockScreen() {
        appLocked = true;
        pages.clear();
        tabViews.clear();
        updateSecureWindow();

        LinearLayout panel = Ui.column(this);
        panel.setGravity(Gravity.CENTER_HORIZONTAL);
        panel.setPadding(Ui.dp(this, 24), Ui.dp(this, 28), Ui.dp(this, 24), Ui.dp(this, 28));
        panel.setBackground(Ui.rounded(this, Ui.CARD, 20, Ui.LINE, 1));

        TextView brand = Ui.text(this, "Mengidata", 12, Ui.ACCENT, true);
        brand.setGravity(Gravity.CENTER);
        panel.addView(brand, Ui.matchWrap());
        TextView title = Ui.display(this, "Weka PIN kufungua app", 22, Ui.INK);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleParams = Ui.matchWrap();
        titleParams.topMargin = Ui.dp(this, 14);
        panel.addView(title, titleParams);

        EditText pin = Ui.number(this, "PIN yako");
        pin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        pin.setGravity(Gravity.CENTER);
        pin.setTextSize(22);
        LinearLayout.LayoutParams pinParams = Ui.matchWrap();
        pinParams.topMargin = Ui.dp(this, 18);
        panel.addView(pin, pinParams);

        TextView feedback = Ui.hint(this, "Ingiza PIN yako ili kuendelea.");
        feedback.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams feedbackParams = Ui.matchWrap();
        feedbackParams.topMargin = Ui.dp(this, 10);
        panel.addView(feedback, feedbackParams);

        TextView unlock = Ui.accentButton(this, "Fungua app");
        LinearLayout.LayoutParams unlockParams = Ui.matchWrap();
        unlockParams.topMargin = Ui.dp(this, 16);
        panel.addView(unlock, unlockParams);

        Runnable submit = () -> {
            if (AppLock.remainingLockoutMillis(this) > 0) {
                updateLockFeedback(feedback, unlock);
                return;
            }
            String entered = pin.getText().toString();
            pin.setText("");
            try {
                if (AppLock.verify(this, entered)) {
                    cancelLockCountdown();
                    openApp();
                } else {
                    feedback.setText(LocaleText.tr(this, AppLock.remainingLockoutMillis(this) > 0
                            ? "PIN si sahihi. Jaribu tena baada ya muda."
                            : "PIN si sahihi. Jaribu tena."));
                    updateLockFeedback(feedback, unlock);
                    pin.requestFocus();
                }
            } catch (GeneralSecurityException e) {
                Ui.alert(this, "Hitilafu ya usalama", "Imeshindwa kuthibitisha PIN. Funga na ufungue app tena.");
            }
        };
        unlock.setOnClickListener(v -> submit.run());
        pin.setOnEditorActionListener((view, actionId, event) -> {
            submit.run();
            return true;
        });

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Ui.BG);
        root.addView(panel, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        setContentView(root);
        updateLockFeedback(feedback, unlock);
    }

    private void updateLockFeedback(TextView feedback, TextView unlock) {
        cancelLockCountdown();
        long remaining = AppLock.remainingLockoutMillis(this);
        if (remaining <= 0) {
            feedback.setText(LocaleText.tr(this, "Ingiza PIN yako ili kuendelea."));
            unlock.setEnabled(true);
            unlock.setAlpha(1f);
            return;
        }
        long seconds = (remaining + 999) / 1000;
        feedback.setText(LocaleText.isEnglish()
                ? "Wait " + seconds + " seconds before trying again."
                : "Subiri sekunde " + seconds + " kabla ya kujaribu tena.");
        unlock.setEnabled(false);
        unlock.setAlpha(0.5f);
        lockCountdown = () -> updateLockFeedback(feedback, unlock);
        lockHandler.postDelayed(lockCountdown, Math.min(remaining, 1000));
    }

    private void cancelLockCountdown() {
        if (lockCountdown != null) {
            lockHandler.removeCallbacks(lockCountdown);
            lockCountdown = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!appLocked && AppLock.isEnabled(this)) showLockScreen();
    }

    private void configureWindow() {
        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        w.setStatusBarColor(Ui.BRAND);
        w.setNavigationBarColor(Ui.BRAND);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            w.getDecorView().setSystemUiVisibility(0);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Ui.applyTheme(this)) {
            configureWindow();
            if (appLocked) {
                showLockScreen();
            } else if (content != null) {
                rebuildShell(true);
            }
        }
    }

    void setThemeMode(String mode) {
        Ui.saveThemeMode(this, mode);
        Ui.applyTheme(this);
        configureWindow();
        rebuildShell(true);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (Ui.applyTheme(this)) {
            configureWindow();
            if (appLocked) showLockScreen();
            else if (content != null) rebuildShell(true);
        }
    }

    private void rebuildShell(boolean animate) {
        pages.clear();
        tabViews.clear();
        buildShell(animate);
        showPage(currentKey);
    }

    private void buildShell() {
        buildShell(false);
    }

    private void buildShell(boolean animate) {
        LinearLayout root = Ui.column(this);
        root.setBackgroundColor(Ui.BG);
        root.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        root.addView(buildTopBar(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 78)));

        content = new FrameLayout(this);
        content.setBackgroundColor(Ui.BG);
        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        root.addView(content, contentParams);

        root.addView(buildBottomNav(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 66)));

        setContentView(root);
        if (animate && Ui.animationsEnabled(this)) {
            root.setAlpha(0f);
            root.animate().alpha(1f).setDuration(180).start();
        }
    }

    private View buildTopBar() {
        LinearLayout bar = Ui.row(this);
        bar.setBackgroundColor(Ui.BRAND);
        Ui.pad(bar, this, 16, 10, 10, 10);

        LinearLayout titles = Ui.column(this);
        TextView brand = Ui.text(this, "Mengidata", 9, Ui.SILVER, true);
        brand.setLetterSpacing(0.22f);
        titles.addView(brand);

        pageTitle = Ui.display(this, "Dashibodi", 21, Color.WHITE);
        titles.addView(pageTitle);

        pageSubtitle = Ui.text(this, "Muhtasari wa biashara", 10, 0xFFB9D0C1, false);
        titles.addView(pageSubtitle);

        bar.addView(titles, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        addButton = new ImageView(this);
        addButton.setImageResource(R.drawable.ic_plus);
        addButton.setColorFilter(Ui.isDark() ? Ui.ACCENT : Ui.BRAND);
        addButton.setPadding(Ui.dp(this, 9), Ui.dp(this, 9), Ui.dp(this, 9), Ui.dp(this, 9));
        addButton.setBackground(Ui.rounded(this, Ui.SILVER, 12, 0, 0));
        addButton.setClickable(true);
        addButton.setOnClickListener(v -> {
            Page page = pages.get(currentKey);
            if (page != null) page.onAdd();
        });
        LinearLayout.LayoutParams addParams = new LinearLayout.LayoutParams(Ui.dp(this, 40), Ui.dp(this, 40));
        addParams.setMargins(0, 0, Ui.dp(this, 6), 0);
        bar.addView(addButton, addParams);

        ImageView settings = new ImageView(this);
        settings.setImageResource(R.drawable.ic_settings);
        settings.setColorFilter(Color.WHITE);
        settings.setPadding(Ui.dp(this, 9), Ui.dp(this, 9), Ui.dp(this, 9), Ui.dp(this, 9));
        settings.setClickable(true);
        settings.setOnClickListener(v -> showPage("settings"));
        bar.addView(settings, new LinearLayout.LayoutParams(Ui.dp(this, 40), Ui.dp(this, 40)));

        TextView language = Ui.button(this, LocaleText.isEnglish(this) ? "SW" : "EN", false);
        language.setTextColor(Color.WHITE);
        language.setBackground(Ui.rounded(this, Ui.BRAND_2, 10, 0xFF507164, 1));
        language.setContentDescription(LocaleText.isEnglish(this) ? "Badili lugha kuwa Kiswahili" : "Switch language to English");
        language.setOnClickListener(v -> {
            LocaleText.toggle(this);
            rebuildShell(true);
        });
        LinearLayout.LayoutParams languageParams = new LinearLayout.LayoutParams(Ui.dp(this, 42), Ui.dp(this, 40));
        languageParams.setMargins(Ui.dp(this, 5), 0, 0, 0);
        bar.addView(language, languageParams);
        return bar;
    }

    private View buildBottomNav() {
        navBar = Ui.row(this);
        navBar.setBackgroundColor(Ui.CARD);
        navBar.setElevation(Ui.dp(this, 12));
        for (int i = 0; i < TABS.length; i++) {
            final String key = TABS[i];
            LinearLayout item = Ui.column(this);
            item.setGravity(Gravity.CENTER);
            item.setPadding(0, Ui.dp(this, 5), 0, Ui.dp(this, 3));
            item.setClickable(true);
            item.setOnClickListener(v -> showPage(key));

            ImageView icon = Ui.icon(this, iconFor(key), Ui.MUTED, 22);
            item.addView(icon);
            TextView label = Ui.text(this, TAB_LABELS[i], 9, Ui.MUTED, true);
            label.setGravity(Gravity.CENTER);
            item.addView(label);

            navBar.addView(item, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
            tabViews.put(key, item);
        }
        return navBar;
    }

    private int iconFor(String key) {
        switch (key) {
            case "transactions": return R.drawable.ic_swap;
            case "invoices": return R.drawable.ic_file;
            case "bills": return R.drawable.ic_receipt;
            case "saver": return R.drawable.ic_saver;
            case "reports": return R.drawable.ic_chart;
            case "notebook": return R.drawable.ic_note;
            default: return R.drawable.ic_home;
        }
    }

    private Page pageFor(String key) {
        Page page = pages.get(key);
        if (page != null) return page;
        switch (key) {
            case "transactions": page = new TransactionsPage(this, db); break;
            case "invoices": page = new InvoicesPage(this, db); break;
            case "bills": page = new BillsPage(this, db); break;
            case "saver": page = new SaverPage(this, db); break;
            case "reports": page = new ReportsPage(this, db); break;
            case "notebook": page = new NotebookPage(this, db); break;
            case "settings": page = new SettingsPage(this, db); break;
            default: page = new DashboardPage(this, db); break;
        }
        pages.put(key, page);
        return page;
    }

    void showPage(String key) {
        String previousKey = currentKey;
        boolean hadPage = content.getChildCount() > 0;
        currentKey = key;
        Page page = pageFor(key);
        content.removeAllViews();
        View pageRoot = page.root();
        content.addView(pageRoot, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        if (hadPage && !previousKey.equals(key) && Ui.animationsEnabled(this)) {
            pageRoot.setAlpha(0f);
            pageRoot.setTranslationY(Ui.dp(this, 8));
            pageRoot.animate().alpha(1f).translationY(0f).setDuration(180).start();
        }
        pageTitle.setText(LocaleText.tr(this, page.title()));
        pageSubtitle.setText(LocaleText.tr(this, page.subtitle()));
        addButton.setVisibility("settings".equals(key) || "reports".equals(key) ? View.GONE : View.VISIBLE);
        updateTabs();
        page.refresh();
    }

    private void updateTabs() {
        for (Map.Entry<String, LinearLayout> entry : tabViews.entrySet()) {
            boolean active = entry.getKey().equals(currentKey);
            LinearLayout row = entry.getValue();
            row.setBackground(active ? Ui.rounded(this, Ui.ACTIVE, 12, 0, 0) : null);
            ImageView icon = (ImageView) row.getChildAt(0);
            TextView label = (TextView) row.getChildAt(1);
            int activeColor = Ui.isDark() ? Ui.ACCENT : Ui.BRAND;
            icon.setColorFilter(active ? activeColor : Ui.MUTED);
            label.setTextColor(active ? activeColor : Ui.MUTED);
            if (Ui.animationsEnabled(this)) {
                row.animate().alpha(active ? 1f : 0.88f).setDuration(150).start();
            } else {
                row.setAlpha(active ? 1f : 0.88f);
            }
        }
    }

    void reloadAll() {
        for (Page page : pages.values()) page.refresh();
        Page current = pages.get(currentKey);
        if (current != null) current.refresh();
        BillReminderReceiver.scheduleAll(this);
    }

    void saveFile(String name, String mime, byte[] bytes) {
        pendingFile = bytes;
        pendingShare = false;
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mime);
        intent.putExtra(Intent.EXTRA_TITLE, name);
        startActivityForResult(intent, REQUEST_SAVE_FILE);
    }

    void saveAndShareFile(String name, String mime, byte[] bytes) {
        pendingFile = bytes;
        pendingShare = true;
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mime);
        intent.putExtra(Intent.EXTRA_TITLE, name);
        startActivityForResult(intent, REQUEST_SAVE_FILE);
    }

    void toggleBillReminders() {
        if (BillReminderReceiver.isEnabled(this)) {
            BillReminderReceiver.setEnabled(this, false);
            reloadAll();
            Ui.toast(this, "Vikumbusho vya bili vimezimwa.");
            return;
        }
        BillReminderReceiver.setEnabled(this, true);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 3001);
        } else {
            reloadAll();
            Ui.toast(this, "Vikumbusho vya bili vimewashwa.");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != 3001) return;
        boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        BillReminderReceiver.setEnabled(this, granted);
        reloadAll();
        if (granted) Ui.toast(this, "Vikumbusho vya bili vimewashwa.");
        else Ui.alert(this, "Arifa hazijaruhusiwa", "Ruhusu arifa kwenye mipangilio ya simu ili upokee vikumbusho vya bili.");
    }

    void chooseBackup() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        startActivityForResult(intent, REQUEST_IMPORT_BACKUP);
    }

    void printPdf(String title, byte[] pdf) {
        PrintManager manager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
        if (manager == null) {
            Ui.alert(this, "Chapishaji haupatikani", "Hifadhi ankara kama PDF badala yake.");
            return;
        }
        manager.print(title, new PrintDocumentAdapter() {
            @Override
            public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes,
                                 CancellationSignal cancellationSignal, LayoutResultCallback callback,
                                 android.os.Bundle extras) {
                if (cancellationSignal.isCanceled()) {
                    callback.onLayoutCancelled();
                    return;
                }
                PrintDocumentInfo info = new PrintDocumentInfo.Builder(title + ".pdf")
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .setPageCount(1).build();
                callback.onLayoutFinished(info, true);
            }

            @Override
            public void onWrite(PageRange[] pages, ParcelFileDescriptor destination,
                                CancellationSignal cancellationSignal, WriteResultCallback callback) {
                try (FileOutputStream stream = new FileOutputStream(destination.getFileDescriptor())) {
                    if (cancellationSignal.isCanceled()) {
                        callback.onWriteCancelled();
                        return;
                    }
                    stream.write(pdf);
                    callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});
                } catch (IOException e) {
                    callback.onWriteFailed(e.getMessage());
                }
            }
        }, null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            pendingFile = null;
            pendingShare = false;
            return;
        }
        Uri uri = data.getData();
        if (requestCode == REQUEST_SAVE_FILE) {
            if (pendingFile == null) {
                pendingShare = false;
                return;
            }
            boolean share = pendingShare;
            boolean saved = false;
            try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                if (out == null) throw new IOException("Imeshindwa kufungua faili.");
                out.write(pendingFile);
                saved = true;
            } catch (IOException e) {
                Ui.alert(this, "Imeshindwa kuhifadhi faili", e.getMessage() == null ? "Jaribu tena." : e.getMessage());
            } finally {
                pendingFile = null;
                pendingShare = false;
            }
            if (saved && share) {
                Intent send = new Intent(Intent.ACTION_SEND);
                send.setType("application/pdf");
                send.putExtra(Intent.EXTRA_STREAM, uri);
                send.setClipData(ClipData.newRawUri("invoice", uri));
                send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                Intent chooser = Intent.createChooser(send, LocaleText.tr(this, "Kushiriki ankara"));
                chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(chooser);
            } else if (saved) {
                Ui.toast(this, "Faili limehifadhiwa.");
            }
        } else if (requestCode == REQUEST_IMPORT_BACKUP) {
            try (InputStream in = getContentResolver().openInputStream(uri);
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                if (in == null) throw new IOException("Imeshindwa kusoma faili.");
                byte[] buffer = new byte[8192];
                int count;
                while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
                String json = new String(out.toByteArray(), StandardCharsets.UTF_8);
                Ui.dialog(this)
                        .setTitle("Rejesha nakala rudufu?")
                        .setMessage("Data za sasa zitabadilishwa na data ya faili hili.")
                        .setNegativeButton("Ghairi", null)
                        .setPositiveButton("Rejesha", (dialog, which) -> {
                            try {
                                db.importJson(json);
                                reloadAll();
                                Ui.toast(this, "Data imerejeshwa.");
                            } catch (JSONException e) {
                                Ui.alert(this, "Faili si sahihi", "Chagua nakala rudufu sahihi ya Mengidata.");
                            }
                        }).show();
            } catch (IOException e) {
                Ui.alert(this, "Imeshindwa kusoma faili", e.getMessage() == null ? "Jaribu tena." : e.getMessage());
            }
        }
    }

    @Override
    public void onBackPressed() {
        Page page = pages.get(currentKey);
        if (page != null && page.onBack()) return;
        if (!"dashboard".equals(currentKey)) {
            showPage("dashboard");
            return;
        }
        super.onBackPressed();
    }
}
