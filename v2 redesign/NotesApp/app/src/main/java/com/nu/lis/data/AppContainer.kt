package com.nu.lis.data

import android.content.Context
import androidx.room.Room

// Manual dependency injection — tidak perlu Hilt, lebih simpel dan stabil
object AppContainer {
    private var db: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return db ?: synchronized(this) {
            db ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "notes.db"
            )
            .addMigrations(
                MIGRATION_1_2,
                MIGRATION_9_10,
                MIGRATION_10_11,
                MIGRATION_11_12
            )
            .build()
            .also { db = it }
        }
    }

    fun closeDatabase() {
        db?.close()
        db = null
    }
}

