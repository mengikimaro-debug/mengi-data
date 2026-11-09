package tz.mengidata.app;

import android.app.AlertDialog;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

final class CalculatorDialog {
    private CalculatorDialog() {}

    static void show(Context context, EditText destination) {
        LinearLayout content = Ui.column(context);
        content.setPadding(Ui.dp(context, 18), Ui.dp(context, 8), Ui.dp(context, 18), Ui.dp(context, 12));
        TextView display = Ui.text(context, "", 24, Ui.INK, true);
        display.setText(destination.getText().toString());
        display.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        display.setPadding(Ui.dp(context, 10), Ui.dp(context, 12), Ui.dp(context, 10), Ui.dp(context, 12));
        display.setBackground(Ui.rounded(context, Ui.CARD, 10, Ui.LINE, 1));
        content.addView(display, Ui.matchWrap());

        GridLayout keys = new GridLayout(context);
        keys.setColumnCount(4);
        String[] labels = {"C", "⌫", "(", ")", "7", "8", "9", "÷",
                "4", "5", "6", "×", "1", "2", "3", "−", "0", ".", "+", "="};
        for (String label : labels) {
            TextView key = Ui.button(context, label, "=".equals(label));
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = Ui.dp(context, 48);
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(Ui.dp(context, 3), Ui.dp(context, 3), Ui.dp(context, 3), Ui.dp(context, 3));
            keys.addView(key, params);
            key.setOnClickListener(v -> {
                String expression = display.getText().toString();
                if ("C".equals(label)) {
                    display.setText("");
                } else if ("⌫".equals(label)) {
                    if (!expression.isEmpty()) display.setText(expression.substring(0, expression.length() - 1));
                } else if ("=".equals(label)) {
                    try {
                        double result = new ExpressionParser(expression).parse();
                        if (!Double.isFinite(result)) throw new IllegalArgumentException();
                        DecimalFormat format = new DecimalFormat("0.##########",
                                DecimalFormatSymbols.getInstance(Locale.US));
                        display.setText(format.format(result));
                    } catch (IllegalArgumentException e) {
                        Ui.toast(context, "Hesabu haijakamilika au si sahihi.");
                    }
                } else {
                    display.append(label);
                }
            });
        }
        content.addView(keys, Ui.matchWrap());
        AlertDialog dialog = Ui.dialog(context).setTitle("Kikokotoo")
                .setView(content).setNegativeButton("Ghairi", null)
                .setPositiveButton("Tumia kiasi", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    try {
                        double result = new ExpressionParser(display.getText().toString()).parse();
                        if (!Double.isFinite(result)) throw new IllegalArgumentException();
                        destination.setText(new DecimalFormat("0.##########",
                                DecimalFormatSymbols.getInstance(Locale.US)).format(result));
                        dialog.dismiss();
                    } catch (IllegalArgumentException e) {
                        Ui.toast(context, "Weka hesabu sahihi kwanza.");
                    }
                }));
        dialog.show();
    }

    private static final class ExpressionParser {
        private final String input;
        private int index;

        ExpressionParser(String input) {
            this.input = input.replace("×", "*").replace("÷", "/").replace("−", "-")
                    .replace(" ", "");
        }

        double parse() {
            double value = expression();
            if (index != input.length()) throw new IllegalArgumentException();
            return value;
        }

        private double expression() {
            double value = term();
            while (take('+') || take('-')) {
                char operator = input.charAt(index - 1);
                double right = term();
                value = operator == '+' ? value + right : value - right;
            }
            return value;
        }

        private double term() {
            double value = factor();
            while (take('*') || take('/')) {
                char operator = input.charAt(index - 1);
                double right = factor();
                if (operator == '/' && right == 0) throw new IllegalArgumentException();
                value = operator == '*' ? value * right : value / right;
            }
            return value;
        }

        private double factor() {
            if (take('+')) return factor();
            if (take('-')) return -factor();
            if (take('(')) {
                double value = expression();
                if (!take(')')) throw new IllegalArgumentException();
                return value;
            }
            int start = index;
            while (index < input.length()
                    && (Character.isDigit(input.charAt(index)) || input.charAt(index) == '.')) index++;
            if (start == index) throw new IllegalArgumentException();
            return Double.parseDouble(input.substring(start, index));
        }

        private boolean take(char expected) {
            if (index < input.length() && input.charAt(index) == expected) {
                index++;
                return true;
            }
            return false;
        }
    }
}
