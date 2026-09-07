package com.personalnotes.data

import android.content.Context
import androidx.room.Room

// Manual dependency injection — tidak perlu Hilt, lebih simpel dan stabil
object AppContainer {
    private var db: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return db ?: Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "notes.db"
        ).build().also { db = it }
    }
}
