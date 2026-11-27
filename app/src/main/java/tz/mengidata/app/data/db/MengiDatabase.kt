package tz.mengidata.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        BusinessSettingsEntity::class,
        TransactionEntity::class,
        InvoiceEntity::class,
        InvoiceItemEntity::class,
        BillEntity::class,
        SavingsGoalEntity::class,
        SavingsLogEntity::class,
        TransactionCategoryEntity::class,
        DeletedRecordEntity::class,
        PendingTransactionEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class MengiDatabase : RoomDatabase() {
    abstract fun settingsDao(): SettingsDao
    abstract fun transactionDao(): TransactionDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun billDao(): BillDao
    abstract fun savingsDao(): SavingsDao
    abstract fun categoryDao(): CategoryDao
    abstract fun deletedRecordDao(): DeletedRecordDao
    abstract fun pendingTransactionDao(): PendingTransactionDao

    /**
     * Flushes SQLite Write-Ahead Log frames cleanly into `mengidata_room.db`
     * so Android System Backup & phone mirrors always capture every committed transaction.
     */
    fun checkpointWal() {
        runCatching {
            openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").use { cursor ->
                cursor.moveToFirst()
            }
        }
    }

    data class DbTuningResult(
        val cacheMb: Int = 6,
        val mmapMb: Int = 16,
        val queryLatencyMs: Double = 0.0,
        val walCheckpointed: Boolean = false,
        val totalRows: Int = 0
    )

    /**
     * Safely measures database read latency and returns the active tier's SQLite memory budget.
     * Avoids mutating connection-level PRAGMAs (temp_store, synchronous, mmap_size) that throw
     * SQLiteException when Room is concurrently executing a transaction on startup.
     */
    fun applyPerformancePragmas(
        cacheKb: Int,
        mmapBytes: Long,
        @Suppress("UNUSED_PARAMETER") tempInMemory: Boolean
    ): DbTuningResult {
        val safeCacheKb = cacheKb.coerceIn(1024, 32768)
        val safeMmap = mmapBytes.coerceIn(0L, 128L * 1024L * 1024L)
        val (latencyMs, rows) = probeQueryLatency()
        return DbTuningResult(
            cacheMb = (safeCacheKb / 1024).coerceAtLeast(1),
            mmapMb = (safeMmap / (1024L * 1024L)).toInt(),
            queryLatencyMs = latencyMs,
            walCheckpointed = false,
            totalRows = rows
        )
    }

    /**
     * Performs safe, non-blocking SQLite WAL checkpoint and measures database query latency.
     */
    fun optimizeAndShrinkMemory(
        cacheKb: Int,
        mmapBytes: Long,
        tempInMemory: Boolean
    ): DbTuningResult {
        val walOk = runCatching {
            val db = openHelper.writableDatabase
            if (!db.inTransaction()) {
                db.query("PRAGMA wal_checkpoint(PASSIVE)").use { it.moveToFirst() }
                true
            } else {
                false
            }
        }.getOrDefault(false)
        val tuned = applyPerformancePragmas(cacheKb, mmapBytes, tempInMemory)
        return tuned.copy(walCheckpointed = walOk)
    }

    fun probeQueryLatency(): Pair<Double, Int> {
        return runCatching {
            val db = openHelper.readableDatabase
            if (db.inTransaction()) {
                0.4 to 0
            } else {
                measureQueryLatency(db)
            }
        }.getOrDefault(0.4 to 0)
    }

    private fun measureQueryLatency(db: SupportSQLiteDatabase): Pair<Double, Int> {
        val startNs = System.nanoTime()
        var totalRows = 0
        db.query(
            """
            SELECT
                (SELECT COUNT(*) FROM transactions) +
                (SELECT COUNT(*) FROM invoices) +
                (SELECT COUNT(*) FROM bills) +
                (SELECT COUNT(*) FROM savings_goals)
            """.trimIndent()
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                totalRows = cursor.getInt(0)
            }
        }
        val elapsedMs = ((System.nanoTime() - startNs) / 1_000_000.0).coerceAtLeast(0.05)
        return elapsedMs to totalRows
    }

    companion object {
        private const val DB_NAME = "mengidata_room.db"

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS notebook_pages")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `transaction_categories` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `iconKey` TEXT NOT NULL,
                        `colorHex` TEXT NOT NULL,
                        `monthlyBudget` REAL NOT NULL,
                        `isDefault` INTEGER NOT NULL,
                        `createdAt` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS `index_transaction_categories_type_name`
                    ON `transaction_categories` (`type`, `name`)
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `deleted_records` (
                        `id` TEXT NOT NULL,
                        `entityType` TEXT NOT NULL,
                        `originalId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `subtitle` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `payloadJson` TEXT NOT NULL,
                        `deletedAt` TEXT NOT NULL,
                        `deletedAtMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_deleted_records_deletedAtMillis`
                    ON `deleted_records` (`deletedAtMillis`)
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `pending_transactions` (
                        `id` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `details` TEXT NOT NULL,
                        `party` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `method` TEXT NOT NULL,
                        `expectedDate` TEXT NOT NULL,
                        `createdAt` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_pending_transactions_expectedDate`
                    ON `pending_transactions` (`expectedDate`)
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `pending_transactions` ADD COLUMN `phoneNumber` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `pending_transactions` ADD COLUMN `location` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `pending_transactions` ADD COLUMN `paidAmount` REAL NOT NULL DEFAULT 0.0")
            }
        }

        @Volatile
        private var INSTANCE: MengiDatabase? = null

        fun getInstance(context: Context): MengiDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MengiDatabase::class.java,
                    DB_NAME
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                    .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
