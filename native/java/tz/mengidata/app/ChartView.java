package tz.mengidata.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;

/** Simple grouped bar chart drawn with Canvas (income vs expense). */
final class ChartView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private double[] income = new double[0];
    private double[] expense = new double[0];
    private String[] labels = new String[0];

    ChartView(Context context) {
        super(context);
        textPaint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        textPaint.setTextSize(Ui.dp(context, 9));
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    void setData(double[] income, double[] expense, String[] labels) {
        this.income = income;
        this.expense = expense;
        this.labels = labels;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;
        int bottom = h - Ui.dp(getContext(), 18);
        int top = Ui.dp(getContext(), 10);
        int left = Ui.dp(getContext(), 4);
        int right = w - Ui.dp(getContext(), 4);

        double max = 1;
        for (double v : income) max = Math.max(max, v);
        for (double v : expense) max = Math.max(max, v);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Ui.dp(getContext(), 0.7f));
        paint.setColor(Ui.LINE);
        for (int i = 0; i <= 3; i++) {
            float y = top + (bottom - top) * i / 3f;
            canvas.drawLine(left, y, right, y, paint);
        }

        int n = income.length;
        if (n == 0) return;
        float slot = (right - left) / (float) n;
        float barW = Math.min(Ui.dp(getContext(), 12), slot * 0.26f);
        float gap = Ui.dp(getContext(), 3);
        for (int i = 0; i < n; i++) {
            float cx = left + slot * i + slot / 2f;
            float ih = (float) ((bottom - top) * (income[i] / max));
            float eh = (float) ((bottom - top) * (expense[i] / max));
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Ui.ACCENT);
            canvas.drawRoundRect(new RectF(cx - gap / 2 - barW, bottom - ih, cx - gap / 2, bottom), barW / 2f, barW / 2f, paint);
            paint.setColor(Ui.ROSE);
            canvas.drawRoundRect(new RectF(cx + gap / 2, bottom - eh, cx + gap / 2 + barW, bottom), barW / 2f, barW / 2f, paint);
            if (i < labels.length) {
                textPaint.setColor(Ui.MUTED);
                canvas.drawText(labels[i], cx, h - Ui.dp(getContext(), 4), textPaint);
            }
        }
    }
}
