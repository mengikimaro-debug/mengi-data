package tz.mengidata.app;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import tz.mengidata.app.Models.Invoice;

/** Small formatting/date helpers shared by the native screens. */
final class Util {

    static final Locale SW = new Locale("sw", "TZ");
    private static final String ISO = "yyyy-MM-dd";

    private Util() {}

    static String today() {
        return new SimpleDateFormat(ISO, Locale.US).format(new Date());
    }

    static Date parseIso(String iso) {
        try {
            return new SimpleDateFormat(ISO, Locale.US).parse(iso == null ? "" : iso);
        } catch (ParseException e) {
            return new Date();
        }
    }

    static String iso(Date date) {
        return new SimpleDateFormat(ISO, Locale.US).format(date);
    }

    static String addDays(String iso, int days) {
        Calendar c = Calendar.getInstance();
        c.setTime(parseIso(iso));
        c.add(Calendar.DAY_OF_MONTH, days);
        return iso(c.getTime());
    }

    static String addMonths(String iso, int months) {
        Calendar c = Calendar.getInstance();
        c.setTime(parseIso(iso));
        c.add(Calendar.MONTH, months);
        return iso(c.getTime());
    }

    static String fmtDate(String iso) {
        if (iso == null || iso.isEmpty()) return "—";
        return new SimpleDateFormat("dd MMM yyyy", LocaleText.isEnglish() ? Locale.US : SW).format(parseIso(iso));
    }

    static String fmtDateShort(String iso) {
        if (iso == null || iso.isEmpty()) return "—";
        return new SimpleDateFormat("dd MMM", LocaleText.isEnglish() ? Locale.US : SW).format(parseIso(iso));
    }

    static String monthKey(String iso) {
        return iso == null || iso.length() < 7 ? "" : iso.substring(0, 7);
    }

    static String monthLabel(String key) {
        if (key == null || key.length() < 7) return "—";
        try {
            Date d = new SimpleDateFormat("yyyy-MM", Locale.US).parse(key);
            return new SimpleDateFormat("MMM yyyy", LocaleText.isEnglish() ? Locale.US : SW).format(d);
        } catch (ParseException e) {
            return key;
        }
    }

    static String monthShortLabel(String key) {
        if (key == null || key.length() < 7) return "—";
        try {
            Date d = new SimpleDateFormat("yyyy-MM", Locale.US).parse(key);
            return new SimpleDateFormat("MMM", LocaleText.isEnglish() ? Locale.US : SW).format(d);
        } catch (ParseException e) {
            return key.substring(5, 7);
        }
    }

    static String money(double value) {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        nf.setMaximumFractionDigits(0);
        nf.setMinimumFractionDigits(0);
        return nf.format(Math.abs(value) < 0.005 ? 0 : value);
    }

    static String tzs(double value) {
        return "TZS " + money(value);
    }

    static String qty(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.0001) return money(value);
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        nf.setMaximumFractionDigits(2);
        return nf.format(value);
    }

    static boolean overdue(String dueDate, boolean paid) {
        return !paid && dueDate != null && !dueDate.isEmpty() && dueDate.compareTo(today()) < 0;
    }

    static int daysBetween(String from, String to) {
        long a = parseIso(from).getTime();
        long b = parseIso(to).getTime();
        return (int) Math.round((b - a) / 86400000.0);
    }

    static String statusKey(Invoice inv) {
        if ("draft".equals(inv.state)) return "draft";
        double total = inv.total();
        if (total <= 0) return "draft";
        if (inv.amountPaid >= total - 0.5) return "paid";
        if (inv.amountPaid > 0) return "partial";
        if (overdue(inv.dueDate, false)) return "overdue";
        return "issued";
    }

    static String statusLabel(Invoice inv) {
        String key = statusKey(inv);
        switch (key) {
            case "paid": return "Imelipwa";
            case "partial": return "Malipo kwa kiasi";
            case "overdue": return "Imechelewa";
            case "issued": return "Haijalipwa";
            default: return "Rasimu";
        }
    }

    static int statusColor(Invoice inv) {
        String key = statusKey(inv);
        switch (key) {
            case "paid": return 0xFF226B44;
            case "partial": return 0xFF946510;
            case "overdue": return 0xFFA3462C;
            default: return 0xFF6F6A5E;
        }
    }

    static int statusBackground(Invoice inv) {
        String key = statusKey(inv);
        switch (key) {
            case "paid": return 0xFFDCEBDC;
            case "partial": return 0xFFF8EDD3;
            case "overdue": return 0xFFF7E4DB;
            default: return 0xFFEFEDE5;
        }
    }
}
