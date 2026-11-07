package tz.mengidata.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import tz.mengidata.app.Models.Bill;
import tz.mengidata.app.Models.Invoice;
import tz.mengidata.app.Models.InvoiceItem;
import tz.mengidata.app.Models.NotebookEntry;
import tz.mengidata.app.Models.Settings;
import tz.mengidata.app.Models.Txn;

/** Offline SQLite storage for the native app. */
final class Db extends SQLiteOpenHelper {

    private static final String NAME = "mengi_data_native.db";
    private static final int VERSION = 2;
    static final int MAX_NOTEBOOK_PAGES = 500;

    private static Db instance;

    static synchronized Db get(Context context) {
        if (instance == null) instance = new Db(context.getApplicationContext());
        return instance;
    }

    private Db(Context context) {
        super(context, NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE settings (" +
                "id INTEGER PRIMARY KEY CHECK (id = 1)," +
                "business_name TEXT, owner_name TEXT, tagline TEXT, address TEXT, phone TEXT, email TEXT," +
                "tin TEXT, website TEXT, invoice_prefix TEXT, vat_rate REAL, payment_details TEXT," +
                "invoice_note TEXT, currency TEXT, demo INTEGER)");
        db.execSQL("CREATE TABLE transactions (" +
                "id TEXT PRIMARY KEY, type TEXT, date TEXT, description TEXT, party TEXT, category TEXT," +
                "amount REAL, method TEXT, reference TEXT, notes TEXT, source TEXT, linked_id TEXT, created_at TEXT)");
        db.execSQL("CREATE TABLE invoices (" +
                "id TEXT PRIMARY KEY, number TEXT, customer_name TEXT, customer_phone TEXT, customer_email TEXT," +
                "customer_address TEXT, issue_date TEXT, due_date TEXT, discount REAL, vat_rate REAL," +
                "amount_paid REAL, state TEXT, paid_date TEXT, notes TEXT, linked_txn_id TEXT," +
                "created_at TEXT, updated_at TEXT)");
        db.execSQL("CREATE TABLE invoice_items (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, invoice_id TEXT, description TEXT, qty REAL, price REAL, position INTEGER)");
        db.execSQL("CREATE TABLE bills (" +
                "id TEXT PRIMARY KEY, name TEXT, category TEXT, payee TEXT, amount REAL, due_date TEXT," +
                "paid INTEGER, paid_date TEXT, method TEXT, recurring INTEGER, notes TEXT, linked_txn_id TEXT, created_at TEXT)");
        createNotebookTable(db);
        db.execSQL("CREATE INDEX idx_txn_date ON transactions(date)");
        db.execSQL("CREATE INDEX idx_inv_issue ON invoices(issue_date)");
        db.execSQL("CREATE INDEX idx_bill_due ON bills(due_date)");

        insertDefaultSettings(db);
        seedDemo(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) createNotebookTable(db);
    }

    private static void createNotebookTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS notebook_pages (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, content TEXT NOT NULL DEFAULT ''," +
                "bookmarked INTEGER NOT NULL DEFAULT 0, updated_at TEXT NOT NULL)");
    }

    // ------------------------------------------------------------------ settings

    private static void insertDefaultSettings(SQLiteDatabase db) {
        Settings s = new Settings();
        ContentValues v = settingsValues(s);
        v.put("id", 1);
        db.insertWithOnConflict("settings", null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    private static ContentValues settingsValues(Settings s) {
        ContentValues v = new ContentValues();
        v.put("business_name", s.businessName);
        v.put("owner_name", s.ownerName);
        v.put("tagline", s.tagline);
        v.put("address", s.address);
        v.put("phone", s.phone);
        v.put("email", s.email);
        v.put("tin", s.tin);
        v.put("website", s.website);
        v.put("invoice_prefix", s.invoicePrefix);
        v.put("vat_rate", s.vatRate);
        v.put("payment_details", s.paymentDetails);
        v.put("invoice_note", s.invoiceNote);
        v.put("currency", s.currency);
        v.put("demo", s.demo ? 1 : 0);
        return v;
    }

    Settings settings() {
        Settings s = new Settings();
        Cursor c = getReadableDatabase().rawQuery("SELECT * FROM settings WHERE id = 1", null);
        try {
            if (c.moveToFirst()) readSettings(c, s);
        } finally {
            c.close();
        }
        return s;
    }

    void migrateBrandSettings() {
        Settings settings = settings();
        if (normalizeLegacyBrand(settings)) saveSettings(settings);
    }

    private static boolean normalizeLegacyBrand(Settings settings) {
        if (!"HOME SATELLITE TV".equals(settings.businessName)) return false;
        settings.businessName = "Mengidata";
        if ("Mengi".equals(settings.ownerName)) settings.ownerName = "";
        if ("Ufungaji wa Dish na huduma za TV ya satelaiti".equals(settings.tagline)) {
            settings.tagline = "Dashibodi ya biashara na fedha";
        }
        if ("HSTV".equals(settings.invoicePrefix)) settings.invoicePrefix = "MD";
        if (settings.paymentDetails.startsWith("M-Pesa Lipa Namba: 5521400 (Mengi Data)")) {
            settings.paymentDetails = "";
        }
        if (settings.invoiceNote.startsWith("Asante kwa kufanya biashara nasi. Malipo yafanyike")) {
            settings.invoiceNote = "Asante kwa kufanya biashara nasi.";
        }
        return true;
    }

    private static void readSettings(Cursor c, Settings s) {
        s.businessName = str(c, "business_name", s.businessName);
        s.ownerName = str(c, "owner_name", s.ownerName);
        s.tagline = str(c, "tagline", s.tagline);
        s.address = str(c, "address", s.address);
        s.phone = str(c, "phone", s.phone);
        s.email = str(c, "email", s.email);
        s.tin = str(c, "tin", s.tin);
        s.website = str(c, "website", s.website);
        s.invoicePrefix = str(c, "invoice_prefix", s.invoicePrefix);
        s.vatRate = dbl(c, "vat_rate", s.vatRate);
        s.paymentDetails = str(c, "payment_details", s.paymentDetails);
        s.invoiceNote = str(c, "invoice_note", s.invoiceNote);
        s.currency = str(c, "currency", s.currency);
        s.demo = c.getInt(c.getColumnIndexOrThrow("demo")) == 1;
    }

    void saveSettings(Settings s) {
        ContentValues v = settingsValues(s);
        v.put("id", 1);
        getWritableDatabase().insertWithOnConflict("settings", null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    List<NotebookEntry> notebookPages(boolean bookmarksOnly) {
        List<NotebookEntry> pages = new ArrayList<>();
        String selection = bookmarksOnly ? " WHERE bookmarked = 1" : "";
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT * FROM notebook_pages" + selection + " ORDER BY id ASC", null);
        try {
            while (c.moveToNext()) {
                NotebookEntry page = new NotebookEntry();
                page.id = c.getLong(c.getColumnIndexOrThrow("id"));
                page.title = str(c, "title", "");
                page.content = str(c, "content", "");
                page.bookmarked = c.getInt(c.getColumnIndexOrThrow("bookmarked")) == 1;
                page.updatedAt = str(c, "updated_at", "");
                pages.add(page);
            }
        } finally {
            c.close();
        }
        return pages;
    }

    int notebookPageCount() {
        Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM notebook_pages", null);
        try {
            return c.moveToFirst() ? c.getInt(0) : 0;
        } finally {
            c.close();
        }
    }

    boolean saveNotebookPage(NotebookEntry page) {
        SQLiteDatabase sql = getWritableDatabase();
        sql.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put("title", page.title);
            values.put("content", page.content);
            values.put("bookmarked", page.bookmarked ? 1 : 0);
            values.put("updated_at", new java.util.Date().toString());
            if (page.id == 0) {
                if (notebookPageCount() >= MAX_NOTEBOOK_PAGES) return false;
                page.id = sql.insertOrThrow("notebook_pages", null, values);
            } else {
                sql.update("notebook_pages", values, "id = ?", new String[]{Long.toString(page.id)});
            }
            sql.setTransactionSuccessful();
            return true;
        } finally {
            sql.endTransaction();
        }
    }

    void toggleNotebookBookmark(long id) {
        SQLiteDatabase sql = getWritableDatabase();
        sql.execSQL("UPDATE notebook_pages SET bookmarked = CASE bookmarked WHEN 1 THEN 0 ELSE 1 END," +
                " updated_at = ? WHERE id = ?", new Object[]{new java.util.Date().toString(), id});
    }

    void deleteNotebookPage(long id) {
        getWritableDatabase().delete("notebook_pages", "id = ?", new String[]{Long.toString(id)});
    }

    // ------------------------------------------------------------------ transactions

    List<Txn> transactions() {
        List<Txn> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT * FROM transactions ORDER BY date DESC, created_at DESC", null);
        try {
            while (c.moveToNext()) list.add(readTxn(c));
        } finally {
            c.close();
        }
        return list;
    }

    Txn findTxn(String id) {
        if (id == null) return null;
        Cursor c = getReadableDatabase().rawQuery("SELECT * FROM transactions WHERE id = ?", new String[]{id});
        try {
            return c.moveToFirst() ? readTxn(c) : null;
        } finally {
            c.close();
        }
    }

    private static Txn readTxn(Cursor c) {
        Txn t = new Txn();
        t.id = str(c, "id", "");
        t.type = str(c, "type", "income");
        t.date = str(c, "date", Util.today());
        t.description = str(c, "description", "");
        t.party = str(c, "party", "");
        t.category = str(c, "category", "");
        t.amount = dbl(c, "amount", 0);
        t.method = str(c, "method", "Cash");
        t.reference = str(c, "reference", "");
        t.notes = str(c, "notes", "");
        t.source = str(c, "source", "manual");
        t.linkedId = str(c, "linked_id", null);
        t.createdAt = str(c, "created_at", "");
        return t;
    }

    void saveTxn(Txn t) {
        SQLiteDatabase db = getWritableDatabase();
        if (t.id == null || t.id.isEmpty()) t.id = UUID.randomUUID().toString();
        if (t.createdAt == null || t.createdAt.isEmpty()) t.createdAt = new java.util.Date().toString();
        ContentValues v = new ContentValues();
        v.put("id", t.id);
        v.put("type", t.type);
        v.put("date", t.date);
        v.put("description", t.description);
        v.put("party", t.party);
        v.put("category", t.category);
        v.put("amount", t.amount);
        v.put("method", t.method);
        v.put("reference", t.reference);
        v.put("notes", t.notes);
        v.put("source", t.source);
        v.put("linked_id", t.linkedId);
        v.put("created_at", t.createdAt);
        db.insertWithOnConflict("transactions", null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    void deleteTxn(String id) {
        if (id == null) return;
        getWritableDatabase().delete("transactions", "id = ?", new String[]{id});
    }

    // ------------------------------------------------------------------ invoices

    List<Invoice> invoices() {
        List<Invoice> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT * FROM invoices ORDER BY issue_date DESC, number DESC", null);
        try {
            while (c.moveToNext()) {
                Invoice inv = readInvoice(c);
                inv.items.addAll(items(inv.id));
                list.add(inv);
            }
        } finally {
            c.close();
        }
        return list;
    }

    Invoice findInvoice(String id) {
        if (id == null) return null;
        Cursor c = getReadableDatabase().rawQuery("SELECT * FROM invoices WHERE id = ?", new String[]{id});
        try {
            if (!c.moveToFirst()) return null;
            Invoice inv = readInvoice(c);
            inv.items.addAll(items(inv.id));
            return inv;
        } finally {
            c.close();
        }
    }

    private List<InvoiceItem> items(String invoiceId) {
        List<InvoiceItem> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT * FROM invoice_items WHERE invoice_id = ? ORDER BY position, id", new String[]{invoiceId});
        try {
            while (c.moveToNext()) {
                InvoiceItem item = new InvoiceItem();
                item.description = str(c, "description", "");
                item.qty = dbl(c, "qty", 1);
                item.price = dbl(c, "price", 0);
                list.add(item);
            }
        } finally {
            c.close();
        }
        return list;
    }

    private static Invoice readInvoice(Cursor c) {
        Invoice inv = new Invoice();
        inv.id = str(c, "id", "");
        inv.number = str(c, "number", "");
        inv.customerName = str(c, "customer_name", "");
        inv.customerPhone = str(c, "customer_phone", "");
        inv.customerEmail = str(c, "customer_email", "");
        inv.customerAddress = str(c, "customer_address", "");
        inv.issueDate = str(c, "issue_date", Util.today());
        inv.dueDate = str(c, "due_date", Util.today());
        inv.discount = dbl(c, "discount", 0);
        inv.vatRate = dbl(c, "vat_rate", 0);
        inv.amountPaid = dbl(c, "amount_paid", 0);
        inv.state = str(c, "state", "draft");
        inv.paidDate = str(c, "paid_date", "");
        inv.notes = str(c, "notes", "");
        inv.linkedTxnId = str(c, "linked_txn_id", null);
        inv.createdAt = str(c, "created_at", "");
        inv.updatedAt = str(c, "updated_at", "");
        return inv;
    }

    void saveInvoice(Invoice inv) {
        if (inv.id == null || inv.id.isEmpty()) inv.id = UUID.randomUUID().toString();
        if (inv.number == null || inv.number.isEmpty()) inv.number = nextInvoiceNumber();
        String now = Util.today();
        if (inv.createdAt == null || inv.createdAt.isEmpty()) inv.createdAt = now;
        inv.updatedAt = now;

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues v = new ContentValues();
            v.put("id", inv.id);
            v.put("number", inv.number);
            v.put("customer_name", inv.customerName);
            v.put("customer_phone", inv.customerPhone);
            v.put("customer_email", inv.customerEmail);
            v.put("customer_address", inv.customerAddress);
            v.put("issue_date", inv.issueDate);
            v.put("due_date", inv.dueDate);
            v.put("discount", inv.discount);
            v.put("vat_rate", inv.vatRate);
            v.put("amount_paid", inv.amountPaid);
            v.put("state", inv.state);
            v.put("paid_date", inv.paidDate);
            v.put("notes", inv.notes);
            v.put("linked_txn_id", inv.linkedTxnId);
            v.put("created_at", inv.createdAt);
            v.put("updated_at", inv.updatedAt);
            db.insertWithOnConflict("invoices", null, v, SQLiteDatabase.CONFLICT_REPLACE);

            db.delete("invoice_items", "invoice_id = ?", new String[]{inv.id});
            int pos = 0;
            for (InvoiceItem item : inv.items) {
                ContentValues iv = new ContentValues();
                iv.put("invoice_id", inv.id);
                iv.put("description", item.description);
                iv.put("qty", item.qty);
                iv.put("price", item.price);
                iv.put("position", pos++);
                db.insert("invoice_items", null, iv);
            }

            String linked = syncInvoice(db, inv);
            if (linked == null ? inv.linkedTxnId != null : !linked.equals(inv.linkedTxnId)) {
                inv.linkedTxnId = linked;
                ContentValues lv = new ContentValues();
                lv.put("linked_txn_id", linked);
                db.update("invoices", lv, "id = ?", new String[]{inv.id});
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private String syncInvoice(SQLiteDatabase db, Invoice inv) {
        boolean shouldPay = "issued".equals(inv.state) && inv.amountPaid > 0.004;
        if (!shouldPay) {
            if (inv.linkedTxnId != null) {
                db.delete("transactions", "id = ?", new String[]{inv.linkedTxnId});
            }
            return null;
        }
        String date = (inv.paidDate == null || inv.paidDate.isEmpty()) ? inv.issueDate : inv.paidDate;
        ContentValues v = new ContentValues();
        v.put("date", date);
        v.put("description", "Malipo ya ankara " + inv.number);
        v.put("party", inv.customerName);
        v.put("category", "Mauzo ya bidhaa");
        v.put("amount", inv.amountPaid);
        v.put("method", "Benki");
        v.put("reference", inv.number);
        v.put("notes", "Imeundwa kiotomatiki kutoka ankara.");
        v.put("source", "invoice");
        v.put("linked_id", inv.id);

        if (inv.linkedTxnId != null && findTxn(inv.linkedTxnId) != null) {
            db.update("transactions", v, "id = ?", new String[]{inv.linkedTxnId});
            return inv.linkedTxnId;
        }
        String id = UUID.randomUUID().toString();
        v.put("id", id);
        v.put("type", "income");
        v.put("created_at", new java.util.Date().toString());
        db.insert("transactions", null, v);
        return id;
    }

    void deleteInvoice(String id) {
        Invoice inv = findInvoice(id);
        if (inv == null) return;
        SQLiteDatabase db = getWritableDatabase();
        if (inv.linkedTxnId != null) db.delete("transactions", "id = ?", new String[]{inv.linkedTxnId});
        db.delete("invoice_items", "invoice_id = ?", new String[]{id});
        db.delete("invoices", "id = ?", new String[]{id});
    }

    void markInvoicePaid(String id) {
        Invoice inv = findInvoice(id);
        if (inv == null) return;
        inv.state = "issued";
        inv.amountPaid = inv.total();
        inv.paidDate = Util.today();
        saveInvoice(inv);
    }

    void markInvoiceUnpaid(String id) {
        Invoice inv = findInvoice(id);
        if (inv == null) return;
        inv.state = "issued";
        inv.amountPaid = 0;
        inv.paidDate = "";
        saveInvoice(inv);
    }

    String nextInvoiceNumber() {
        Settings s = settings();
        String prefix = (s.invoicePrefix == null ? "HSTV" : s.invoicePrefix).toUpperCase().replaceAll("[^A-Z0-9]", "");
        if (prefix.isEmpty()) prefix = "HSTV";
        int year = Calendar.getInstance().get(Calendar.YEAR);
        String start = prefix + "-" + year + "-";
        int max = 0;
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT number FROM invoices WHERE number LIKE ?", new String[]{start + "%"});
        try {
            while (c.moveToNext()) {
                String n = c.getString(0);
                try {
                    max = Math.max(max, Integer.parseInt(n.substring(start.length())));
                } catch (Exception ignored) {
                }
            }
        } finally {
            c.close();
        }
        return start + String.format("%03d", max + 1);
    }

    // ------------------------------------------------------------------ bills

    List<Bill> bills() {
        List<Bill> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT * FROM bills ORDER BY paid ASC, due_date ASC", null);
        try {
            while (c.moveToNext()) list.add(readBill(c));
        } finally {
            c.close();
        }
        return list;
    }

    Bill findBill(String id) {
        if (id == null) return null;
        Cursor c = getReadableDatabase().rawQuery("SELECT * FROM bills WHERE id = ?", new String[]{id});
        try {
            return c.moveToFirst() ? readBill(c) : null;
        } finally {
            c.close();
        }
    }

    private static Bill readBill(Cursor c) {
        Bill b = new Bill();
        b.id = str(c, "id", "");
        b.name = str(c, "name", "");
        b.category = str(c, "category", "");
        b.payee = str(c, "payee", "");
        b.amount = dbl(c, "amount", 0);
        b.dueDate = str(c, "due_date", Util.today());
        b.paid = c.getInt(c.getColumnIndexOrThrow("paid")) == 1;
        b.paidDate = str(c, "paid_date", "");
        b.method = str(c, "method", "Benki");
        b.recurring = c.getInt(c.getColumnIndexOrThrow("recurring")) == 1;
        b.notes = str(c, "notes", "");
        b.linkedTxnId = str(c, "linked_txn_id", null);
        b.createdAt = str(c, "created_at", "");
        return b;
    }

    void saveBill(Bill b) {
        if (b.id == null || b.id.isEmpty()) b.id = UUID.randomUUID().toString();
        if (b.createdAt == null || b.createdAt.isEmpty()) b.createdAt = Util.today();
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            ContentValues v = new ContentValues();
            v.put("id", b.id);
            v.put("name", b.name);
            v.put("category", b.category);
            v.put("payee", b.payee);
            v.put("amount", b.amount);
            v.put("due_date", b.dueDate);
            v.put("paid", b.paid ? 1 : 0);
            v.put("paid_date", b.paidDate);
            v.put("method", b.method);
            v.put("recurring", b.recurring ? 1 : 0);
            v.put("notes", b.notes);
            v.put("linked_txn_id", b.linkedTxnId);
            v.put("created_at", b.createdAt);
            db.insertWithOnConflict("bills", null, v, SQLiteDatabase.CONFLICT_REPLACE);

            String linked = syncBill(db, b);
            if (linked == null ? b.linkedTxnId != null : !linked.equals(b.linkedTxnId)) {
                b.linkedTxnId = linked;
                ContentValues lv = new ContentValues();
                lv.put("linked_txn_id", linked);
                db.update("bills", lv, "id = ?", new String[]{b.id});
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private String syncBill(SQLiteDatabase db, Bill b) {
        if (!b.paid) {
            if (b.linkedTxnId != null) db.delete("transactions", "id = ?", new String[]{b.linkedTxnId});
            return null;
        }
        String date = (b.paidDate == null || b.paidDate.isEmpty()) ? b.dueDate : b.paidDate;
        ContentValues v = new ContentValues();
        v.put("date", date);
        v.put("description", "Malipo: " + b.name);
        v.put("party", b.payee);
        v.put("category", b.category);
        v.put("amount", b.amount);
        v.put("method", b.method);
        v.put("reference", "");
        v.put("notes", "Imeundwa kiotomatiki kutoka bili.");
        v.put("source", "bill");
        v.put("linked_id", b.id);
        if (b.linkedTxnId != null && findTxn(b.linkedTxnId) != null) {
            db.update("transactions", v, "id = ?", new String[]{b.linkedTxnId});
            return b.linkedTxnId;
        }
        String id = UUID.randomUUID().toString();
        v.put("id", id);
        v.put("type", "expense");
        v.put("created_at", new java.util.Date().toString());
        db.insert("transactions", null, v);
        return id;
    }

    void deleteBill(String id) {
        Bill b = findBill(id);
        if (b == null) return;
        SQLiteDatabase db = getWritableDatabase();
        if (b.linkedTxnId != null) db.delete("transactions", "id = ?", new String[]{b.linkedTxnId});
        db.delete("bills", "id = ?", new String[]{id});
    }

    void toggleBillPaid(String id) {
        Bill b = findBill(id);
        if (b == null) return;
        b.paid = !b.paid;
        b.paidDate = b.paid ? Util.today() : "";
        saveBill(b);
    }

    // ------------------------------------------------------------------ data management

    void clearData() {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("transactions", null, null);
            db.delete("invoice_items", null, null);
            db.delete("invoices", null, null);
            db.delete("bills", null, null);
            Settings s = settings();
            s.demo = false;
            saveSettings(s);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    void loadDemo() {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("transactions", null, null);
            db.delete("invoice_items", null, null);
            db.delete("invoices", null, null);
            db.delete("bills", null, null);
            Settings s = settings();
            s.demo = true;
            saveSettings(s);
            seedDemo(db);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private static void seedDemo(SQLiteDatabase db) {
        String t = Util.today();
        String m1 = Util.addMonths(t, -1);
        String m2 = Util.addMonths(t, -2);
        String m3 = Util.addMonths(t, -3);

        seedTxn(db, "income", Util.addDays(t, -30), "Mauzo ya vifaa vya ofisi & kompyuta", "Tumaini Stores",
                "Mauzo ya bidhaa", 1850000, "M-Pesa", "MP-884210");
        seedTxn(db, "income", Util.addDays(t, -27), "Huduma ya ufungaji wa mfumo wa mtandao", "Green Valley Hotel",
                "Huduma", 950000, "Benki", "CRDB-221045");
        seedTxn(db, "expense", Util.addDays(t, -25), "Manunuzi ya stock — vifaa vya ofisi", "Kariakoo Electronics Co.",
                "Manunuzi / stock", 620000, "Cash", "RCP-9941");
        seedTxn(db, "expense", Util.addDays(t, -20), "Mafuta na usafiri wa kikazi", "Puma Energy",
                "Usafiri", 180000, "Cash", "");
        seedTxn(db, "income", Util.addDays(m1, -12), "Ushauri wa mfumo wa mauzo na TEHAMA", "Sahara Agro Ltd",
                "Ushauri", 2100000, "Benki", "NMB-77812");
        seedTxn(db, "expense", Util.addDays(m1, -8), "Usafiri wa kikazi — Mwanza & Morogoro", "Air Tanzania",
                "Usafiri", 350000, "M-Pesa", "ATCL-114");
        seedTxn(db, "income", Util.addDays(m2, -15), "Mauzo ya printer na toner za ofisi", "Mlimani Fabrics",
                "Mauzo ya bidhaa", 1450000, "Benki", "");
        seedTxn(db, "expense", Util.addDays(m2, -9), "Matengenezo ya gari la kazi", "Garage ya Mjini",
                "Matengenezo", 280000, "Cash", "");
        seedTxn(db, "income", Util.addDays(m3, -18), "Mradi wa mfumo wa stoo na data", "Bahari Logistics",
                "Huduma", 3200000, "Benki", "CRDB-8812");
        seedTxn(db, "expense", Util.addDays(m3, -10), "Umeme na maji ya ofisi", "TANESCO & DAWASA",
                "Umeme & maji", 190000, "M-Pesa", "");

        Invoice inv1 = new Invoice();
        inv1.id = "demo-inv-1";
        inv1.number = "MD-" + Calendar.getInstance().get(Calendar.YEAR) + "-006";
        inv1.customerName = "Bahari Logistics Ltd";
        inv1.customerPhone = "+255 713 220 110";
        inv1.customerEmail = "accounts@bahari.co.tz";
        inv1.customerAddress = "Kurasini Bandari, Dar es Salaam";
        inv1.issueDate = Util.addDays(t, -2);
        inv1.dueDate = Util.addDays(t, 12);
        inv1.state = "draft";
        inv1.items.add(new InvoiceItem("Mfumo wa kufuatilia mizigo na maghala", 1, 2400000));
        inv1.items.add(new InvoiceItem("Mafunzo ya timu ya stoo na ripoti", 1, 600000));
        seedInvoice(db, inv1);

        Invoice inv2 = new Invoice();
        inv2.id = "demo-inv-2";
        inv2.number = "MD-" + Calendar.getInstance().get(Calendar.YEAR) + "-005";
        inv2.customerName = "Kilimanjaro Coffee Co.";
        inv2.customerPhone = "+255 754 889 200";
        inv2.customerEmail = "orders@kilimanjarocoffee.co.tz";
        inv2.customerAddress = "Moshi Mjini, Kilimanjaro";
        inv2.issueDate = Util.addDays(t, -7);
        inv2.dueDate = Util.addDays(t, 7);
        inv2.state = "issued";
        inv2.amountPaid = 600000;
        inv2.paidDate = Util.addDays(t, -3);
        inv2.items.add(new InvoiceItem("Ubunifu na uchapishaji wa vifungashio", 1, 1250000));
        inv2.items.add(new InvoiceItem("Ushauri wa chapa na masoko ya mtandao", 1, 700000));
        seedInvoice(db, inv2);

        Invoice inv3 = new Invoice();
        inv3.id = "demo-inv-3";
        inv3.number = "MD-" + Calendar.getInstance().get(Calendar.YEAR) + "-004";
        inv3.customerName = "Tumaini Stores";
        inv3.customerPhone = "+255 765 330 440";
        inv3.customerAddress = "Kariakoo Msimbazi, Dar es Salaam";
        inv3.issueDate = Util.addDays(t, -16);
        inv3.dueDate = Util.addDays(t, -2);
        inv3.state = "issued";
        inv3.amountPaid = 2300000;
        inv3.paidDate = Util.addDays(t, -2);
        inv3.items.add(new InvoiceItem("Kuweka mfumo wa mauzo (POS)", 1, 1800000));
        inv3.items.add(new InvoiceItem("Mafunzo ya wafanyakazi wa kaunta", 1, 500000));
        seedInvoice(db, inv3);

        seedBill(db, "Kodi ya pango — " + Util.monthLabel(Util.monthKey(t)), "Kodi ya pango",
                "Mengi Plaza Limited", 700000, Util.addDays(t, -5), true, Util.addDays(t, -7), true);
        seedBill(db, "Mishahara ya timu", "Mishahara", "Timu ya Mengidata", 1200000,
                Util.addDays(t, 6), false, "", true);
        seedBill(db, "Bili ya Umeme — LUKU ya Ofisi", "Umeme & maji", "TANESCO", 85000,
                Util.addDays(t, 3), false, "", true);
        seedBill(db, "Internet ya kasi (Fiber 50Mbps)", "Internet & simu", "Vodacom Business", 120000,
                Util.addDays(t, 5), false, "", true);
        seedBill(db, "Kodi ya Mapato (TRA Makadirio)", "Kodi & vibali", "TRA", 450000,
                Util.addDays(t, 10), false, "", false);
    }

    private static void seedTxn(SQLiteDatabase db, String type, String date, String desc, String party,
                                String category, double amount, String method, String ref) {
        ContentValues v = new ContentValues();
        v.put("id", UUID.randomUUID().toString());
        v.put("type", type);
        v.put("date", date);
        v.put("description", desc);
        v.put("party", party);
        v.put("category", category);
        v.put("amount", amount);
        v.put("method", method);
        v.put("reference", ref);
        v.put("notes", "");
        v.put("source", "manual");
        v.put("linked_id", (String) null);
        v.put("created_at", new java.util.Date().toString());
        db.insert("transactions", null, v);
    }

    private static void seedInvoice(SQLiteDatabase db, Invoice inv) {
        ContentValues v = new ContentValues();
        v.put("id", inv.id);
        v.put("number", inv.number);
        v.put("customer_name", inv.customerName);
        v.put("customer_phone", inv.customerPhone);
        v.put("customer_email", inv.customerEmail);
        v.put("customer_address", inv.customerAddress);
        v.put("issue_date", inv.issueDate);
        v.put("due_date", inv.dueDate);
        v.put("discount", inv.discount);
        v.put("vat_rate", inv.vatRate);
        v.put("amount_paid", inv.amountPaid);
        v.put("state", inv.state);
        v.put("paid_date", inv.paidDate);
        v.put("notes", inv.notes);
        v.put("created_at", Util.today());
        v.put("updated_at", Util.today());
        db.insert("invoices", null, v);
        int pos = 0;
        for (InvoiceItem item : inv.items) {
            ContentValues iv = new ContentValues();
            iv.put("invoice_id", inv.id);
            iv.put("description", item.description);
            iv.put("qty", item.qty);
            iv.put("price", item.price);
            iv.put("position", pos++);
            db.insert("invoice_items", null, iv);
        }
        if ("issued".equals(inv.state) && inv.amountPaid > 0) {
            ContentValues tv = new ContentValues();
            String tid = UUID.randomUUID().toString();
            tv.put("id", tid);
            tv.put("type", "income");
            tv.put("date", inv.paidDate.isEmpty() ? inv.issueDate : inv.paidDate);
            tv.put("description", "Malipo ya ankara " + inv.number);
            tv.put("party", inv.customerName);
            tv.put("category", "Mauzo ya bidhaa");
            tv.put("amount", inv.amountPaid);
            tv.put("method", "Benki");
            tv.put("reference", inv.number);
            tv.put("notes", "Imeundwa kiotomatiki kutoka ankara.");
            tv.put("source", "invoice");
            tv.put("linked_id", inv.id);
            tv.put("created_at", new java.util.Date().toString());
            db.insert("transactions", null, tv);
            ContentValues lv = new ContentValues();
            lv.put("linked_txn_id", tid);
            db.update("invoices", lv, "id = ?", new String[]{inv.id});
        }
    }

    private static void seedBill(SQLiteDatabase db, String name, String category, String payee, double amount,
                                 String due, boolean paid, String paidDate, boolean recurring) {
        ContentValues v = new ContentValues();
        String id = UUID.randomUUID().toString();
        v.put("id", id);
        v.put("name", name);
        v.put("category", category);
        v.put("payee", payee);
        v.put("amount", amount);
        v.put("due_date", due);
        v.put("paid", paid ? 1 : 0);
        v.put("paid_date", paidDate);
        v.put("method", "Benki");
        v.put("recurring", recurring ? 1 : 0);
        v.put("notes", "");
        v.put("created_at", Util.today());
        db.insert("bills", null, v);
        if (paid) {
            String tid = UUID.randomUUID().toString();
            ContentValues tv = new ContentValues();
            tv.put("id", tid);
            tv.put("type", "expense");
            tv.put("date", paidDate.isEmpty() ? due : paidDate);
            tv.put("description", "Malipo: " + name);
            tv.put("party", payee);
            tv.put("category", category);
            tv.put("amount", amount);
            tv.put("method", "Benki");
            tv.put("reference", "");
            tv.put("notes", "Imeundwa kiotomatiki kutoka bili.");
            tv.put("source", "bill");
            tv.put("linked_id", id);
            tv.put("created_at", new java.util.Date().toString());
            db.insert("transactions", null, tv);
            ContentValues lv = new ContentValues();
            lv.put("linked_txn_id", tid);
            db.update("bills", lv, "id = ?", new String[]{id});
        }
    }

    // ------------------------------------------------------------------ backup

    String exportJson() {
        try {
            JSONObject root = new JSONObject();
            root.put("version", 3);
            Settings s = settings();
            JSONObject js = new JSONObject();
            js.put("businessName", s.businessName);
            js.put("ownerName", s.ownerName);
            js.put("tagline", s.tagline);
            js.put("address", s.address);
            js.put("phone", s.phone);
            js.put("email", s.email);
            js.put("tin", s.tin);
            js.put("website", s.website);
            js.put("invoicePrefix", s.invoicePrefix);
            js.put("vatRate", s.vatRate);
            js.put("paymentDetails", s.paymentDetails);
            js.put("invoiceNote", s.invoiceNote);
            js.put("currency", s.currency);
            root.put("settings", js);

            JSONArray jt = new JSONArray();
            for (Txn t : transactions()) {
                JSONObject o = new JSONObject();
                o.put("id", t.id);
                o.put("type", t.type);
                o.put("date", t.date);
                o.put("description", t.description);
                o.put("party", t.party);
                o.put("category", t.category);
                o.put("amount", t.amount);
                o.put("method", t.method);
                o.put("reference", t.reference);
                o.put("notes", t.notes);
                o.put("source", t.source);
                o.put("linkedId", t.linkedId == null ? JSONObject.NULL : t.linkedId);
                o.put("createdAt", t.createdAt);
                jt.put(o);
            }
            root.put("transactions", jt);

            JSONArray ji = new JSONArray();
            for (Invoice inv : invoices()) {
                JSONObject o = new JSONObject();
                o.put("id", inv.id);
                o.put("number", inv.number);
                o.put("customerName", inv.customerName);
                o.put("customerPhone", inv.customerPhone);
                o.put("customerEmail", inv.customerEmail);
                o.put("customerAddress", inv.customerAddress);
                o.put("issueDate", inv.issueDate);
                o.put("dueDate", inv.dueDate);
                o.put("discount", inv.discount);
                o.put("vatRate", inv.vatRate);
                o.put("amountPaid", inv.amountPaid);
                o.put("state", inv.state);
                o.put("paidDate", inv.paidDate);
                o.put("notes", inv.notes);
                JSONArray jitems = new JSONArray();
                for (InvoiceItem item : inv.items) {
                    JSONObject io = new JSONObject();
                    io.put("description", item.description);
                    io.put("qty", item.qty);
                    io.put("price", item.price);
                    jitems.put(io);
                }
                o.put("items", jitems);
                ji.put(o);
            }
            root.put("invoices", ji);

            JSONArray jb = new JSONArray();
            for (Bill b : bills()) {
                JSONObject o = new JSONObject();
                o.put("id", b.id);
                o.put("name", b.name);
                o.put("category", b.category);
                o.put("payee", b.payee);
                o.put("amount", b.amount);
                o.put("dueDate", b.dueDate);
                o.put("paid", b.paid);
                o.put("paidDate", b.paidDate);
                o.put("method", b.method);
                o.put("recurring", b.recurring);
                o.put("notes", b.notes);
                jb.put(o);
            }
            root.put("bills", jb);

            JSONArray jn = new JSONArray();
            for (NotebookEntry page : notebookPages(false)) {
                JSONObject o = new JSONObject();
                o.put("title", page.title);
                o.put("content", page.content);
                o.put("bookmarked", page.bookmarked);
                jn.put(o);
            }
            root.put("notebook", jn);
            return root.toString(2);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    void importJson(String text) throws JSONException {
        JSONObject root = new JSONObject(text);
        JSONArray jt = root.optJSONArray("transactions");
        JSONArray ji = root.optJSONArray("invoices");
        if (jt == null || ji == null) throw new JSONException("Faili si sahihi");

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("transactions", null, null);
            db.delete("invoice_items", null, null);
            db.delete("invoices", null, null);
            db.delete("bills", null, null);

            JSONObject js = root.optJSONObject("settings");
            if (js != null) {
                Settings s = settings();
                s.businessName = js.optString("businessName", s.businessName);
                s.ownerName = js.optString("ownerName", s.ownerName);
                s.tagline = js.optString("tagline", s.tagline);
                s.address = js.optString("address", s.address);
                s.phone = js.optString("phone", s.phone);
                s.email = js.optString("email", s.email);
                s.tin = js.optString("tin", s.tin);
                s.website = js.optString("website", s.website);
                s.invoicePrefix = js.optString("invoicePrefix", s.invoicePrefix);
                s.vatRate = js.optDouble("vatRate", s.vatRate);
                s.paymentDetails = js.optString("paymentDetails", s.paymentDetails);
                s.invoiceNote = js.optString("invoiceNote", s.invoiceNote);
                s.currency = js.optString("currency", s.currency);
                s.demo = false;
                normalizeLegacyBrand(s);
                saveSettings(s);
            }

            for (int i = 0; i < jt.length(); i++) {
                JSONObject o = jt.getJSONObject(i);
                Txn t = new Txn();
                t.id = o.optString("id");
                t.type = o.optString("type", "income");
                t.date = o.optString("date", Util.today());
                t.description = o.optString("description");
                t.party = o.optString("party");
                t.category = o.optString("category");
                t.amount = o.optDouble("amount", 0);
                t.method = o.optString("method", "Cash");
                t.reference = o.optString("reference");
                t.notes = o.optString("notes");
                t.source = o.optString("source", "manual");
                t.linkedId = o.isNull("linkedId") ? null : o.optString("linkedId");
                t.createdAt = o.optString("createdAt");
                saveTxn(t);
            }

            for (int i = 0; i < ji.length(); i++) {
                JSONObject o = ji.getJSONObject(i);
                Invoice inv = new Invoice();
                inv.id = o.optString("id");
                inv.number = o.optString("number");
                inv.customerName = o.optString("customerName");
                inv.customerPhone = o.optString("customerPhone");
                inv.customerEmail = o.optString("customerEmail");
                inv.customerAddress = o.optString("customerAddress");
                inv.issueDate = o.optString("issueDate", Util.today());
                inv.dueDate = o.optString("dueDate", Util.today());
                inv.discount = o.optDouble("discount", 0);
                inv.vatRate = o.optDouble("vatRate", 0);
                inv.amountPaid = o.optDouble("amountPaid", 0);
                inv.state = o.optString("state", "draft");
                inv.paidDate = o.optString("paidDate");
                inv.notes = o.optString("notes");
                JSONArray jitems = o.optJSONArray("items");
                if (jitems != null) {
                    for (int k = 0; k < jitems.length(); k++) {
                        JSONObject io = jitems.getJSONObject(k);
                        inv.items.add(new InvoiceItem(io.optString("description"),
                                io.optDouble("qty", 1), io.optDouble("price", 0)));
                    }
                }
                saveInvoice(inv);
            }

            JSONArray jb = root.optJSONArray("bills");
            if (jb != null) {
                for (int i = 0; i < jb.length(); i++) {
                    JSONObject o = jb.getJSONObject(i);
                    Bill b = new Bill();
                    b.id = o.optString("id");
                    b.name = o.optString("name");
                    b.category = o.optString("category");
                    b.payee = o.optString("payee");
                    b.amount = o.optDouble("amount", 0);
                    b.dueDate = o.optString("dueDate", Util.today());
                    b.paid = o.optBoolean("paid", false);
                    b.paidDate = o.optString("paidDate");
                    b.method = o.optString("method", "Benki");
                    b.recurring = o.optBoolean("recurring", false);
                    b.notes = o.optString("notes");
                    saveBill(b);
                }
            }

            JSONArray jn = root.optJSONArray("notebook");
            if (jn != null) {
                db.delete("notebook_pages", null, null);
                for (int i = 0; i < Math.min(jn.length(), MAX_NOTEBOOK_PAGES); i++) {
                    JSONObject o = jn.getJSONObject(i);
                    ContentValues values = new ContentValues();
                    values.put("title", o.optString("title"));
                    values.put("content", o.optString("content"));
                    values.put("bookmarked", o.optBoolean("bookmarked", false) ? 1 : 0);
                    values.put("updated_at", new java.util.Date().toString());
                    db.insertOrThrow("notebook_pages", null, values);
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    // ------------------------------------------------------------------ cursor helpers

    private static String str(Cursor c, String column, String fallback) {
        int i = c.getColumnIndex(column);
        if (i < 0 || c.isNull(i)) return fallback;
        return c.getString(i);
    }

    private static double dbl(Cursor c, String column, double fallback) {
        int i = c.getColumnIndex(column);
        if (i < 0 || c.isNull(i)) return fallback;
        return c.getDouble(i);
    }
}
