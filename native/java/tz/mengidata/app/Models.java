package tz.mengidata.app;

import java.util.ArrayList;
import java.util.List;

/** Plain data objects used by the native screens and the SQLite repository. */
final class Models {

    private Models() {}

    static final class Settings {
        String businessName = "Mengidata";
        String ownerName = "";
        String tagline = "Dashibodi ya biashara na fedha";
        String address = "";
        String phone = "";
        String email = "";
        String tin = "";
        String website = "";
        String invoicePrefix = "MD";
        String paymentDetails = "";
        String invoiceNote = "Asante kwa kufanya biashara nasi.";
        String currency = "TZS";
        double vatRate = 0;
        boolean demo = true;
    }

    static final class Txn {
        String id = "";
        String type = "income"; // income | expense
        String date = Util.today();
        String description = "";
        String party = "";
        String category = "Mauzo ya bidhaa";
        double amount = 0;
        String method = "Cash";
        String reference = "";
        String notes = "";
        String source = "manual";
        String linkedId;
        String createdAt = "";

        boolean income() {
            return "income".equals(type);
        }
    }

    static final class InvoiceItem {
        String description = "";
        double qty = 1;
        double price = 0;

        InvoiceItem() {}

        InvoiceItem(String description, double qty, double price) {
            this.description = description;
            this.qty = qty;
            this.price = price;
        }

        double total() {
            return qty * price;
        }
    }

    static final class Invoice {
        String id = "";
        String number = "";
        String customerName = "";
        String customerPhone = "";
        String customerEmail = "";
        String customerAddress = "";
        String issueDate = Util.today();
        String dueDate = Util.addDays(Util.today(), 14);
        double discount = 0;
        double vatRate = 0;
        double amountPaid = 0;
        String state = "draft";
        String paidDate = "";
        String notes = "";
        String linkedTxnId;
        String createdAt = "";
        String updatedAt = "";
        final List<InvoiceItem> items = new ArrayList<>();

        double subtotal() {
            double sum = 0;
            for (InvoiceItem item : items) sum += item.total();
            return sum;
        }

        double discountValue() {
            return Math.min(Math.max(discount, 0), subtotal());
        }

        double vat() {
            return (subtotal() - discountValue()) * Math.max(vatRate, 0) / 100.0;
        }

        double total() {
            return subtotal() - discountValue() + vat();
        }

        double balance() {
            return Math.max(total() - amountPaid, 0);
        }
    }

    static final class Bill {
        String id = "";
        String name = "";
        String category = "Kodi ya pango";
        String payee = "";
        double amount = 0;
        String dueDate = Util.today();
        boolean paid = false;
        String paidDate = "";
        String method = "Benki";
        boolean recurring = false;
        String notes = "";
        String linkedTxnId;
        String createdAt = "";
    }

    static final class NotebookEntry {
        long id;
        String title = "";
        String content = "";
        boolean bookmarked;
        String updatedAt = "";
    }

    static final class SavingsGoal {
        String id = "";
        String title = "";
        double targetAmount = 0;
        double currentAmount = 0;
        String targetDate = "";
        String notes = "";
        String createdAt = "";
        String updatedAt = "";

        double progressRatio() {
            if (targetAmount <= 0) return 1.0;
            return Math.min(Math.max(currentAmount / targetAmount, 0.0), 1.0);
        }

        int progressPercent() {
            return (int) Math.round(progressRatio() * 100);
        }

        double remaining() {
            return Math.max(targetAmount - currentAmount, 0);
        }
    }

    static final class SavingsLog {
        String id = "";
        String savingsId = "";
        String type = "deposit"; // deposit | withdraw
        double amount = 0;
        String date = Util.today();
        String note = "";
        String createdAt = "";

        boolean deposit() {
            return "deposit".equals(type);
        }
    }
}
