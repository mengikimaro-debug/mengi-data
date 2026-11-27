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
                    .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
