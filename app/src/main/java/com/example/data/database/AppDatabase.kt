package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.*
import com.example.data.model.*

@Database(
    entities = [
        AppSettings::class,
        Customer::class,
        Product::class,
        Invoice::class,
        InvoiceLineItem::class,
        Payment::class,
        AuditLog::class,
        BankAccount::class,
        Project::class
    ],
    version = 17,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun customerDao(): CustomerDao
    abstract fun productDao(): ProductDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun invoiceLineItemDao(): InvoiceLineItemDao
    abstract fun paymentDao(): PaymentDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun bankAccountDao(): BankAccountDao
    abstract fun projectDao(): ProjectDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "invoice_builder_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
