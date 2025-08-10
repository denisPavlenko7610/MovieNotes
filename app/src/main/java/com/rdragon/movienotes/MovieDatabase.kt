package com.rdragon.movienotes

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Movie::class], version = 2)
abstract class MovieDatabase : RoomDatabase() {
    abstract fun movieDao(): MovieDao

    companion object {
        @Volatile private var INSTANCE: MovieDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE movies ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getInstance(context: Context): MovieDatabase {
            return INSTANCE ?: synchronized(this) {
                val inst = Room.databaseBuilder(
                    context.applicationContext,
                    MovieDatabase::class.java,
                    "movie_db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()

                INSTANCE = inst

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        inst.movieDao().fixCreatedAt(System.currentTimeMillis())
                    } catch (e: Exception) {
                    }
                }

                inst
            }
        }
    }
}
