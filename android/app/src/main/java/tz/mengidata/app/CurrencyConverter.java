package tz.mengidata.app;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class CurrencyConverter {
    private static final String[] CURRENCIES = {"TZS", "USD", "KES", "UGX", "EUR", "GBP"};
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private CurrencyConverter() {}

    static void show(Context context) {
        LinearLayout form = Ui.column(context);
        form.setPadding(Ui.dp(context, 20), Ui.dp(context, 8), Ui.dp(context, 20), Ui.dp(context, 10));
        EditText amount = new EditText(context);
        amount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        amount.setSingleLine(true);
        amount.setHint("Kiasi");
        amount.setTextColor(Ui.INK);
        amount.setBackground(Ui.rounded(context, Ui.CARD, 10, Ui.LINE, 1));
        amount.setPadding(Ui.dp(context, 12), Ui.dp(context, 10), Ui.dp(context, 12), Ui.dp(context, 10));
        form.addView(Ui.label(context, "KIASI"), Ui.matchWrap());
        form.addView(amount, Ui.matchWrap());
        form.addView(Ui.spacer(context, 10));

        Spinner from = Ui.spinner(context, CURRENCIES);
        Spinner to = Ui.spinner(context, CURRENCIES);
        to.setSelection(1);
        form.addView(Ui.label(context, "KUTOKA"), Ui.matchWrap());
        form.addView(from, Ui.matchWrap());
        form.addView(Ui.label(context, "KWENDA"), Ui.matchWrap());
        form.addView(to, Ui.matchWrap());
        TextView result = Ui.hint(context, "Rate ya sasa inahitaji intaneti.");
        form.addView(result, Ui.matchWrap());
        TextView convert = Ui.accentButton(context, "Pata rate na badili");
        LinearLayout.LayoutParams buttonParams = Ui.matchWrap();
        buttonParams.topMargin = Ui.dp(context, 10);
        form.addView(convert, buttonParams);

        AlertDialog dialog = Ui.dialog(context).setTitle("Kibadilisha sarafu")
                .setView(form).setNegativeButton("Funga", null).create();
        convert.setOnClickListener(v -> {
            double entered;
            try {
                entered = Double.parseDouble(amount.getText().toString().replace(",", "").trim());
                if (!Double.isFinite(entered) || entered < 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                result.setText(LocaleText.tr(context, "Weka kiasi sahihi."));
                return;
            }
            String base = CURRENCIES[from.getSelectedItemPosition()];
            String quote = CURRENCIES[to.getSelectedItemPosition()];
            convert.setEnabled(false);
            result.setText(LocaleText.tr(context, "Inatafuta rate ya sasa..."));
            EXECUTOR.execute(() -> {
                try {
                    JSONObject response = fetch(base);
                    JSONObject rates = response.getJSONObject("rates");
                    if (!rates.has(quote)) throw new IllegalStateException("Sarafu haipatikani.");
                    double rate = rates.getDouble(quote);
                    double converted = entered * rate;
                    String updated = response.optString("time_last_update_utc", "");
                    MAIN.post(() -> {
                        DecimalFormat format = new DecimalFormat("#,##0.####",
                                DecimalFormatSymbols.getInstance(Locale.US));
                        result.setText(format.format(entered) + " " + base + " = "
                                + format.format(converted) + " " + quote + "\n"
                                + "1 " + base + " = " + format.format(rate) + " " + quote
                                + (updated.isEmpty() ? "" : "\n" + updated));
                        convert.setEnabled(true);
                    });
                } catch (Exception e) {
                    MAIN.post(() -> {
                        result.setText(LocaleText.tr(context,
                                "Imeshindwa kupata rate. Angalia intaneti ujaribu tena."));
                        convert.setEnabled(true);
                    });
                }
            });
        });
        dialog.show();
    }

    private static JSONObject fetch(String base) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(
                "https://open.er-api.com/v6/latest/" + base).openConnection();
        connection.setConnectTimeout(10_000);
        connection.setReadTimeout(10_000);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", "Mengidata-Android");
        try {
            int status = connection.getResponseCode();
            InputStream stream = status >= 200 && status < 300
                    ? connection.getInputStream() : connection.getErrorStream();
            if (stream == null) throw new IllegalStateException("Huduma ya rate haijibu.");
            StringBuilder body = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) body.append(line);
            }
            if (status < 200 || status >= 300) throw new IllegalStateException("Huduma haipatikani.");
            JSONObject response = new JSONObject(body.toString());
            if (!"success".equals(response.optString("result"))) {
                throw new IllegalStateException("Rate haipatikani.");
            }
            return response;
        } finally {
            connection.disconnect();
        }
    }
}
