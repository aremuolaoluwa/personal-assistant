package com.josepharemu.reminderassistant

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Reminder::class],
    version = 2,
    exportSchema = false
)
abstract class ReminderDatabase : RoomDatabase() {

    abstract fun reminderDao(): ReminderDao

    companion object {

        @Volatile
        private var INSTANCE: ReminderDatabase? = null

        private val MIGRATION_1_2 =
            object : Migration(1, 2) {

                override fun migrate(
                    database: SupportSQLiteDatabase
                ) {

                    database.execSQL(
                        """
                        ALTER TABLE reminders
                        ADD COLUMN repeatRule TEXT NOT NULL DEFAULT 'None'
                        """.trimIndent()
                    )
                }
            }

        fun getDatabase(
            context: Context
        ): ReminderDatabase {

            return INSTANCE ?: synchronized(this) {

                val instance =
                    Room.databaseBuilder(
                        context.applicationContext,
                        ReminderDatabase::class.java,
                        "reminder_database"
                    )
                        .addMigrations(
                            MIGRATION_1_2
                        )
                        .build()

                INSTANCE = instance

                instance
            }
        }
    }
}