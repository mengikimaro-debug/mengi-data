package tz.mengidata.app;

import android.app.AlertDialog;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.CheckBox;
import android.text.InputType;
import java.security.GeneralSecurityException;

import java.io.ByteArrayOutputStream;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import tz.mengidata.app.Models.Bill;
import tz.mengidata.app.Models.Invoice;
import tz.mengidata.app.Models.InvoiceItem;
import tz.mengidata.app.Models.NotebookEntry;
import tz.mengidata.app.Models.Settings;
import tz.mengidata.app.Models.Txn;

abstract class NativePage implements Page {
    final MainActivity host;
    final Db db;
    final ScrollView scroll;
    final LinearLayout body;

    NativePage(MainActivity host, Db db) {
        this.host = host;
        this.db = db;
        scroll = Ui.scroll(host);
        body = Ui.column(host);
        body.setPadding(Ui.dp(host, 16), 0, Ui.dp(host, 16), Ui.dp(host, 20));
        scroll.addView(body, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    @Override public View root() { return scroll; }
    @Override public boolean onBack() { return false; }

    LinearLayout card(String heading) {
        LinearLayout card = Ui.card(host);
        if (heading != null && !heading.isEmpty()) {
            card.addView(Ui.text(host, heading, 14, Ui.INK, true));
            card.addView(Ui.spacer(host, 8));
        }
        body.addView(card, Ui.matchWrap());
        body.addView(Ui.spacer(host, 12));
        return card;
    }

    void addLine(LinearLayout parent, String label, String value, int color) {
        LinearLayout row = Ui.row(host);
        row.addView(Ui.text(host, label, 11, Ui.MUTED, false),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(Ui.text(host, value, 13, color, true));
        parent.addView(row, Ui.matchWrap());
        parent.addView(Ui.spacer(host, 6));
    }

    EditText field(LinearLayout parent, String label, String value, String hint, boolean numeric) {
        parent.addView(Ui.label(host, label), Ui.matchWrap());
        EditText input = numeric ? Ui.number(host, hint) : Ui.input(host, hint);
        if (value != null) input.setText(value);
        LinearLayout.LayoutParams p = Ui.matchWrap();
        p.topMargin = Ui.dp(host, 4);
        p.bottomMargin = Ui.dp(host, 10);
        parent.addView(input, p);
        return input;
    }

    EditText memo(LinearLayout parent, String label, String value, String hint) {
        parent.addView(Ui.label(host, label), Ui.matchWrap());
        EditText input = Ui.multiline(host, hint, 2);
        if (value != null) input.setText(value);
        LinearLayout.LayoutParams p = Ui.matchWrap();
        p.topMargin = Ui.dp(host, 4);
        p.bottomMargin = Ui.dp(host, 10);
        parent.addView(input, p);
        return input;
    }

    void addCalculatorButton(LinearLayout parent, EditText target) {
        TextView calculator = Ui.button(host, "Fungua kikokotoo", false);
        calculator.setOnClickListener(v -> CalculatorDialog.show(host, target));
        parent.addView(calculator, Ui.matchWrap());
    }

    void empty(LinearLayout parent, String message) {
        TextView text = Ui.hint(host, message);
        text.setPadding(0, Ui.dp(host, 10), 0, Ui.dp(host, 10));
        parent.addView(text, Ui.matchWrap());
    }

    void addMonthlyChart(LinearLayout parent, List<Txn> transactions) {
        double[] income = new double[6];
        double[] expense = new double[6];
        String[] labels = new String[6];
        for (int i = 0; i < 6; i++) {
            String month = Util.monthKey(Util.addMonths(Util.today(), i - 5));
            labels[i] = Util.monthShortLabel(month);
            for (Txn txn : transactions) {
                if (!month.equals(Util.monthKey(txn.date))) continue;
                if (txn.income()) income[i] += txn.amount;
                else expense[i] += txn.amount;
            }
        }
        ChartView chart = new ChartView(host);
        chart.setData(income, expense, labels);
        parent.addView(chart, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(host, 156)));
        LinearLayout legend = Ui.row(host);
        addLegendItem(legend, "Mapato", Ui.ACCENT);
        addLegendItem(legend, "Matumizi", Ui.ROSE);
        parent.addView(legend, Ui.matchWrap());
    }

    private void addLegendItem(LinearLayout parent, String label, int color) {
        View dot = new View(host);
        dot.setBackground(Ui.rounded(host, color, 5, 0, 0));
        parent.addView(dot, new LinearLayout.LayoutParams(Ui.dp(host, 9), Ui.dp(host, 9)));
        TextView text = Ui.hint(host, LocaleText.tr(host, label));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = Ui.dp(host, 5);
        params.rightMargin = Ui.dp(host, 14);
        parent.addView(text, params);
    }

    void confirm(String title, String message, String positive, Runnable action) {
        Ui.confirm(host, title, message, positive, action);
    }

    String value(EditText input) { return input.getText().toString().trim(); }
    double number(EditText input) {
        try { return Math.max(0, Double.parseDouble(value(input).replace(",", ""))); }
        catch (NumberFormatException e) { return 0; }
    }
    boolean validDate(String date) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        format.setLenient(false);
        ParsePosition position = new ParsePosition(0);
        return format.parse(date, position) != null && position.getIndex() == date.length();
    }
}

final class DashboardPage extends NativePage {
    DashboardPage(MainActivity host, Db db) { super(host, db); }
    @Override public String title() { return "Dashibodi"; }
    @Override public String subtitle() { return "Muhtasari wa biashara"; }
    @Override public void onAdd() {
        host.showPage("transactions");
        new TransactionsPage(host, db).onAdd();
    }

    @Override public void refresh() {
        body.removeAllViews();
        List<Txn> all = db.transactions();
        String month = Util.monthKey(Util.today());
        double income = 0, expense = 0;
        for (Txn t : all) {
            if (month.equals(Util.monthKey(t.date))) {
                if (t.income()) income += t.amount; else expense += t.amount;
            }
        }
        LinearLayout hero = card(null);
        hero.setBackground(Ui.rounded(host, Ui.BRAND, 18, 0, 0));
        hero.setPadding(Ui.dp(host, 14), Ui.dp(host, 10), Ui.dp(host, 14), Ui.dp(host, 12));
        if (!Ui.isDark()) {
            ImageView illustration = new ImageView(host);
            illustration.setImageResource(R.drawable.dashboard_illustration);
            illustration.setScaleType(ImageView.ScaleType.FIT_CENTER);
            hero.addView(illustration, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(host, 106)));
        }
        TextView welcome = Ui.display(host, "Fedha zako, kwa mpangilio.", 18, 0xFFFFFFFF);
        hero.addView(welcome, Ui.matchWrap());
        TextView note = Ui.text(host, "Muhtasari wa biashara yako, hata bila intaneti.", 11, 0xFFB9D0C1, false);
        LinearLayout.LayoutParams noteParams = Ui.matchWrap();
        noteParams.topMargin = Ui.dp(host, 4);
        hero.addView(note, noteParams);

        LinearLayout summary = card(LocaleText.tr(host, "Mwezi huu") + " · " + Util.monthLabel(month));
        addLine(summary, "Fedha zilizoingia", "TZS " + Util.money(income), Ui.ACCENT);
        addLine(summary, "Fedha zilizotoka", "TZS " + Util.money(expense), Ui.ROSE);
        addLine(summary, "Faida / hasara", "TZS " + Util.money(income - expense),
                income >= expense ? Ui.ACCENT : Ui.ROSE);

        LinearLayout recent = card("Miamala ya hivi karibuni");
        int shown = 0;
        for (Txn t : all) {
            if (shown++ == 8) break;
            addLine(recent, t.description.isEmpty() ? t.party : t.description,
                    (t.income() ? "+" : "−") + " " + Util.money(t.amount),
                    t.income() ? Ui.ACCENT : Ui.ROSE);
        }
        if (all.isEmpty()) empty(recent, "Bado hakuna miamala. Bonyeza + kuongeza ya kwanza.");

        LinearLayout trend = card("Mwenendo wa miezi 6");
        addMonthlyChart(trend, all);

        double unpaid = 0;
        int dueCount = 0;
        for (Bill b : db.bills()) if (!b.paid) { unpaid += b.amount; dueCount++; }
        LinearLayout bills = card("Bili ambazo hazijalipwa");
        addLine(bills, dueCount + " " + LocaleText.tr(host, "bili"), "TZS " + Util.money(unpaid), Ui.AMBER);
        TextView open = Ui.button(host, "Fungua bili", false);
        open.setOnClickListener(v -> host.showPage("bills"));
        bills.addView(open, Ui.matchWrap());
    }
}

final class TransactionsPage extends NativePage {
    private EditText search;
    private String filter = "Zote";
    TransactionsPage(MainActivity host, Db db) { super(host, db); }
    @Override public String title() { return "Miamala"; }
    @Override public String subtitle() { return "Mapato na matumizi"; }
    @Override public void onAdd() { showForm(null); }

    @Override public void refresh() {
        body.removeAllViews();
        LinearLayout controls = card("Tafuta na chuja");
        search = Ui.input(host, "Tafuta maelezo au mteja");
        controls.addView(search, Ui.matchWrap());
        LinearLayout filters = Ui.row(host);
        for (String item : new String[]{"Zote", "Mapato", "Matumizi"}) {
            TextView button = Ui.button(host, item, filter.equals(item));
            button.setOnClickListener(v -> { filter = item; refresh(); });
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, Ui.dp(host, 40), 1);
            p.setMargins(Ui.dp(host, 2), 0, Ui.dp(host, 2), 0);
            filters.addView(button, p);
        }
        controls.addView(filters, Ui.matchWrap());
        search.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { renderRows(s.toString()); }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });
        renderRows("");
    }

    private void renderRows(String query) {
        while (body.getChildCount() > 1) body.removeViewAt(1);
        LinearLayout list = card("Miamala");
        int count = 0;
        String q = query.toLowerCase(Locale.ROOT);
        for (Txn t : db.transactions()) {
            if ("Mapato".equals(filter) && !t.income()) continue;
            if ("Matumizi".equals(filter) && t.income()) continue;
            if (!q.isEmpty() && !(t.description + " " + t.party + " " + t.category).toLowerCase(Locale.ROOT).contains(q)) continue;
            count++;
            LinearLayout row = Ui.column(host);
            LinearLayout top = Ui.row(host);
            top.addView(Ui.text(host, t.description.isEmpty() ? t.party : t.description, 12, Ui.INK, true),
                    new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            top.addView(Ui.text(host, (t.income() ? "+" : "−") + " " + Util.money(t.amount), 12,
                    t.income() ? Ui.ACCENT : Ui.ROSE, true));
            row.addView(top, Ui.matchWrap());
            row.addView(Ui.hint(host, Util.fmtDate(t.date) + " · " + t.category + " · " + t.method), Ui.matchWrap());
            LinearLayout actions = Ui.row(host);
            TextView edit = Ui.button(host, "Hariri", false);
            edit.setOnClickListener(v -> showForm(t));
            TextView delete = Ui.dangerButton(host, "Futa");
            delete.setOnClickListener(v -> confirm("Futa muamala?", "Kitendo hiki hakiwezi kutenduliwa.",
                    "Futa", () -> { db.deleteTxn(t.id); refresh(); }));
            actions.addView(edit);
            actions.addView(Ui.spacer(host, 0), new LinearLayout.LayoutParams(Ui.dp(host, 8), 1));
            actions.addView(delete);
            row.addView(actions);
            list.addView(row, Ui.matchWrap());
            list.addView(Ui.divider(host));
            list.addView(Ui.spacer(host, 8));
        }
        if (count == 0) empty(list, "Hakuna miamala inayolingana na utafutaji.");
    }

    private void showForm(Txn existing) {
        LinearLayout form = Ui.column(host);
        form.setPadding(Ui.dp(host, 18), Ui.dp(host, 6), Ui.dp(host, 18), Ui.dp(host, 6));
        android.widget.Spinner type = Ui.spinner(host, new String[]{"Fedha ndani (mapato)", "Fedha nje (matumizi)"});
        type.setSelection(existing != null && !existing.income() ? 1 : 0);
        form.addView(Ui.label(host, "AINA"), Ui.matchWrap());
        form.addView(type, Ui.matchWrap());
        EditText date = field(form, "TAREHE (YYYY-MM-DD)", existing == null ? Util.today() : existing.date, "", false);
        EditText description = field(form, "MAELEZO", existing == null ? "" : existing.description, "Mfano: Mauzo ya bidhaa", false);
        EditText party = field(form, "MTEJA / MSAMBAZAJI", existing == null ? "" : existing.party, "", false);
        form.addView(Ui.label(host, "KUNDI"), Ui.matchWrap());
        Spinner category = Ui.spinner(host, transactionCategories(type.getSelectedItemPosition() == 0,
                existing == null ? "" : existing.category));
        form.addView(category, Ui.matchWrap());
        LinearLayout customCategoryGroup = Ui.column(host);
        EditText customCategory = field(customCategoryGroup, "KUNDI MAALUM", "", "Andika kundi", false);
        form.addView(customCategoryGroup, Ui.matchWrap());
        customCategoryGroup.setVisibility(View.GONE);
        category.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                customCategoryGroup.setVisibility(isCustomCategory(category) ? View.VISIBLE : View.GONE);
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        type.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String previousCategory = category.getSelectedItem() == null ? "" : category.getSelectedItem().toString();
                category.setAdapter(Ui.spinner(host, transactionCategories(position == 0, previousCategory)).getAdapter());
                customCategoryGroup.setVisibility(isCustomCategory(category) ? View.VISIBLE : View.GONE);
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        if (existing != null) {
            int position = categoryPosition(category, existing.category);
            if (position >= 0) category.setSelection(position);
        }
        EditText amount = field(form, "KIASI (TZS)", existing == null ? "" : Util.money(existing.amount), "", true);
        addCalculatorButton(form, amount);
        EditText method = field(form, "NJIA YA MALIPO", existing == null ? "Cash" : existing.method, "Cash, M-Pesa, Benki", false);
        EditText reference = field(form, "KUMBUKUMBU", existing == null ? "" : existing.reference, "", false);
        EditText notes = memo(form, "MAELEZO YA ZIADA", existing == null ? "" : existing.notes, "");
        ScrollView wrap = new ScrollView(host);
        wrap.addView(form);
        AlertDialog dialog = Ui.dialog(host).setTitle(existing == null ? "Ongeza muamala" : "Hariri muamala")
                .setView(wrap).setNegativeButton("Ghairi", null).setPositiveButton("Hifadhi", null).create();
        dialog.setOnShowListener(x -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (value(description).isEmpty() || number(amount) <= 0 || !validDate(value(date))) {
                Ui.toast(host, "Weka maelezo, kiasi sahihi na tarehe ya YYYY-MM-DD.");
                return;
            }
            Txn t = existing == null ? new Txn() : existing;
            t.type = type.getSelectedItemPosition() == 0 ? "income" : "expense";
            t.date = value(date); t.description = value(description); t.party = value(party);
            t.category = isCustomCategory(category) ? value(customCategory) : category.getSelectedItem().toString();
            if (t.category.isEmpty()) { Ui.toast(host, "Chagua au andika kundi la muamala."); return; }
            t.amount = number(amount); t.method = value(method);
            t.reference = value(reference); t.notes = value(notes);
            db.saveTxn(t);
            dialog.dismiss(); host.reloadAll();
        }));
        dialog.show();
    }

    private String[] transactionCategories(boolean income, String existingCategory) {
        List<String> categories = new ArrayList<>();
        if (income) {
            categories.add("Huduma ya TV");
            categories.add("Vifurushi vya TV");
            categories.add("Ufungaji wa Dish");
            categories.add("Mauzo ya vifaa");
            categories.add("Malipo ya huduma");
            categories.add("Mauzo ya bidhaa");
        } else {
            categories.add("Matumizi ya kawaida");
            categories.add("Matumizi ya kila siku");
            categories.add("Matumizi ya nyumbani");
            categories.add("Chakula");
            categories.add("Umeme");
            categories.add("Maji");
            categories.add("Kodi ya nyumba");
            categories.add("Usafiri");
            categories.add("Afya");
            categories.add("Elimu");
            categories.add("Mawasiliano");
            categories.add("Mafuta");
            categories.add("Manunuzi / stock");
            categories.add("Kodi & vibali");
            categories.add("Malipo ya wafanyakazi");
        }
        String other = LocaleText.tr(host, "Nyingine");
        categories.add(other);
        if (existingCategory != null && !existingCategory.isEmpty()) {
            String translated = LocaleText.tr(host, existingCategory);
            boolean present = false;
            for (String option : categories) if (LocaleText.tr(host, option).equals(translated)) present = true;
            if (!present) categories.add(existingCategory);
        }
        return categories.toArray(new String[0]);
    }

    private int categoryPosition(Spinner category, String saved) {
        String target = LocaleText.tr(host, saved);
        for (int i = 0; i < category.getCount(); i++) {
            if (target.equals(category.getItemAtPosition(i).toString())) return i;
        }
        return -1;
    }

    private boolean isCustomCategory(Spinner category) {
        return category.getSelectedItem() != null
                && LocaleText.tr(host, "Nyingine").equals(category.getSelectedItem().toString());
    }

}

final class InvoicesPage extends NativePage {
    private String invoiceFilter = "Zote";
    InvoicesPage(MainActivity host, Db db) { super(host, db); }
    @Override public String title() { return "Ankara"; }
    @Override public String subtitle() { return "Ankara na malipo ya wateja"; }
    @Override public void onAdd() { showForm(null); }

    @Override public void refresh() {
        body.removeAllViews();
        List<Invoice> invoices = db.invoices();
        LinearLayout payments = card("Malipo ya wateja");
        double received = 0;
        double outstanding = 0;
        for (Invoice invoice : invoices) {
            if (!"draft".equals(invoice.state)) {
                received += invoice.amountPaid;
                outstanding += invoice.balance();
            }
        }
        addLine(payments, "Wamelipa", "TZS " + Util.money(received), Ui.ACCENT);
        addLine(payments, "Wanaodaiwa", "TZS " + Util.money(outstanding), Ui.ROSE);
        LinearLayout filters = Ui.row(host);
        for (String item : new String[]{"Zote", "Wamelipa", "Wanaodaiwa"}) {
            TextView filter = Ui.button(host, item, invoiceFilter.equals(item));
            filter.setOnClickListener(v -> {
                invoiceFilter = item;
                refresh();
            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, Ui.dp(host, 40), 1);
            params.setMargins(Ui.dp(host, 2), 0, Ui.dp(host, 2), 0);
            filters.addView(filter, params);
        }
        payments.addView(filters, Ui.matchWrap());

        LinearLayout list = card("Ankara zote");
        int shown = 0;
        for (Invoice inv : invoices) {
            if (!matchesInvoiceFilter(inv)) continue;
            shown++;
            LinearLayout row = Ui.column(host);
            LinearLayout top = Ui.row(host);
            top.addView(Ui.text(host, inv.number + " · " + inv.customerName, 12, Ui.INK, true),
                    new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            top.addView(Ui.pill(host, Util.statusLabel(inv), Util.statusBackground(inv), Util.statusColor(inv)));
            row.addView(top, Ui.matchWrap());
            row.addView(Ui.hint(host, Util.fmtDate(inv.issueDate) + " · "
                    + LocaleText.tr(host, "Jumla") + " TZS " + Util.money(inv.total())
                    + " · " + LocaleText.tr(host, "Salio") + " " + Util.money(inv.balance())), Ui.matchWrap());
            LinearLayout actions = Ui.row(host);
            TextView view = Ui.button(host, "Angalia", false);
            view.setOnClickListener(v -> showDetails(inv));
            actions.addView(view);
            TextView edit = Ui.button(host, "Hariri", false);
            edit.setOnClickListener(v -> showForm(inv));
            actions.addView(edit);
            TextView delete = Ui.dangerButton(host, "Futa");
            delete.setOnClickListener(v -> confirm("Futa ankara?", inv.number,
                    "Futa", () -> { db.deleteInvoice(inv.id); host.reloadAll(); }));
            actions.addView(delete);
            if (!"draft".equals(inv.state)) {
                boolean paidInFull = inv.balance() <= 0.005;
                TextView paid = paidInFull ? Ui.button(host, "Weka haijalipwa", false)
                        : Ui.accentButton(host, "Weka imelipwa");
                paid.setOnClickListener(v -> confirm(paidInFull ? "Weka haijalipwa?" : "Mteja amelipa?",
                        inv.customerName, paidInFull ? "Weka haijalipwa" : "Weka imelipwa", () -> {
                            if (paidInFull) db.markInvoiceUnpaid(inv.id);
                            else db.markInvoicePaid(inv.id);
                            host.reloadAll();
                        }));
                actions.addView(paid);
            }
            row.addView(actions);
            list.addView(row, Ui.matchWrap());
            list.addView(Ui.divider(host));
            list.addView(Ui.spacer(host, 8));
        }
        if (invoices.isEmpty()) empty(list, "Bado hakuna ankara. Bonyeza + kuunda ankara.");
        else if (shown == 0) empty(list, "Hakuna ankara katika hali hii.");
    }

    private boolean matchesInvoiceFilter(Invoice inv) {
        if ("Wamelipa".equals(invoiceFilter)) return !"draft".equals(inv.state) && inv.balance() <= 0.005;
        if ("Wanaodaiwa".equals(invoiceFilter)) return !"draft".equals(inv.state) && inv.balance() > 0.005;
        return true;
    }

    private void showDetails(Invoice inv) {
        StringBuilder text = new StringBuilder(LocaleText.tr(host, "Mteja") + ": ").append(inv.customerName)
                .append("\n").append(LocaleText.tr(host, "Tarehe")).append(": ").append(Util.fmtDate(inv.issueDate))
                .append("\n").append(LocaleText.tr(host, "Mwisho wa malipo")).append(": ").append(Util.fmtDate(inv.dueDate)).append("\n\n");
        for (InvoiceItem item : inv.items) {
            text.append(item.description).append(" · ").append(Util.qty(item.qty)).append(" × ")
                    .append(Util.money(item.price)).append(" = ").append(Util.money(item.total())).append("\n");
        }
        text.append("\n").append(LocaleText.tr(host, "Jumla")).append(": TZS ").append(Util.money(inv.total()))
                .append("\n").append(LocaleText.tr(host, "Imelipwa")).append(": TZS ").append(Util.money(inv.amountPaid))
                .append("\n").append(LocaleText.tr(host, "Salio")).append(": TZS ").append(Util.money(inv.balance()))
                .append("\n").append(LocaleText.tr(host, "Hali")).append(": ").append(LocaleText.tr(host, Util.statusLabel(inv)));
        if (inv.notes != null && !inv.notes.isEmpty()) text.append("\n\n").append(inv.notes);
        LinearLayout details = Ui.column(host);
        TextView summary = Ui.text(host, text.toString(), 12, Ui.INK, false);
        Ui.pad(summary, host, 6, 8, 6, 12);
        details.addView(summary, Ui.matchWrap());
        TextView share = Ui.accentButton(host, "Shiriki ankara");
        details.addView(share, Ui.matchWrap());
        ScrollView detailsScroll = new ScrollView(host);
        detailsScroll.addView(details);
        AlertDialog dialog = Ui.dialog(host).setTitle("Ankara " + inv.number).setView(detailsScroll)
                .setNegativeButton("Chapisha", (d, w) -> printPdf(inv))
                .setNeutralButton("PDF", (d, w) -> savePdf(inv))
                .setPositiveButton("Sawa", null).create();
        share.setOnClickListener(v -> {
            dialog.dismiss();
            sharePdf(inv);
        });
        dialog.show();
    }

    private static final class ItemFields {
        EditText description, qty, price;
    }

    private void showForm(Invoice existing) {
        LinearLayout form = Ui.column(host);
        form.setPadding(Ui.dp(host, 18), Ui.dp(host, 6), Ui.dp(host, 18), Ui.dp(host, 8));
        EditText customer = field(form, "JINA LA MTEJA *", existing == null ? "" : existing.customerName, "", false);
        EditText number = field(form, "NAMBA YA ANKARA", existing == null ? db.nextInvoiceNumber() : existing.number, "", false);
        EditText phone = field(form, "SIMU", existing == null ? "" : existing.customerPhone, "", false);
        EditText email = field(form, "BARUA PEPE", existing == null ? "" : existing.customerEmail, "", false);
        EditText address = field(form, "ANWANI", existing == null ? "" : existing.customerAddress, "", false);
        EditText issue = field(form, "TAREHE YA KUTOA (YYYY-MM-DD)", existing == null ? Util.today() : existing.issueDate, "", false);
        EditText due = field(form, "TAREHE YA MALIPO (YYYY-MM-DD)", existing == null ? Util.addDays(Util.today(), 14) : existing.dueDate, "", false);
        EditText discount = field(form, "PUNGUZO (TZS)", existing == null ? "0" : Util.money(existing.discount), "", true);
        addCalculatorButton(form, discount);
        EditText vat = field(form, "VAT (%)", existing == null ? Util.money(db.settings().vatRate) : Util.money(existing.vatRate), "", true);
        EditText paid = field(form, "KIASI KILICHOLIPWA", existing == null ? "0" : Util.money(existing.amountPaid), "", true);
        addCalculatorButton(form, paid);
        EditText notes = memo(form, "MAELEZO", existing == null ? db.settings().invoiceNote : existing.notes, "");
        form.addView(Ui.label(host, "BIDHAA / HUDUMA"), Ui.matchWrap());
        LinearLayout itemRows = Ui.column(host);
        form.addView(itemRows, Ui.matchWrap());
        List<ItemFields> rows = new ArrayList<>();
        if (existing != null && !existing.items.isEmpty()) {
            for (InvoiceItem item : existing.items) addItemRow(itemRows, rows, item);
        } else addItemRow(itemRows, rows, null);
        TextView addItem = Ui.button(host, "＋ Ongeza bidhaa/huduma", false);
        addItem.setOnClickListener(v -> addItemRow(itemRows, rows, null));
        form.addView(addItem, Ui.matchWrap());
        ScrollView wrap = new ScrollView(host);
        wrap.addView(form);
        AlertDialog dialog = Ui.dialog(host)
                .setTitle(existing == null ? "Tengeneza ankara" : "Hariri ankara")
                .setView(wrap).setNegativeButton("Ghairi", null).setNeutralButton("Hifadhi rasimu", null)
                .setPositiveButton("Hifadhi ankara", null).create();
        dialog.setOnShowListener(x -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> saveInvoice(dialog, existing,
                    customer, number, phone, email, address, issue, due, discount, vat, paid, notes, rows, false));
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> saveInvoice(dialog, existing,
                    customer, number, phone, email, address, issue, due, discount, vat, paid, notes, rows, true));
        });
        dialog.show();
    }

    private void addItemRow(LinearLayout parent, List<ItemFields> rows, InvoiceItem item) {
        LinearLayout row = Ui.column(host);
        LinearLayout.LayoutParams rp = Ui.matchWrap();
        rp.bottomMargin = Ui.dp(host, 6);
        row.setLayoutParams(rp);
        ItemFields f = new ItemFields();
        f.description = field(row, "MAELEZO", item == null ? "" : item.description, "Bidhaa au huduma", false);
        LinearLayout numbers = Ui.row(host);
        f.qty = field(numbers, "IDADI", item == null ? "1" : Util.qty(item.qty), "", true);
        f.price = field(numbers, "BEI (TZS)", item == null ? "" : Util.money(item.price), "", true);
        row.addView(numbers, Ui.matchWrap());
        addCalculatorButton(row, f.price);
        TextView remove = Ui.text(host, "Ondoa bidhaa", 11, Ui.ROSE, true);
        remove.setPadding(0, 0, 0, Ui.dp(host, 8));
        remove.setOnClickListener(v -> {
            if (rows.size() <= 1) { Ui.toast(host, "Ankara inahitaji angalau bidhaa moja."); return; }
            rows.remove(f); parent.removeView(row);
        });
        row.addView(remove, Ui.matchWrap());
        parent.addView(row, Ui.matchWrap());
        rows.add(f);
    }

    private void saveInvoice(AlertDialog dialog, Invoice existing, EditText customer, EditText number, EditText phone,
                             EditText email, EditText address,
                             EditText issue, EditText due, EditText discount, EditText vat, EditText paid,
                             EditText notes, List<ItemFields> rows, boolean draft) {
        if (value(customer).isEmpty() || value(number).isEmpty()
                || !validDate(value(issue)) || !validDate(value(due))) {
            Ui.toast(host, "Weka jina, namba ya ankara na tarehe sahihi.");
            return;
        }
        for (Invoice saved : db.invoices()) {
            if (!saved.id.equals(existing == null ? "" : existing.id)
                    && saved.number.equalsIgnoreCase(value(number))) {
                Ui.toast(host, "Namba hii ya ankara tayari inatumika.");
                return;
            }
        }
        Invoice inv = existing == null ? new Invoice() : existing;
        inv.customerName = value(customer); inv.number = value(number); inv.customerPhone = value(phone);
        inv.customerEmail = value(email); inv.customerAddress = value(address);
        inv.issueDate = value(issue); inv.dueDate = value(due); inv.discount = number(discount);
        inv.vatRate = Math.min(100, number(vat));
        inv.state = draft ? "draft" : "issued"; inv.notes = value(notes);
        inv.items.clear();
        for (ItemFields row : rows) {
            if (!value(row.description).isEmpty()) {
                InvoiceItem item = new InvoiceItem(value(row.description),
                        number(row.qty) > 0 ? number(row.qty) : 1, number(row.price));
                inv.items.add(item);
            }
        }
        if (inv.items.isEmpty()) { Ui.toast(host, "Ongeza angalau bidhaa au huduma."); return; }
        inv.amountPaid = draft ? 0 : Math.min(number(paid), inv.total());
        inv.paidDate = inv.amountPaid > 0 ? Util.today() : "";
        db.saveInvoice(inv);
        dialog.dismiss(); host.reloadAll();
    }

    private void savePdf(Invoice inv) {
        byte[] pdf = buildPdf(inv);
        if (pdf != null) host.saveFile("Ankara-" + inv.number + ".pdf", "application/pdf", pdf);
    }

    private void sharePdf(Invoice inv) {
        byte[] pdf = buildPdf(inv);
        if (pdf != null) host.saveAndShareFile("Ankara-" + inv.number + ".pdf", "application/pdf", pdf);
    }

    private void printPdf(Invoice inv) {
        byte[] pdf = buildPdf(inv);
        if (pdf != null) host.printPdf("Ankara-" + inv.number, pdf);
    }

    private byte[] buildPdf(Invoice inv) {
        PdfDocument doc = new PdfDocument();
        List<PdfDocument.Page> pages = new ArrayList<>();
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Settings settings = db.settings();
        PdfDocument.Page page = startInvoicePage(doc, inv, settings, pages, paint, true);
        Canvas canvas = page.getCanvas();
        drawInvoiceParties(canvas, inv, settings, paint);
        int y = 294;
        drawInvoiceTableHeader(canvas, y, paint);
        y += 37;

        for (InvoiceItem item : inv.items) {
            List<String> description = wrapText(item.description, paint, 255, 10.5f);
            int rowHeight = Math.max(34, description.size() * 14 + 12);
            if (y + rowHeight > 748) {
                finishInvoicePage(doc, page, settings, pages.size());
                page = startInvoicePage(doc, inv, settings, pages, paint, false);
                canvas = page.getCanvas();
                y = 157;
                drawInvoiceTableHeader(canvas, y, paint);
                y += 37;
            }
            if (((y - 331) / 34) % 2 == 0) {
                paint.setColor(Ui.CARD);
                canvas.drawRect(40, y - 22, 555, y + rowHeight - 9, paint);
            }
            paint.setColor(Ui.INK);
            paint.setTextSize(10.5f);
            paint.setTypeface(android.graphics.Typeface.DEFAULT);
            for (int line = 0; line < description.size(); line++) {
                canvas.drawText(description.get(line), 50, y + line * 14, paint);
            }
            drawRight(canvas, Util.qty(item.qty), 374, y, paint, Ui.INK, 10.5f, false);
            drawRight(canvas, Util.money(item.price), 463, y, paint, Ui.INK, 10.5f, false);
            drawRight(canvas, Util.money(item.total()), 545, y, paint, Ui.INK, 10.5f, true);
            paint.setColor(Ui.LINE);
            canvas.drawLine(40, y + rowHeight - 9, 555, y + rowHeight - 9, paint);
            y += rowHeight;
        }

        if (y + 196 > 770) {
            finishInvoicePage(doc, page, settings, pages.size());
            page = startInvoicePage(doc, inv, settings, pages, paint, false);
            canvas = page.getCanvas();
            y = 160;
        }
        y += 18;
        drawInvoiceTotals(canvas, inv, settings, y, paint);
        drawInvoiceNotes(canvas, inv, settings, y, paint);

        finishInvoicePage(doc, page, settings, pages.size());
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            doc.writeTo(output);
            return output.toByteArray();
        } catch (Exception e) {
            Ui.alert(host, "Imeshindwa kutengeneza PDF", e.getMessage() == null ? "Jaribu tena." : e.getMessage());
            return null;
        } finally {
            doc.close();
        }
    }

    private PdfDocument.Page startInvoicePage(PdfDocument doc, Invoice inv, Settings settings,
                                               List<PdfDocument.Page> pages, Paint paint, boolean first) {
        PdfDocument.Page page = doc.startPage(new PdfDocument.PageInfo.Builder(595, 842, pages.size() + 1).create());
        pages.add(page);
        Canvas canvas = page.getCanvas();
        canvas.drawColor(Color.WHITE);
        paint.setColor(Ui.BRAND);
        canvas.drawRect(0, 0, 595, 126, paint);
        paint.setColor(Ui.SILVER);
        canvas.drawRect(0, 126, 595, 131, paint);
        paint.setColor(Color.WHITE);
        paint.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD));
        paint.setTextSize(18);
        canvas.drawText(trim(settings.businessName, 34), 40, 45, paint);
        paint.setTextSize(9);
        paint.setTypeface(android.graphics.Typeface.DEFAULT);
        if (first && settings.tagline != null && !settings.tagline.isEmpty()) {
            canvas.drawText(trim(settings.tagline, 68), 40, 64, paint);
        }
        String contact = joinText(settings.phone, settings.email);
        if (contact.isEmpty()) contact = settings.address;
        canvas.drawText(trim(contact, 68), 40, 83, paint);
        canvas.drawText(trim(settings.address, 68), 40, 99, paint);
        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD));
        paint.setTextSize(22);
        canvas.drawText(LocaleText.tr(host, "ANKARA"), 555, 47, paint);
        paint.setTextSize(12);
        canvas.drawText(inv.number, 555, 70, paint);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTypeface(android.graphics.Typeface.DEFAULT);
        paint.setTextSize(9);
        canvas.drawText(LocaleText.tr(host, Util.statusLabel(inv)).toUpperCase(Locale.ROOT), 40, 117, paint);
        if (!first) {
            paint.setColor(Ui.MUTED);
            canvas.drawText(LocaleText.tr(host, "Ankara") + " " + inv.number
                    + " · " + LocaleText.tr(host, "Inaendelea"), 40, 151, paint);
        }
        return page;
    }

    private void finishInvoicePage(PdfDocument doc, PdfDocument.Page page, Settings settings, int pageNumber) {
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Ui.LINE);
        paint.setStrokeWidth(1);
        canvas.drawLine(40, 790, 555, 790, paint);
        paint.setColor(Ui.MUTED);
        paint.setTextSize(8);
        paint.setTypeface(android.graphics.Typeface.DEFAULT);
        canvas.drawText(settings.businessName + " · " + settings.phone, 40, 810, paint);
        drawRight(canvas, (LocaleText.isEnglish() ? "Page " : "Ukurasa ") + pageNumber,
                555, 810, paint, Ui.MUTED, 8, false);
        doc.finishPage(page);
    }

    private void drawInvoiceParties(Canvas canvas, Invoice inv, Settings settings, Paint paint) {
        paint.setColor(Ui.MUTED);
        paint.setTextSize(8);
        paint.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD));
        canvas.drawText(LocaleText.tr(host, "BILI KWA"), 44, 158, paint);
        canvas.drawText(LocaleText.tr(host, "TAARIFA ZA ANKARA"), 326, 158, paint);

        paint.setColor(Ui.CARD);
        canvas.drawRoundRect(new RectF(40, 168, 300, 267), 9, 9, paint);
        canvas.drawRoundRect(new RectF(312, 168, 555, 267), 9, 9, paint);

        paint.setColor(Ui.INK);
        paint.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD));
        paint.setTextSize(12);
        canvas.drawText(trim(inv.customerName, 34), 52, 190, paint);
        paint.setTypeface(android.graphics.Typeface.DEFAULT);
        paint.setTextSize(9);
        int y = 208;
        for (String line : wrapText(joinText(inv.customerAddress, inv.customerPhone, inv.customerEmail),
                paint, 230, 9)) {
            canvas.drawText(line, 52, y, paint);
            y += 14;
            if (y > 250) break;
        }

        drawMetaLine(canvas, paint, LocaleText.tr(host, "Namba"), inv.number, 188);
        drawMetaLine(canvas, paint, LocaleText.tr(host, "Tarehe"), Util.fmtDate(inv.issueDate), 213);
        drawMetaLine(canvas, paint, LocaleText.tr(host, "Mwisho wa malipo"), Util.fmtDate(inv.dueDate), 238);
        if (!settings.tin.isEmpty()) {
            paint.setColor(Ui.MUTED);
            paint.setTextSize(8);
            canvas.drawText(LocaleText.tr(host, "TIN") + ": " + settings.tin, 40, 284, paint);
        }
    }

    private void drawMetaLine(Canvas canvas, Paint paint, String label, String value, int y) {
        paint.setColor(Ui.MUTED);
        paint.setTypeface(android.graphics.Typeface.DEFAULT);
        paint.setTextSize(8);
        canvas.drawText(label, 326, y, paint);
        drawRight(canvas, value, 543, y, paint, Ui.INK, 9, true);
    }

    private void drawInvoiceTableHeader(Canvas canvas, int y, Paint paint) {
        paint.setColor(Ui.BRAND);
        canvas.drawRoundRect(new RectF(40, y - 20, 555, y + 12), 5, 5, paint);
        paint.setColor(Color.WHITE);
        paint.setTextSize(8);
        paint.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD));
        canvas.drawText(LocaleText.tr(host, "Bidhaa / huduma").toUpperCase(Locale.ROOT), 50, y, paint);
        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(LocaleText.tr(host, "Idadi"), 374, y, paint);
        canvas.drawText(LocaleText.tr(host, "Bei"), 463, y, paint);
        canvas.drawText(LocaleText.tr(host, "Jumla"), 545, y, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawInvoiceTotals(Canvas canvas, Invoice inv, Settings settings, int y, Paint paint) {
        String currency = settings.currency == null || settings.currency.isEmpty() ? "TZS" : settings.currency;
        drawRight(canvas, LocaleText.tr(host, "Jumla ndogo"), 436, y, paint, Ui.MUTED, 9, false);
        drawRight(canvas, currency + " " + Util.money(inv.subtotal()), 545, y, paint, Ui.INK, 9, false);
        y += 20;
        if (inv.discountValue() > 0) {
            drawRight(canvas, LocaleText.tr(host, "Punguzo"), 436, y, paint, Ui.MUTED, 9, false);
            drawRight(canvas, "− " + currency + " " + Util.money(inv.discountValue()), 545, y, paint, Ui.INK, 9, false);
            y += 20;
        }
        if (inv.vatRate > 0) {
            drawRight(canvas, LocaleText.tr(host, "VAT") + " (" + Util.money(inv.vatRate) + "%)", 436, y, paint, Ui.MUTED, 9, false);
            drawRight(canvas, currency + " " + Util.money(inv.vat()), 545, y, paint, Ui.INK, 9, false);
            y += 20;
        }
        paint.setColor(Ui.BRAND);
        canvas.drawRoundRect(new RectF(332, y - 15, 555, y + 21), 6, 6, paint);
        drawRight(canvas, LocaleText.tr(host, "Jumla"), 436, y + 8, paint, Color.WHITE, 10, true);
        drawRight(canvas, currency + " " + Util.money(inv.total()), 545, y + 8, paint, Color.WHITE, 11, true);
        y += 48;
        if (inv.amountPaid > 0) {
            drawRight(canvas, LocaleText.tr(host, "Imelipwa"), 436, y, paint, Ui.MUTED, 9, false);
            drawRight(canvas, currency + " " + Util.money(inv.amountPaid), 545, y, paint, Ui.ACCENT, 9, true);
            y += 20;
        }
        paint.setColor(0xFFF0F5E7);
        canvas.drawRoundRect(new RectF(332, y - 15, 555, y + 21), 6, 6, paint);
        drawRight(canvas, LocaleText.tr(host, "Salio"), 436, y + 8, paint, Ui.BRAND, 10, true);
        drawRight(canvas, currency + " " + Util.money(inv.balance()), 545, y + 8, paint, Ui.BRAND, 11, true);
    }

    private void drawInvoiceNotes(Canvas canvas, Invoice inv, Settings settings, int y, Paint paint) {
        int leftY = y;
        if (inv.notes != null && !inv.notes.trim().isEmpty()) {
            paint.setColor(Ui.BRAND);
            paint.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD));
            paint.setTextSize(9);
            canvas.drawText(LocaleText.tr(host, "Maelezo"), 40, leftY, paint);
            paint.setColor(Ui.MUTED);
            paint.setTypeface(android.graphics.Typeface.DEFAULT);
            paint.setTextSize(8);
            leftY += 15;
            for (String line : wrapText(inv.notes, paint, 255, 8)) {
                if (leftY > 755) break;
                canvas.drawText(line, 40, leftY, paint);
                leftY += 12;
            }
        }
        if (settings.paymentDetails != null && !settings.paymentDetails.trim().isEmpty()) {
            int paymentY = Math.max(leftY + 10, y);
            if (paymentY < 755) {
                paint.setColor(Ui.BRAND);
                paint.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD));
                paint.setTextSize(9);
                canvas.drawText(LocaleText.tr(host, "Taarifa za Malipo"), 40, paymentY, paint);
                paint.setColor(Ui.MUTED);
                paint.setTypeface(android.graphics.Typeface.DEFAULT);
                paint.setTextSize(8);
                paymentY += 15;
                for (String line : wrapText(settings.paymentDetails, paint, 255, 8)) {
                    if (paymentY > 755) break;
                    canvas.drawText(line, 40, paymentY, paint);
                    paymentY += 12;
                }
            }
        }
    }

    private void drawRight(Canvas canvas, String text, float x, float y, Paint paint,
                           int color, float size, boolean bold) {
        paint.setColor(color);
        paint.setTextSize(size);
        paint.setTypeface(android.graphics.Typeface.create("sans-serif",
                bold ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL));
        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(text, x, y, paint);
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private List<String> wrapText(String text, Paint paint, float width, float size) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) return lines;
        paint.setTextSize(size);
        StringBuilder line = new StringBuilder();
        for (String word : text.trim().split("\\s+")) {
            String next = line.length() == 0 ? word : line + " " + word;
            if (line.length() > 0 && paint.measureText(next) > width) {
                lines.add(line.toString());
                line.setLength(0);
                line.append(word);
            } else {
                if (line.length() > 0) line.append(' ');
                line.append(word);
            }
        }
        if (line.length() > 0) lines.add(line.toString());
        return lines;
    }

    private String joinText(String... values) {
        StringBuilder result = new StringBuilder();
        for (String value : values) {
            if (value == null || value.trim().isEmpty()) continue;
            if (result.length() > 0) result.append(" · ");
            result.append(value.trim());
        }
        return result.toString();
    }

    private String trim(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }
}

final class BillsPage extends NativePage {
    BillsPage(MainActivity host, Db db) { super(host, db); }
    @Override public String title() { return "Bili"; }
    @Override public String subtitle() { return "Bili na malipo yanayokuja"; }
    @Override public void onAdd() { showForm(null); }

    @Override public void refresh() {
        body.removeAllViews();
        LinearLayout list = card("Bili zote");
        List<Bill> bills = db.bills();
        if (bills.isEmpty()) empty(list, "Bado hakuna bili. Bonyeza + kuongeza bili.");
        for (Bill bill : bills) {
            LinearLayout row = Ui.column(host);
            LinearLayout top = Ui.row(host);
            top.addView(Ui.text(host, bill.name, 12, Ui.INK, true),
                    new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            top.addView(Ui.pill(host, bill.paid ? "Imelipwa" : (Util.overdue(bill.dueDate, false) ? "Imechelewa" : "Inasubiri"),
                    bill.paid ? Ui.SOFT_ACCENT : Ui.SOFT_ROSE, bill.paid ? Ui.ACCENT : Ui.ROSE));
            row.addView(top, Ui.matchWrap());
            row.addView(Ui.hint(host, "TZS " + Util.money(bill.amount) + " · " + Util.fmtDate(bill.dueDate)
                    + (bill.payee.isEmpty() ? "" : " · " + bill.payee)), Ui.matchWrap());
            LinearLayout actions = Ui.row(host);
            TextView toggle = Ui.button(host, bill.paid ? "Weka haijalipwa" : "Weka imelipwa", false);
            toggle.setOnClickListener(v -> { db.toggleBillPaid(bill.id); host.reloadAll(); });
            actions.addView(toggle);
            TextView edit = Ui.button(host, "Hariri", false);
            edit.setOnClickListener(v -> showForm(bill));
            actions.addView(edit);
            TextView delete = Ui.dangerButton(host, "Futa");
            delete.setOnClickListener(v -> confirm("Futa bili?", bill.name, "Futa",
                    () -> { db.deleteBill(bill.id); host.reloadAll(); }));
            actions.addView(delete);
            row.addView(actions);
            list.addView(row, Ui.matchWrap());
            list.addView(Ui.divider(host));
            list.addView(Ui.spacer(host, 8));
        }
    }

    private void showForm(Bill existing) {
        LinearLayout form = Ui.column(host);
        form.setPadding(Ui.dp(host, 18), Ui.dp(host, 6), Ui.dp(host, 18), Ui.dp(host, 6));
        EditText name = field(form, "JINA LA BILI *", existing == null ? "" : existing.name, "", false);
        String[] billCategoryValues = billCategories(existing == null ? "" : existing.category);
        form.addView(Ui.label(host, "KUNDI"), Ui.matchWrap());
        Spinner category = Ui.spinner(host, billCategoryValues);
        form.addView(category, Ui.matchWrap());
        if (existing != null) {
            int position = billCategoryPosition(category, existing.category);
            if (position >= 0) category.setSelection(position);
        }
        EditText payee = field(form, "MLIPWAJI", existing == null ? "" : existing.payee, "", false);
        EditText amount = field(form, "KIASI (TZS)", existing == null ? "" : Util.money(existing.amount), "", true);
        addCalculatorButton(form, amount);
        EditText due = field(form, "TAREHE YA MALIPO (YYYY-MM-DD)", existing == null ? Util.today() : existing.dueDate, "", false);
        EditText method = field(form, "NJIA YA MALIPO", existing == null ? LocaleText.tr(host, "Benki") : existing.method, "", false);
        CheckBox recurring = new CheckBox(host);
        recurring.setText(LocaleText.tr(host, "Bili inayojirudia"));
        recurring.setTextColor(Ui.INK);
        recurring.setChecked(existing != null && existing.recurring);
        form.addView(recurring, Ui.matchWrap());
        EditText notes = memo(form, "MAELEZO", existing == null ? "" : existing.notes, "");
        ScrollView wrap = new ScrollView(host); wrap.addView(form);
        AlertDialog dialog = Ui.dialog(host).setTitle(existing == null ? "Ongeza bili" : "Hariri bili")
                .setView(wrap).setNegativeButton("Ghairi", null).setPositiveButton("Hifadhi", null).create();
        dialog.setOnShowListener(x -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (value(name).isEmpty() || number(amount) <= 0 || !validDate(value(due))) {
                Ui.toast(host, "Weka jina, kiasi na tarehe sahihi."); return;
            }
            Bill b = existing == null ? new Bill() : existing;
            b.name = value(name);
            int categoryPosition = category.getSelectedItemPosition();
            b.category = categoryPosition >= 0 && categoryPosition < billCategoryValues.length
                    ? billCategoryValues[categoryPosition] : billCategoryValues[0];
            b.payee = value(payee);
            b.amount = number(amount); b.dueDate = value(due); b.method = value(method);
            b.recurring = recurring.isChecked(); b.notes = value(notes);
            db.saveBill(b); dialog.dismiss(); host.reloadAll();
        }));
        dialog.show();
    }

    private String[] billCategories(String existingCategory) {
        List<String> categories = new ArrayList<>();
        categories.add("Kodi ya pango");
        categories.add("Umeme");
        categories.add("Maji");
        categories.add("Umeme & maji");
        categories.add("Internet & simu");
        categories.add("Chakula");
        categories.add("Malipo ya fundi");
        categories.add("Matengenezo");
        categories.add("Mishahara");
        categories.add("Usafiri");
        categories.add("Mafuta");
        categories.add("Manunuzi / stock");
        categories.add("Bili za nyumbani");
        categories.add("Afya");
        categories.add("Elimu");
        categories.add("Kodi & vibali");
        categories.add("Bima");
        categories.add("Mkopo");
        categories.add("Masoko & matangazo");
        categories.add("Usafi");
        categories.add("Ulinzi");
        categories.add("Usafirishaji");
        categories.add("Ada za benki");
        categories.add("Vifaa vya kazi");
        categories.add("Nyingine");
        if (existingCategory != null && !existingCategory.isEmpty()) {
            String translated = LocaleText.tr(host, existingCategory);
            boolean present = false;
            for (String option : categories) {
                if (LocaleText.tr(host, option).equals(translated)) {
                    present = true;
                    break;
                }
            }
            if (!present) categories.add(existingCategory);
        }
        return categories.toArray(new String[0]);
    }

    private int billCategoryPosition(Spinner category, String saved) {
        String target = LocaleText.tr(host, saved);
        for (int i = 0; i < category.getCount(); i++) {
            if (target.equals(category.getItemAtPosition(i).toString())) return i;
        }
        return -1;
    }
}

final class ReportsPage extends NativePage {
    ReportsPage(MainActivity host, Db db) { super(host, db); }
    @Override public String title() { return "Ripoti"; }
    @Override public String subtitle() { return "Muhtasari wa fedha"; }
    @Override public void onAdd() {}

    @Override public void refresh() {
        body.removeAllViews();
        List<Txn> transactions = db.transactions();
        double income = 0, expense = 0;
        for (Txn t : transactions) if (t.income()) income += t.amount; else expense += t.amount;
        LinearLayout totals = card("Jumla ya miamala yote");
        addLine(totals, "Mapato", "TZS " + Util.money(income), Ui.ACCENT);
        addLine(totals, "Matumizi", "TZS " + Util.money(expense), Ui.ROSE);
        addLine(totals, "Faida / hasara", "TZS " + Util.money(income - expense),
                income >= expense ? Ui.ACCENT : Ui.ROSE);
        addLine(totals, "Idadi ya miamala", String.valueOf(transactions.size()), Ui.INK);
        TextView export = Ui.accentButton(host, "Pakua ripoti ya CSV");
        export.setOnClickListener(v -> exportCsv());
        totals.addView(export, Ui.matchWrap());

        LinearLayout monthly = card("Mwenendo wa miezi 6");
        addMonthlyChart(monthly, transactions);
        java.util.Map<String, Double> byCategory = new java.util.LinkedHashMap<>();
        for (Txn t : transactions) {
            String key = (t.income() ? "Mapato · " : "Matumizi · ") + (t.category.isEmpty() ? "Bila kundi" : t.category);
            byCategory.put(key, byCategory.containsKey(key) ? byCategory.get(key) + t.amount : t.amount);
        }
        LinearLayout categories = card("Muhtasari kwa kundi");
        if (byCategory.isEmpty()) empty(categories, "Hakuna miamala ya kuonyesha.");
        for (java.util.Map.Entry<String, Double> entry : byCategory.entrySet()) {
            addLine(categories, entry.getKey(), "TZS " + Util.money(entry.getValue()), Ui.INK);
        }
        TextView pdf = Ui.button(host, "Pakua ripoti ya PDF", false);
        pdf.setOnClickListener(v -> {
            byte[] data = buildReportPdf();
            if (data.length > 0) host.saveFile("mengidata-ripoti-" + Util.today() + ".pdf", "application/pdf", data);
        });
        totals.addView(pdf, Ui.matchWrap());
        TextView print = Ui.button(host, "Chapisha ripoti", false);
        print.setOnClickListener(v -> {
            byte[] data = buildReportPdf();
            if (data.length > 0) host.printPdf("Mengidata - Ripoti", data);
        });
        totals.addView(print, Ui.matchWrap());
    }

    void exportCsv() {
        StringBuilder csv = new StringBuilder("\uFEFFTarehe,Aina,Maelezo,Mteja/Msambazaji,Kundi,Njia ya malipo,Kiasi,Kumbukumbu\r\n");
        for (Txn t : db.transactions()) {
            csv.append(csv(t.date)).append(',').append(csv(t.income() ? "Fedha ndani" : "Fedha nje")).append(',')
                    .append(csv(t.description)).append(',').append(csv(t.party)).append(',')
                    .append(csv(t.category)).append(',').append(csv(t.method)).append(',')
                    .append(csv(Util.money(t.amount))).append(',').append(csv(t.reference)).append("\r\n");
        }
        host.saveFile("home-satellite-tv-miamala-" + Util.today() + ".csv", "text/csv",
                csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    private String csv(String input) { return "\"" + (input == null ? "" : input.replace("\"", "\"\"")) + "\""; }

    private byte[] buildReportPdf() {
        PdfDocument doc = new PdfDocument();
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        PdfDocument.Page page = doc.startPage(new PdfDocument.PageInfo.Builder(595, 842, 1).create());
        Canvas canvas = page.getCanvas();
        canvas.drawColor(0xFFFFFFFF);
        paint.setColor(Ui.BRAND); paint.setTextSize(24);
        paint.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD));
        canvas.drawText(LocaleText.tr(host, "Ripoti ya fedha"), 40, 54, paint);
        paint.setColor(Ui.MUTED); paint.setTextSize(12);
        paint.setTypeface(android.graphics.Typeface.DEFAULT);
        canvas.drawText(db.settings().businessName + " · " + Util.today(), 40, 76, paint);
        double income = 0, expense = 0;
        List<Txn> all = db.transactions();
        for (Txn t : all) if (t.income()) income += t.amount; else expense += t.amount;
        int y = 120;
        String[] summary = {"Mapato: TZS " + Util.money(income), "Matumizi: TZS " + Util.money(expense),
                "Faida / hasara: TZS " + Util.money(income - expense), "Miamala: " + all.size()};
        paint.setColor(Ui.INK); paint.setTextSize(14);
        for (String line : summary) { canvas.drawText(LocaleText.tr(host, line), 40, y, paint); y += 24; }
        y += 12;
        paint.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD));
        canvas.drawText(LocaleText.tr(host, "Mwenendo wa miezi 6"), 40, y, paint);
        paint.setTypeface(android.graphics.Typeface.DEFAULT);
        y += 24;
        for (int i = 5; i >= 0; i--) {
            String month = Util.monthKey(Util.addMonths(Util.today(), -i));
            double in = 0, out = 0;
            for (Txn t : all) if (month.equals(Util.monthKey(t.date))) {
                if (t.income()) in += t.amount; else out += t.amount;
            }
            canvas.drawText(Util.monthLabel(month) + ": +" + Util.money(in) + " / -" + Util.money(out), 40, y, paint);
            y += 20;
        }
        y += 14;
        paint.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD));
        canvas.drawText(LocaleText.tr(host, "Miamala ya hivi karibuni"), 40, y, paint);
        paint.setTypeface(android.graphics.Typeface.DEFAULT);
        y += 22;
        int count = 0;
        for (Txn t : all) {
            if (count++ == 18 || y > 780) break;
            String line = Util.fmtDate(t.date) + " · " + (t.income() ? "+" : "-") + Util.money(t.amount)
                    + " · " + (t.description.isEmpty() ? t.party : t.description);
            canvas.drawText(line.length() > 84 ? line.substring(0, 81) + "..." : line, 40, y, paint);
            y += 18;
        }
        doc.finishPage(page);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            doc.writeTo(output);
            return output.toByteArray();
        } catch (Exception e) {
            Ui.alert(host, "Imeshindwa kutengeneza ripoti", e.getMessage() == null ? "Jaribu tena." : e.getMessage());
            return new byte[0];
        } finally {
            doc.close();
        }
    }
}

final class NotebookPage extends NativePage {
    private boolean bookmarksOnly;

    NotebookPage(MainActivity host, Db db) { super(host, db); }
    @Override public String title() { return "Daftari"; }
    @Override public String subtitle() { return "Daftari la biashara"; }
    @Override public void onAdd() { showEditor(null); }

    @Override public void refresh() {
        body.removeAllViews();
        List<NotebookEntry> allPages = db.notebookPages(false);
        LinearLayout overview = card("Daftari la biashara");
        overview.addView(Ui.hint(host, allPages.size() + " / " + Db.MAX_NOTEBOOK_PAGES + " " +
                LocaleText.tr(host, "kurasa zimetumika")), Ui.matchWrap());
        LinearLayout filters = Ui.row(host);
        TextView all = Ui.button(host, "Kurasa zote", !bookmarksOnly);
        all.setOnClickListener(v -> {
            bookmarksOnly = false;
            refresh();
        });
        filters.addView(all, Ui.weight(1));
        TextView bookmarks = Ui.button(host, "Zilizowekwa alama", bookmarksOnly);
        LinearLayout.LayoutParams bookmarkParams = Ui.weight(1);
        bookmarkParams.leftMargin = Ui.dp(host, 8);
        filters.addView(bookmarks, bookmarkParams);
        bookmarks.setOnClickListener(v -> {
            bookmarksOnly = true;
            refresh();
        });
        LinearLayout.LayoutParams filterParams = Ui.matchWrap();
        filterParams.topMargin = Ui.dp(host, 10);
        overview.addView(filters, filterParams);

        if (allPages.isEmpty()) {
            empty(body, "Bado hakuna ukurasa. Bonyeza + kuandika dokezo la kwanza.");
            return;
        }
        int shown = 0;
        for (int i = 0; i < allPages.size(); i++) {
            NotebookEntry page = allPages.get(i);
            if (bookmarksOnly && !page.bookmarked) continue;
            shown++;
            LinearLayout item = Ui.card(host);
            body.addView(item, Ui.matchWrap());
            body.addView(Ui.spacer(host, 10));

            LinearLayout header = Ui.row(host);
            TextView number = Ui.label(host, "UKURASA " + (i + 1));
            header.addView(number, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            TextView mark = Ui.button(host, page.bookmarked ? "★" : "☆", page.bookmarked);
            mark.setContentDescription(LocaleText.tr(host, page.bookmarked
                    ? "Ondoa alama ya ukurasa" : "Weka alama ya ukurasa"));
            mark.setOnClickListener(v -> {
                db.toggleNotebookBookmark(page.id);
                refresh();
            });
            header.addView(mark, new LinearLayout.LayoutParams(
                    Ui.dp(host, 44), ViewGroup.LayoutParams.WRAP_CONTENT));
            item.addView(header, Ui.matchWrap());
            item.addView(Ui.spacer(host, 6));
            item.addView(Ui.text(host, page.title, 14, Ui.INK, true), Ui.matchWrap());
            if (!page.content.trim().isEmpty()) {
                TextView preview = Ui.hint(host, page.content.trim());
                preview.setMaxLines(3);
                LinearLayout.LayoutParams previewParams = Ui.matchWrap();
                previewParams.topMargin = Ui.dp(host, 5);
                item.addView(preview, previewParams);
            }
            item.setClickable(true);
            item.setOnClickListener(v -> showEditor(page));
        }
        if (bookmarksOnly && shown == 0) {
            empty(body, "Hakuna ukurasa uliowekwa alama bado.");
        }
    }

    private void showEditor(NotebookEntry existing) {
        NotebookEntry page = new NotebookEntry();
        if (existing != null) {
            page.id = existing.id;
            page.title = existing.title;
            page.content = existing.content;
            page.bookmarked = existing.bookmarked;
        }
        LinearLayout form = Ui.column(host);
        form.setPadding(Ui.dp(host, 20), Ui.dp(host, 8), Ui.dp(host, 20), Ui.dp(host, 4));
        EditText title = field(form, "KICHWA *", page.title, "Mfano: Oda ya mteja", false);
        form.addView(Ui.label(host, "MAELEZO"), Ui.matchWrap());
        EditText content = Ui.multiline(host, "Andika dokezo lako la biashara hapa", 8);
        content.setText(page.content);
        LinearLayout.LayoutParams contentParams = Ui.matchWrap();
        contentParams.topMargin = Ui.dp(host, 4);
        contentParams.bottomMargin = Ui.dp(host, 8);
        form.addView(content, contentParams);
        TextView error = Ui.hint(host, "");
        form.addView(error, Ui.matchWrap());

        AlertDialog.Builder builder = Ui.dialog(host)
                .setTitle(existing == null ? "Ukurasa mpya" : "Hariri ukurasa")
                .setView(form)
                .setNegativeButton("Ghairi", null)
                .setPositiveButton("Hifadhi", null);
        if (existing != null) builder.setNeutralButton("Futa ukurasa", null);
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                String pageTitle = title.getText().toString().trim();
                if (pageTitle.isEmpty()) {
                    error.setText("Weka kichwa cha ukurasa.");
                    return;
                }
                page.title = pageTitle;
                page.content = content.getText().toString().trim();
                if (!db.saveNotebookPage(page)) {
                    error.setText("Daftari limefika kikomo cha kurasa 500.");
                    return;
                }
                dialog.dismiss();
                refresh();
            });
            if (existing != null) {
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(view ->
                        Ui.confirm(host, "Futa ukurasa?", "Dokezo hili litafutwa kabisa.",
                                "Futa ukurasa", () -> {
                                    db.deleteNotebookPage(page.id);
                                    dialog.dismiss();
                                    refresh();
                                }));
            }
        });
        dialog.show();
    }
}

final class SettingsPage extends NativePage {
    SettingsPage(MainActivity host, Db db) { super(host, db); }
    @Override public String title() { return "Mipangilio"; }
    @Override public String subtitle() { return "Biashara, ankara na data"; }
    @Override public void onAdd() {}

    @Override public void refresh() {
        body.removeAllViews();
        Settings s = db.settings();
        LinearLayout business = card("Taarifa za biashara");
        EditText businessName = field(business, "JINA LA BIASHARA *", s.businessName, "", false);
        EditText owner = field(business, "MMILIKI / MSIMAMIZI", s.ownerName, "", false);
        EditText tagline = field(business, "MAELEZO MAFUPI", s.tagline, "", false);
        EditText address = field(business, "ANWANI", s.address, "", false);
        EditText phone = field(business, "SIMU", s.phone, "", false);
        EditText currency = field(business, "SARAFU", s.currency, "TZS", false);
        EditText email = field(business, "BARUA PEPE", s.email, "", false);
        EditText tin = field(business, "TIN", s.tin, "", false);
        EditText website = field(business, "TOVUTI", s.website, "", false);
        TextView saveBiz = Ui.accentButton(host, "Hifadhi taarifa za biashara");
        saveBiz.setOnClickListener(v -> {
            if (value(businessName).isEmpty()) { Ui.toast(host, "Jina la biashara linahitajika."); return; }
            Settings next = db.settings();
            next.businessName = value(businessName); next.ownerName = value(owner); next.tagline = value(tagline);
            next.address = value(address); next.phone = value(phone); next.currency = value(currency).isEmpty() ? "TZS" : value(currency).toUpperCase(Locale.ROOT);
            next.email = value(email);
            next.tin = value(tin); next.website = value(website);
            db.saveSettings(next); host.reloadAll(); Ui.toast(host, "Mipangilio imehifadhiwa.");
        });
        business.addView(saveBiz, Ui.matchWrap());

        LinearLayout invoice = card("Mipangilio ya ankara");
        EditText prefix = field(invoice, "KIAMBISHI CHA NAMBA YA ANKARA", s.invoicePrefix, "HSTV", false);
        EditText vat = field(invoice, "VAT (%)", Util.money(s.vatRate), "", true);
        EditText payment = memo(invoice, "MAELEZO YA MALIPO", s.paymentDetails, "");
        EditText note = memo(invoice, "UJUMBE KWENYE ANKARA", s.invoiceNote, "");
        TextView saveInv = Ui.button(host, "Hifadhi mipangilio ya ankara", true);
        saveInv.setOnClickListener(v -> {
            Settings next = db.settings();
            next.invoicePrefix = value(prefix).isEmpty() ? "HSTV" : value(prefix).toUpperCase(Locale.ROOT);
            next.vatRate = Math.min(100, number(vat)); next.paymentDetails = value(payment);
            next.invoiceNote = value(note);
            db.saveSettings(next); host.reloadAll(); Ui.toast(host, "Mipangilio ya ankara imehifadhiwa.");
        });
        invoice.addView(saveInv, Ui.matchWrap());

        LinearLayout security = card("Usalama na kufuli");
        TextView pinStatus = Ui.hint(host, AppLock.isEnabled(host)
                ? "PIN imewashwa. App itajifunga ikifichwa."
                : "Weka PIN ili kuzuia watu wengine kufungua app.");
        security.addView(pinStatus, Ui.matchWrap());
        TextView managePin = Ui.button(host,
                AppLock.isEnabled(host) ? "Badili PIN ya app" : "Weka PIN ya app", true);
        managePin.setOnClickListener(v -> showPinDialog());
        LinearLayout.LayoutParams managePinParams = Ui.matchWrap();
        managePinParams.topMargin = Ui.dp(host, 10);
        security.addView(managePin, managePinParams);
        security.addView(Ui.hint(host,
                "Tumia tarakimu 6–12. Ukisahau PIN, hakuna njia ya kuirejesha bila kufuta data ya app."),
                Ui.matchWrap());

        LinearLayout appearance = card("Muonekano");
        appearance.addView(Ui.hint(host,
                "Chagua mandhari ya kufuata simu, mwanga, au giza halisi lenye mandharinyuma meusi."),
                Ui.matchWrap());
        LinearLayout themeChoices = Ui.row(host);
        String[] modes = {"system", "light", "dark"};
        String[] labels = {"System", "Light", "Pure Dark"};
        String selectedMode = Ui.themeMode(host);
        for (int i = 0; i < modes.length; i++) {
            final String mode = modes[i];
            TextView choice = Ui.button(host, labels[i], mode.equals(selectedMode));
            choice.setOnClickListener(v -> host.setThemeMode(mode));
            LinearLayout.LayoutParams choiceParams = Ui.weight(1);
            if (i > 0) choiceParams.leftMargin = Ui.dp(host, 6);
            themeChoices.addView(choice, choiceParams);
        }
        LinearLayout.LayoutParams themeParams = Ui.matchWrap();
        themeParams.topMargin = Ui.dp(host, 10);
        appearance.addView(themeChoices, themeParams);

        LinearLayout currencyTools = card("Sarafu na kikokotoo");
        currencyTools.addView(Ui.hint(host,
                "Pata kiwango cha kubadilisha sarafu mtandaoni, au hesabu kiasi kwa kikokotoo."),
                Ui.matchWrap());
        TextView convertCurrency = Ui.button(host, "Badili sarafu kwa rate ya sasa", true);
        convertCurrency.setOnClickListener(v -> CurrencyConverter.show(host));
        currencyTools.addView(convertCurrency, Ui.matchWrap());
        EditText calculatorValue = Ui.number(host, "Kiasi au hesabu");
        calculatorValue.setTextSize(15);
        currencyTools.addView(calculatorValue, Ui.matchWrap());
        addCalculatorButton(currencyTools, calculatorValue);

        LinearLayout updates = card("Updates za app");
        TextView updateStatus = Ui.hint(host, "Toleo lililosakinishwa: " + ReleaseUpdater.currentVersion(host));
        updates.addView(updateStatus, Ui.matchWrap());
        TextView autoUpdates = Ui.button(host,
                ReleaseUpdater.autoUpdatesEnabled(host) ? "Zima ukaguzi wa update kiotomatiki"
                        : "Washa ukaguzi wa update kiotomatiki", false);
        autoUpdates.setOnClickListener(v -> {
            boolean enabled = !ReleaseUpdater.autoUpdatesEnabled(host);
            ReleaseUpdater.setAutoUpdatesEnabled(host, enabled);
            autoUpdates.setText(LocaleText.tr(host, enabled
                    ? "Zima ukaguzi wa update kiotomatiki"
                    : "Washa ukaguzi wa update kiotomatiki"));
            Ui.toast(host, enabled ? "Ukaguzi wa update utawashwa app inapofunguliwa."
                    : "Ukaguzi wa update kiotomatiki umezimwa.");
        });
        updates.addView(autoUpdates, Ui.matchWrap());
        TextView checkUpdates = Ui.button(host, "Angalia update sasa", true);
        checkUpdates.setOnClickListener(v -> host.checkForUpdates(false));
        updates.addView(checkUpdates, Ui.matchWrap());
        updates.addView(Ui.hint(host,
                "Ukaguzi wa kiotomatiki hufanyika app inapofunguliwa (kila baada ya saa 12). Android itaomba uthibitishe usakinishaji."),
                Ui.matchWrap());

        LinearLayout data = card("Data na nakala rudufu");
        TextView backup = Ui.button(host, "Pakua nakala rudufu (.json)", false);
        backup.setOnClickListener(v -> host.saveFile("home-satellite-tv-backup-" + Util.today() + ".json",
                "application/json", db.exportJson().getBytes(StandardCharsets.UTF_8)));
        data.addView(backup, Ui.matchWrap());
        TextView restore = Ui.button(host, "Rejesha nakala rudufu (.json)", false);
        restore.setOnClickListener(v -> host.chooseBackup());
        data.addView(restore, Ui.matchWrap());
        TextView csv = Ui.button(host, "Pakua miamala (.csv)", false);
        csv.setOnClickListener(v -> new ReportsPage(host, db).exportCsv());
        data.addView(csv, Ui.matchWrap());
        TextView reminders = Ui.button(host,
                BillReminderReceiver.isEnabled(host) ? "Zima vikumbusho vya bili" : "Washa vikumbusho vya bili",
                false);
        reminders.setOnClickListener(v -> host.toggleBillReminders());
        data.addView(reminders, Ui.matchWrap());
        data.addView(Ui.hint(host,
                "Arifa ya bili hutumwa siku ya malipo saa 9:00 asubuhi. Bili isiyolipwa hukumbushwa kila siku."),
                Ui.matchWrap());
        TextView clear = Ui.dangerButton(host, "Futa miamala, ankara na bili");
        clear.setOnClickListener(v -> confirm("Futa data zote za fedha?",
                "Taarifa za biashara zitabaki. Data iliyofutwa haiwezi kurejeshwa bila nakala rudufu.",
                "Futa data", () -> { db.clearData(); host.reloadAll(); }));
        data.addView(clear, Ui.matchWrap());
        TextView demo = Ui.button(host, "Pakia data ya mfano", false);
        demo.setOnClickListener(v -> confirm("Pakia data ya mfano?",
                "Miamala, ankara na bili zilizopo zitabadilishwa na data ya mfano.",
                "Pakia mfano", () -> { db.loadDemo(); host.reloadAll(); }));
        data.addView(demo, Ui.matchWrap());
        data.addView(Ui.hint(host, "Hufanya kazi bila intaneti. Data yako huhifadhiwa kwenye simu hii."),
                Ui.matchWrap());
    }

    private void showPinDialog() {
        boolean hasPin = AppLock.isEnabled(host);
        LinearLayout form = Ui.column(host);
        form.setPadding(Ui.dp(host, 20), Ui.dp(host, 8), Ui.dp(host, 20), Ui.dp(host, 4));

        EditText currentPin = null;
        if (hasPin) {
            currentPin = secureField(form, "PIN ya sasa");
        }
        EditText newPin = secureField(form, "PIN mpya (tarakimu 6–12)");
        EditText confirmation = secureField(form, "Rudia PIN mpya");
        TextView error = Ui.hint(host, "");
        form.addView(error, Ui.matchWrap());

        EditText oldPinField = currentPin;
        AlertDialog dialog = Ui.dialog(host)
                .setTitle(hasPin ? "Badili PIN ya app" : "Weka PIN ya app")
                .setView(form)
                .setNegativeButton("Ghairi", null)
                .setPositiveButton(hasPin ? "Badili PIN" : "Weka PIN", null)
                .setNeutralButton(hasPin ? "Zima kufuli" : null, null)
                .create();
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                String next = newPin.getText().toString();
                String repeated = confirmation.getText().toString();
                if (hasPin && !verifyCurrentPin(oldPinField, error)) return;
                if (!AppLock.isValidPin(next)) {
                    error.setText("PIN iwe tarakimu 6–12, na isiwe mfululizo rahisi.");
                    return;
                }
                if (!next.equals(repeated)) {
                    error.setText("PIN mbili hazifanani.");
                    return;
                }
                try {
                    AppLock.setPin(host, next);
                    host.updateSecureWindow();
                    host.reloadAll();
                    Ui.toast(host, "PIN imehifadhiwa.");
                    dialog.dismiss();
                } catch (GeneralSecurityException e) {
                    error.setText("Imeshindwa kuhifadhi PIN kwa usalama. Jaribu tena.");
                }
            });
            if (hasPin) {
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(view -> {
                    if (!verifyCurrentPin(oldPinField, error)) return;
                    Ui.confirm(host, "Zima kufuli la PIN?",
                            "Mtu yeyote anayeweza kufungua simu ataweza kufungua app hii.",
                            "Zima kufuli", () -> {
                                AppLock.disable(host);
                                host.updateSecureWindow();
                                host.reloadAll();
                                Ui.toast(host, "Kufuli la PIN limezimwa.");
                                dialog.dismiss();
                            });
                });
            }
        });
        dialog.show();
    }

    private EditText secureField(LinearLayout form, String label) {
        EditText input = field(form, label, "", "", true);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        return input;
    }

    private boolean verifyCurrentPin(EditText input, TextView error) {
        if (input == null || AppLock.remainingLockoutMillis(host) > 0) {
            error.setText("PIN imefungwa kwa muda baada ya majaribio mengi. Subiri kisha ujaribu tena.");
            return false;
        }
        try {
            if (AppLock.verify(host, input.getText().toString())) return true;
            input.setText("");
            error.setText(AppLock.remainingLockoutMillis(host) > 0
                    ? "PIN imefungwa kwa muda baada ya majaribio mengi. Subiri kisha ujaribu tena."
                    : "PIN ya sasa si sahihi.");
        } catch (GeneralSecurityException e) {
            error.setText("Imeshindwa kuthibitisha PIN kwa usalama. Jaribu tena.");
        }
        return false;
    }
}
