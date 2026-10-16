package com.josepharemu.reminderassistant

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Reminder::class],
    version = 3,
    exportSchema = false
)
abstract class ReminderDatabase : RoomDatabase() {

    abstract fun reminderDao(): ReminderDao

    companion object {

        @Volatile
        private var INSTANCE: ReminderDatabase? = null

        /*
         * Previous migration:
         * database version 1 → version 2
         *
         * Added repeatRule.
         */
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

        /*
         * New migration:
         * database version 2 → version 3
         *
         * Adds an anchor date so Monthly and Yearly
         * reminders can preserve their intended date.
         */
        private val MIGRATION_2_3 =
            object : Migration(2, 3) {

                override fun migrate(
                    database: SupportSQLiteDatabase
                ) {

                    database.execSQL(
                        """
                        ALTER TABLE reminders
                        ADD COLUMN repeatAnchorDate TEXT NOT NULL DEFAULT 'None'
                        """.trimIndent()
                    )

                    /*
                     * Existing recurring reminders did not previously
                     * have an anchor date.
                     *
                     * Their current date becomes their initial anchor.
                     */
                    database.execSQL(
                        """
                        UPDATE reminders
                        SET repeatAnchorDate = date
                        WHERE repeatRule != 'None'
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
                            MIGRATION_1_2,
                            MIGRATION_2_3
                        )
                        .build()

                INSTANCE = instance

                instance
            }
        }
    }
}