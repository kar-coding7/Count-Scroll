package com.kiru.countscroll.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kiru.countscroll.data.local.dao.CountScrollDao
import com.kiru.countscroll.data.local.entity.DelayLog
import com.kiru.countscroll.data.local.entity.ScrollEvent
import com.kiru.countscroll.data.local.entity.UsageSession

@Database(
    entities = [ScrollEvent::class, UsageSession::class, DelayLog::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun countScrollDao(): CountScrollDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Template migrasi aman untuk perubahan skema berikutnya
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "count_scroll_db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
