package tz.mengidata.app.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import tz.mengidata.app.i18n.FormatUtils
import kotlin.math.max
import kotlin.math.min

@Entity(tableName = "business_settings")
data class BusinessSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val businessName: String = "VaultLedger",
    val ownerName: String = "",
    val tagline: String = "Dashibodi ya biashara na fedha",
    val address: String = "",
    val phone: String = "",
    val email: String = "",
    val tin: String = "",
    val website: String = "",
    val invoicePrefix: String = "MD",
    val vatRate: Double = 0.0,
    val paymentDetails: String = "",
    val invoiceNote: String = "Asante kwa kufanya biashara nasi.",
    val currency: String = "TZS",
    val demo: Boolean = true
)

@Entity(
    tableName = "transactions",
    indices = [Index(value = ["date"])]
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val type: String, // "income" | "expense"
    val date: String, // YYYY-MM-DD
    val description: String,
    val party: String = "",
    val category: String = "Mauzo ya bidhaa",
    val amount: Double = 0.0,
    val method: String = "Cash",
    val reference: String = "",
    val notes: String = "",
    val source: String = "manual", // "manual" | "invoice" | "bill"
    val linkedId: String? = null,
    val createdAt: String = FormatUtils.nowIsoTimestamp()
) {
    val isIncome: Boolean get() = type == "income"
}

@Entity(
    tableName = "invoices",
    indices = [Index(value = ["issueDate"])]
)
data class InvoiceEntity(
    @PrimaryKey val id: String,
    val number: String,
    val customerName: String,
    val customerPhone: String = "",
    val customerEmail: String = "",
    val customerAddress: String = "",
    val issueDate: String = FormatUtils.todayIso(),
    val dueDate: String = FormatUtils.addDaysIso(FormatUtils.todayIso(), 14),
    val discount: Double = 0.0,
    val vatRate: Double = 0.0,
    val amountPaid: Double = 0.0,
    val state: String = "draft", // "draft" | "issued"
    val paidDate: String = "",
    val notes: String = "",
    val linkedTxnId: String? = null,
    val createdAt: String = FormatUtils.nowIsoTimestamp(),
    val updatedAt: String = FormatUtils.nowIsoTimestamp()
)

@Entity(
    tableName = "invoice_items",
    foreignKeys = [
        ForeignKey(
            entity = InvoiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["invoiceId"])]
)
data class InvoiceItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceId: String,
    val description: String,
    val qty: Double = 1.0,
    val price: Double = 0.0,
    val position: Int = 0
) {
    val lineTotal: Double get() = qty * price
}

data class InvoiceWithItems(
    @Embedded val invoice: InvoiceEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "invoiceId"
    )
    val items: List<InvoiceItemEntity>
) {
    val orderedItems: List<InvoiceItemEntity>
        get() = items.sortedBy { it.position }

    val subtotal: Double
        get() = items.sumOf { it.lineTotal }

    val effectiveDiscount: Double
        get() = min(max(invoice.discount, 0.0), subtotal)

    val vatAmount: Double
        get() = (subtotal - effectiveDiscount) * max(invoice.vatRate, 0.0) / 100.0

    val total: Double
        get() = subtotal - effectiveDiscount + vatAmount

    val balance: Double
        get() = max(total - invoice.amountPaid, 0.0)

    val isDraft: Boolean
        get() = invoice.state == "draft"

    val isPaid: Boolean
        get() = !isDraft && total > 0.0 && balance <= 0.005

    val isPartial: Boolean
        get() = !isDraft && invoice.amountPaid > 0.005 && balance > 0.005

    val isOverdue: Boolean
        get() = !isDraft && balance > 0.005 && FormatUtils.isOverdue(invoice.dueDate)
}

@Entity(
    tableName = "bills",
    indices = [Index(value = ["dueDate"])]
)
data class BillEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String = "Kodi ya pango",
    val payee: String = "",
    val amount: Double = 0.0,
    val dueDate: String = FormatUtils.todayIso(),
    val paid: Boolean = false,
    val paidDate: String = "",
    val method: String = "Benki",
    val recurring: Boolean = false,
    val notes: String = "",
    val linkedTxnId: String? = null,
    val createdAt: String = FormatUtils.nowIsoTimestamp()
) {
    val isOverdue: Boolean get() = !paid && FormatUtils.isOverdue(dueDate)
}

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey val id: String,
    val title: String,
    val targetAmount: Double = 0.0,
    val currentAmount: Double = 0.0,
    val targetDate: String = "",
    val notes: String = "",
    val createdAt: String = FormatUtils.nowIsoTimestamp(),
    val updatedAt: String = FormatUtils.nowIsoTimestamp()
) {
    val progressRatio: Float
        get() = if (targetAmount <= 0.0) 1f else (currentAmount / targetAmount).toFloat().coerceIn(0f, 1f)

    val progressPercent: Int
        get() = (progressRatio * 100f).toInt()

    val remaining: Double
        get() = max(targetAmount - currentAmount, 0.0)
}

@Entity(
    tableName = "savings_logs",
    foreignKeys = [
        ForeignKey(
            entity = SavingsGoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["savingsId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["savingsId"]), Index(value = ["date"])]
)
data class SavingsLogEntity(
    @PrimaryKey val id: String,
    val savingsId: String,
    val type: String = "deposit", // "deposit" | "withdraw"
    val amount: Double = 0.0,
    val date: String = FormatUtils.todayIso(),
    val note: String = "",
    val createdAt: String = FormatUtils.nowIsoTimestamp()
) {
    val isDeposit: Boolean get() = type == "deposit"
}

@Entity(
    tableName = "transaction_categories",
    indices = [Index(value = ["type", "name"], unique = true)]
)
data class TransactionCategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String, // "income" | "expense"
    val iconKey: String = "tag",
    val colorHex: String = "#2563EB",
    val monthlyBudget: Double = 0.0,
    val isDefault: Boolean = false,
    val createdAt: String = FormatUtils.nowIsoTimestamp()
) {
    val isIncome: Boolean get() = type == "income"
}

@Entity(
    tableName = "deleted_records",
    indices = [Index(value = ["deletedAtMillis"])]
)
data class DeletedRecordEntity(
    @PrimaryKey val id: String,
    val entityType: String, // "transaction" | "bill" | "invoice" | "savings_goal"
    val originalId: String,
    val title: String,
    val subtitle: String = "",
    val amount: Double = 0.0,
    val payloadJson: String,
    val deletedAt: String = FormatUtils.todayIso(),
    val deletedAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "pending_transactions",
    indices = [Index(value = ["expectedDate"])]
)
data class PendingTransactionEntity(
    @PrimaryKey val id: String,
    val type: String = "expense", // "income" | "expense"
    val description: String,
    val details: String = "",
    val party: String = "",
    val phoneNumber: String = "",
    val location: String = "",
    val category: String = "Mengineyo",
    val amount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val method: String = "M-Pesa",
    val expectedDate: String = FormatUtils.todayIso(),
    val createdAt: String = FormatUtils.nowIsoTimestamp()
) {
    val isIncome: Boolean get() = type == "income"
    val isOverdue: Boolean get() = !isFullyPaid && FormatUtils.isOverdue(expectedDate)

    val effectivePaidAmount: Double
        get() = paidAmount.coerceIn(0.0, max(amount, 0.0))

    val remainingAmount: Double
        get() = max(amount - effectivePaidAmount, 0.0)

    val isFullyPaid: Boolean
        get() = amount > 0.0 && remainingAmount <= 0.005

    val isPartiallyPaid: Boolean
        get() = effectivePaidAmount > 0.005 && remainingAmount > 0.005

    val isUnpaid: Boolean
        get() = effectivePaidAmount <= 0.005

    val paymentProgressRatio: Float
        get() = if (amount <= 0.0) 0f else (effectivePaidAmount / amount).toFloat().coerceIn(0f, 1f)
}


