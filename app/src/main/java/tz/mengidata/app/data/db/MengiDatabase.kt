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
        TransactionCategoryEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class MengiDatabase : RoomDatabase() {
    abstract fun settingsDao(): SettingsDao
    abstract fun transactionDao(): TransactionDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun billDao(): BillDao
    abstract fun savingsDao(): SavingsDao
    abstract fun categoryDao(): CategoryDao

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

        @Volatile
        private var INSTANCE: MengiDatabase? = null

        fun getInstance(context: Context): MengiDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MengiDatabase::class.java,
                    DB_NAME
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
