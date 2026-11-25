package tz.mengidata.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    @Query("SELECT * FROM business_settings WHERE id = 1")
    fun observeSettings(): Flow<BusinessSettingsEntity?>

    @Query("SELECT * FROM business_settings WHERE id = 1")
    suspend fun getSettings(): BusinessSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: BusinessSettingsEntity)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC, createdAt DESC, rowid DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY date DESC, createdAt DESC, rowid DESC")
    suspend fun getAll(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(txn: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(txns: List<TransactionEntity>)

    @Query("UPDATE transactions SET category = :newName WHERE category = :oldName AND type = :type")
    suspend fun renameCategory(oldName: String, newName: String, type: String)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}

@Dao
interface InvoiceDao {
    @Transaction
    @Query("SELECT * FROM invoices ORDER BY issueDate DESC, createdAt DESC, rowid DESC")
    fun observeAllWithItems(): Flow<List<InvoiceWithItems>>

    @Transaction
    @Query("SELECT * FROM invoices ORDER BY issueDate DESC, createdAt DESC, rowid DESC")
    suspend fun getAllWithItems(): List<InvoiceWithItems>

    @Transaction
    @Query("SELECT * FROM invoices WHERE id = :id")
    suspend fun getByIdWithItems(id: String): InvoiceWithItems?

    @Query("SELECT number FROM invoices WHERE number LIKE :prefixPattern")
    suspend fun getNumbersWithPrefix(prefixPattern: String): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInvoice(invoice: InvoiceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<InvoiceItemEntity>)

    @Query("DELETE FROM invoice_items WHERE invoiceId = :invoiceId")
    suspend fun deleteItemsForInvoice(invoiceId: String)

    @Query("DELETE FROM invoices WHERE id = :id")
    suspend fun deleteInvoiceById(id: String)

    @Query("DELETE FROM invoices")
    suspend fun deleteAll()
}

@Dao
interface BillDao {
    @Query("SELECT * FROM bills ORDER BY dueDate DESC, createdAt DESC, rowid DESC")
    fun observeAll(): Flow<List<BillEntity>>

    @Query("SELECT * FROM bills ORDER BY dueDate DESC, createdAt DESC, rowid DESC")
    suspend fun getAll(): List<BillEntity>

    @Query("SELECT * FROM bills WHERE id = :id")
    suspend fun getById(id: String): BillEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(bill: BillEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(bills: List<BillEntity>)

    @Query("DELETE FROM bills WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM bills")
    suspend fun deleteAll()
}

@Dao
interface SavingsDao {
    @Query("SELECT * FROM savings_goals ORDER BY createdAt DESC, rowid DESC")
    fun observeGoals(): Flow<List<SavingsGoalEntity>>

    @Query("SELECT * FROM savings_goals ORDER BY createdAt DESC, rowid DESC")
    suspend fun getGoals(): List<SavingsGoalEntity>

    @Query("SELECT * FROM savings_goals WHERE id = :id")
    suspend fun getGoalById(id: String): SavingsGoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGoal(goal: SavingsGoalEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGoals(goals: List<SavingsGoalEntity>)

    @Query("DELETE FROM savings_goals WHERE id = :id")
    suspend fun deleteGoalById(id: String)

    @Query("DELETE FROM savings_goals")
    suspend fun deleteAllGoals()

    @Query("SELECT * FROM savings_logs ORDER BY date DESC, createdAt DESC, rowid DESC")
    fun observeAllLogs(): Flow<List<SavingsLogEntity>>

    @Query("SELECT * FROM savings_logs ORDER BY date DESC, createdAt DESC, rowid DESC")
    suspend fun getAllLogs(): List<SavingsLogEntity>

    @Query("SELECT * FROM savings_logs WHERE savingsId = :goalId ORDER BY date DESC, createdAt DESC, rowid DESC")
    suspend fun getLogsForGoal(goalId: String): List<SavingsLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLog(log: SavingsLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLogs(logs: List<SavingsLogEntity>)

    @Query("DELETE FROM savings_logs")
    suspend fun deleteAllLogs()
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM transaction_categories ORDER BY isDefault DESC, name ASC")
    fun observeAll(): Flow<List<TransactionCategoryEntity>>

    @Query("SELECT * FROM transaction_categories ORDER BY isDefault DESC, name ASC")
    suspend fun getAll(): List<TransactionCategoryEntity>

    @Query("SELECT * FROM transaction_categories WHERE id = :id")
    suspend fun getById(id: String): TransactionCategoryEntity?

    @Query("SELECT COUNT(*) FROM transaction_categories")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(category: TransactionCategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(categories: List<TransactionCategoryEntity>)

    @Query("DELETE FROM transaction_categories WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM transaction_categories")
    suspend fun deleteAll()
}

@Dao
interface DeletedRecordDao {
    @Query("SELECT * FROM deleted_records ORDER BY deletedAtMillis DESC, rowid DESC")
    fun observeAll(): Flow<List<DeletedRecordEntity>>

    @Query("SELECT * FROM deleted_records ORDER BY deletedAtMillis DESC, rowid DESC")
    suspend fun getAll(): List<DeletedRecordEntity>

    @Query("SELECT * FROM deleted_records WHERE id = :id")
    suspend fun getById(id: String): DeletedRecordEntity?

    @Query("SELECT COUNT(*) FROM deleted_records")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: DeletedRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(records: List<DeletedRecordEntity>)

    @Query("DELETE FROM deleted_records WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM deleted_records")
    suspend fun deleteAll()
}


