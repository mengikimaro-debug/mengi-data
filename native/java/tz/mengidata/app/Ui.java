package tz.mengidata.app;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.provider.Settings;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextThemeWrapper;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

/** Tiny styling kit for the native screens (no AppCompat/Material dependency). */
final class Ui {

    static int BG = 0xFFF1EFF4;
    static int CARD = 0xFFFCFBFD;
    static int INK = 0xFF211A2B;
    static int MUTED = 0xFF746E7D;
    static int FAINT = 0xFFA19BA8;
    static int BRAND = 0xFF261834;
    static int BRAND_2 = 0xFF39254F;
    static int SILVER = 0xFFD7D2DF;
    static int LINE = 0xFFDFDAE5;
    static int ROSE = 0xFFBD5A3D;
    static int ROSE_DEEP = 0xFF9E432B;
    static int AMBER = 0xFFC58A21;
    static int ACCENT = 0xFF5A3B78;
    static int ACTIVE = 0xFFEFECF4;
    static int SOFT_ROSE = 0xFFF7E4DB;
    static int SOFT_ACCENT = 0xFFEFECF4;
    private static boolean dark;

    private Ui() {}

    static String themeMode(Context context) {
        return context.getSharedPreferences("mengi_data_preferences", Context.MODE_PRIVATE)
                .getString("theme", "system");
    }

    static void saveThemeMode(Context context, String mode) {
        context.getSharedPreferences("mengi_data_preferences", Context.MODE_PRIVATE)
                .edit().putString("theme", mode).apply();
    }

    static boolean isSystemDark(Context context) {
        return (context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    static boolean applyTheme(Context context) {
        String mode = themeMode(context);
        boolean useDark = "dark".equals(mode)
                || ("system".equals(mode) && isSystemDark(context));
        boolean changed = dark != useDark;
        dark = useDark;
        if (useDark) {
            BG = Color.BLACK;
            CARD = 0xFF0B0B0B;
            INK = 0xFFF2EDF7;
            MUTED = 0xFFB7AFC1;
            FAINT = 0xFF91899C;
            BRAND = 0xFF08060A;
            BRAND_2 = 0xFF19131F;
            SILVER = 0xFF2C2732;
            LINE = 0xFF302B36;
            ROSE = 0xFFE18469;
            ROSE_DEEP = 0xFFCF7056;
            AMBER = 0xFFE0B057;
            ACCENT = 0xFFB99ADD;
            ACTIVE = 0xFF21192B;
            SOFT_ROSE = 0xFF321D19;
            SOFT_ACCENT = 0xFF21192B;
        } else {
            BG = 0xFFF1EFF4;
            CARD = 0xFFFCFBFD;
            INK = 0xFF211A2B;
            MUTED = 0xFF746E7D;
            FAINT = 0xFFA19BA8;
            BRAND = 0xFF261834;
            BRAND_2 = 0xFF39254F;
            SILVER = 0xFFD7D2DF;
            LINE = 0xFFDFDAE5;
            ROSE = 0xFFBD5A3D;
            ROSE_DEEP = 0xFF9E432B;
            AMBER = 0xFFC58A21;
            ACCENT = 0xFF5A3B78;
            ACTIVE = 0xFFEFECF4;
            SOFT_ROSE = 0xFFF7E4DB;
            SOFT_ACCENT = 0xFFEFECF4;
        }
        return changed;
    }

    static boolean isDark() {
        return dark;
    }

    static boolean animationsEnabled(Context context) {
        return Settings.Global.getFloat(context.getContentResolver(),
                Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f;
    }

    static int dp(Context c, float value) {
        return Math.round(value * c.getResources().getDisplayMetrics().density);
    }

    static void pad(View v, Context c, int l, int t, int r, int b) {
        v.setPadding(dp(c, l), dp(c, t), dp(c, r), dp(c, b));
    }

    static void margin(View v, Context c, int l, int t, int r, int b) {
        ViewGroup.LayoutParams p = v.getLayoutParams();
        if (p instanceof ViewGroup.MarginLayoutParams) {
            ((ViewGroup.MarginLayoutParams) p).setMargins(dp(c, l), dp(c, t), dp(c, r), dp(c, b));
        }
    }

    static GradientDrawable rounded(int fill, int radiusDp, int strokeColor, int strokeDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(radiusDp);
        if (strokeColor != 0) d.setStroke(strokeDp, strokeColor);
        return d;
    }

    static GradientDrawable rounded(Context c, int fill, float radiusDp, int strokeColor, float strokeDp) {
        return rounded(fill, dp(c, radiusDp), strokeColor, dp(c, strokeDp));
    }

    static TextView text(Context c, CharSequence value, float sizeSp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(LocaleText.tr(c, value == null ? null : value.toString()));
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        t.setTextColor(color);
        t.setTypeface(Typeface.create(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL));
        return t;
    }

    static TextView display(Context c, CharSequence value, float sizeSp, int color) {
        TextView t = new TextView(c);
        t.setText(LocaleText.tr(c, value == null ? null : value.toString()));
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        t.setTextColor(color);
        t.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        return t;
    }

    static TextView label(Context c, CharSequence value) {
        TextView t = text(c, value, 9, ACCENT, true);
        t.setAllCaps(true);
        t.setLetterSpacing(0.14f);
        return t;
    }

    static LinearLayout column(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    static LinearLayout row(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    static LinearLayout card(Context c) {
        LinearLayout l = column(c);
        l.setBackground(rounded(c, CARD, 18, LINE, 1));
        l.setElevation(dp(c, 1));
        pad(l, c, 16, 14, 16, 14);
        return l;
    }

    static TextView button(Context c, String value, boolean primary) {
        TextView b = text(c, value, 12, primary ? Color.WHITE : INK, true);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(c, 14), dp(c, 9), dp(c, 14), dp(c, 9));
        GradientDrawable bg = rounded(c, primary ? BRAND : CARD, 10, primary ? 0 : LINE, 1);
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x22000000), bg, null));
        b.setClickable(true);
        b.setFocusable(true);
        return b;
    }

    static TextView accentButton(Context c, String value) {
        TextView b = text(c, value, 12, dark ? INK : BRAND, true);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(c, 14), dp(c, 9), dp(c, 14), dp(c, 9));
        GradientDrawable bg = rounded(c, SILVER, 10, 0, 0);
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x22000000), bg, null));
        b.setClickable(true);
        b.setFocusable(true);
        return b;
    }

    static TextView dangerButton(Context c, String value) {
        TextView b = text(c, value, 12, Color.WHITE, true);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(c, 14), dp(c, 9), dp(c, 14), dp(c, 9));
        GradientDrawable bg = rounded(c, ROSE, 10, 0, 0);
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x22000000), bg, null));
        b.setClickable(true);
        b.setFocusable(true);
        return b;
    }

    static EditText input(Context c, String hint) {
        EditText e = new EditText(c);
        e.setHint(LocaleText.tr(c, hint));
        e.setHintTextColor(FAINT);
        e.setTextColor(INK);
        e.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        e.setBackground(dark
                ? rounded(c, CARD, 10, LINE, 1)
                : rounded(c, 0xFFFFFDF7, 10, 0xFFD2CAB7, 1));
        e.setPadding(dp(c, 11), dp(c, 9), dp(c, 11), dp(c, 9));
        e.setSingleLine(true);
        return e;
    }

    static EditText multiline(Context c, String hint, int lines) {
        EditText e = input(c, hint);
        e.setSingleLine(false);
        e.setMinLines(lines);
        e.setGravity(Gravity.TOP | Gravity.START);
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        return e;
    }

    static EditText number(Context c, String hint) {
        EditText e = input(c, hint);
        e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        return e;
    }

    static Spinner spinner(Context c, String[] values) {
        Spinner s = new Spinner(c, Spinner.MODE_DROPDOWN);
        String[] translated = new String[values.length];
        for (int i = 0; i < values.length; i++) translated[i] = LocaleText.tr(c, values[i]);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(c, android.R.layout.simple_spinner_item, translated) {
            private TextView styleRow(View view) {
                TextView row = (TextView) view;
                row.setTextColor(INK);
                row.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                return row;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                return styleRow(super.getView(position, convertView, parent));
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                return styleRow(super.getDropDownView(position, convertView, parent));
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s.setAdapter(adapter);
        s.setPopupBackgroundDrawable(rounded(c, CARD, 10, LINE, 1));
        s.setPadding(dp(c, 4), dp(c, 4), dp(c, 4), dp(c, 4));
        return s;
    }

    static ImageView icon(Context c, int resId, int tint, int sizeDp) {
        ImageView v = new ImageView(c);
        v.setImageResource(resId);
        v.setColorFilter(tint);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(c, sizeDp), dp(c, sizeDp));
        v.setLayoutParams(p);
        return v;
    }

    static View divider(Context c) {
        View v = new View(c);
        v.setBackgroundColor(LINE);
        v.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(c, 1)));
        return v;
    }

    static TextView pill(Context c, String value, int bg, int fg) {
        TextView t = text(c, value, 9.5f, fg, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(c, 8), dp(c, 3), dp(c, 8), dp(c, 3));
        t.setBackground(rounded(c, bg, 99, 0, 0));
        return t;
    }

    static TextView hint(Context c, String value) {
        return text(c, value, 11, MUTED, false);
    }

    static View spacer(Context c, int heightDp) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(c, heightDp)));
        return v;
    }

    static View flexSpacer(Context c) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
        return v;
    }

    static LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    static LinearLayout.LayoutParams matchMatch() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    static LinearLayout.LayoutParams weight(float w) {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, w);
    }

    static ScrollView scroll(Context c) {
        ScrollView s = new ScrollView(c);
        s.setFillViewport(true);
        s.setClipToPadding(false);
        s.setPadding(0, dp(c, 14), 0, dp(c, 24));
        s.setBackgroundColor(BG);
        return s;
    }

    static void toast(Context c, String message) {
        Toast.makeText(c, LocaleText.tr(c, message), Toast.LENGTH_SHORT).show();
    }

    static void alert(Context c, String title, String message) {
        dialog(c)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Sawa", null)
                .show();
    }

    static void confirm(Context c, String title, String message, String ok, Runnable action) {
        dialog(c)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton("Ghairi", null)
                .setPositiveButton(ok, (d, w) -> action.run())
                .show();
    }

    static AlertDialog.Builder dialog(Context c) {
        int dialogTheme = dark ? android.R.style.Theme_Material_NoActionBar
                : android.R.style.Theme_Material_Light_NoActionBar;
        return new AlertDialog.Builder(new ContextThemeWrapper(c, dialogTheme)) {
            @Override public AlertDialog.Builder setTitle(CharSequence title) {
                return super.setTitle(LocaleText.tr(c, title == null ? null : title.toString()));
            }

            @Override public AlertDialog.Builder setMessage(CharSequence message) {
                return super.setMessage(LocaleText.tr(c, message == null ? null : message.toString()));
            }

            @Override public AlertDialog.Builder setPositiveButton(CharSequence text, android.content.DialogInterface.OnClickListener listener) {
                return super.setPositiveButton(LocaleText.tr(c, text == null ? null : text.toString()), listener);
            }

            @Override public AlertDialog.Builder setNegativeButton(CharSequence text, android.content.DialogInterface.OnClickListener listener) {
                return super.setNegativeButton(LocaleText.tr(c, text == null ? null : text.toString()), listener);
            }

            @Override public AlertDialog.Builder setNeutralButton(CharSequence text, android.content.DialogInterface.OnClickListener listener) {
                return super.setNeutralButton(LocaleText.tr(c, text == null ? null : text.toString()), listener);
            }
        };
    }
}
