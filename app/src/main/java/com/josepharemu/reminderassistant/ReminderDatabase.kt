package com.josepharemu.reminderassistant

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Reminder::class,
        ReminderAction::class
    ],
    version = 5,
    exportSchema = false
)
abstract class ReminderDatabase : RoomDatabase() {

    abstract fun reminderDao(): ReminderDao

    abstract fun reminderActionDao(): ReminderActionDao

    companion object {

        @Volatile
        private var INSTANCE: ReminderDatabase? = null

        /*
         * Version 1 -> 2
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
         * Version 2 -> 3
         * Added recurrence anchor.
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

                    database.execSQL(
                        """
                        UPDATE reminders
                        SET repeatAnchorDate = date
                        WHERE repeatRule != 'None'
                        """.trimIndent()
                    )
                }
            }

        /*
         * Version 3 -> 4
         * Added intelligent reminder fields
         * and action history.
         */
        private val MIGRATION_3_4 =
            object : Migration(3, 4) {

                override fun migrate(
                    database: SupportSQLiteDatabase
                ) {

                    database.execSQL(
                        """
                        ALTER TABLE reminders
                        ADD COLUMN deadlineDate TEXT NOT NULL DEFAULT 'None'
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        ALTER TABLE reminders
                        ADD COLUMN deadlineTime TEXT NOT NULL DEFAULT 'None'
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        ALTER TABLE reminders
                        ADD COLUMN repeatUntilDate TEXT NOT NULL DEFAULT 'None'
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        ALTER TABLE reminders
                        ADD COLUMN followUpUntilCompleted INTEGER NOT NULL DEFAULT 0
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        ALTER TABLE reminders
                        ADD COLUMN escalationEnabled INTEGER NOT NULL DEFAULT 0
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        ALTER TABLE reminders
                        ADD COLUMN snoozeCount INTEGER NOT NULL DEFAULT 0
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        ALTER TABLE reminders
                        ADD COLUMN lastSnoozedAt INTEGER DEFAULT NULL
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS reminder_actions (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            reminderId INTEGER NOT NULL,
                            actionType TEXT NOT NULL,
                            timestamp INTEGER NOT NULL,
                            occurrenceDate TEXT,
                            details TEXT,
                            FOREIGN KEY(reminderId)
                                REFERENCES reminders(id)
                                ON UPDATE NO ACTION
                                ON DELETE CASCADE
                        )
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        index_reminder_actions_reminderId
                        ON reminder_actions(reminderId)
                        """.trimIndent()
                    )
                }
            }

        /*
         * Version 4 -> 5
         *
         * Repair migration.
         *
         * Some installations reached database version 4
         * before the Reminder entity contained all of the
         * new intelligent-reminder fields.
         *
         * This migration checks the actual database and
         * adds only columns that are missing. Existing
         * reminders are preserved.
         */
        private val MIGRATION_4_5 =
            object : Migration(4, 5) {

                override fun migrate(
                    database: SupportSQLiteDatabase
                ) {

                    val columns =
                        mutableSetOf<String>()

                    database
                        .query(
                            "PRAGMA table_info(reminders)"
                        )
                        .use { cursor ->

                            val nameIndex =
                                cursor.getColumnIndex(
                                    "name"
                                )

                            while (
                                cursor.moveToNext()
                            ) {

                                columns.add(
                                    cursor.getString(
                                        nameIndex
                                    )
                                )
                            }
                        }

                    fun addColumnIfMissing(
                        columnName: String,
                        sql: String
                    ) {

                        if (
                            columnName !in columns
                        ) {

                            database.execSQL(
                                sql
                            )

                            columns.add(
                                columnName
                            )
                        }
                    }

                    val anchorWasMissing =
                        "repeatAnchorDate" !in columns

                    addColumnIfMissing(
                        "repeatRule",
                        """
                        ALTER TABLE reminders
                        ADD COLUMN repeatRule TEXT NOT NULL DEFAULT 'None'
                        """.trimIndent()
                    )

                    addColumnIfMissing(
                        "repeatAnchorDate",
                        """
                        ALTER TABLE reminders
                        ADD COLUMN repeatAnchorDate TEXT NOT NULL DEFAULT 'None'
                        """.trimIndent()
                    )

                    addColumnIfMissing(
                        "deadlineDate",
                        """
                        ALTER TABLE reminders
                        ADD COLUMN deadlineDate TEXT NOT NULL DEFAULT 'None'
                        """.trimIndent()
                    )

                    addColumnIfMissing(
                        "deadlineTime",
                        """
                        ALTER TABLE reminders
                        ADD COLUMN deadlineTime TEXT NOT NULL DEFAULT 'None'
                        """.trimIndent()
                    )

                    addColumnIfMissing(
                        "repeatUntilDate",
                        """
                        ALTER TABLE reminders
                        ADD COLUMN repeatUntilDate TEXT NOT NULL DEFAULT 'None'
                        """.trimIndent()
                    )

                    addColumnIfMissing(
                        "followUpUntilCompleted",
                        """
                        ALTER TABLE reminders
                        ADD COLUMN followUpUntilCompleted INTEGER NOT NULL DEFAULT 0
                        """.trimIndent()
                    )

                    addColumnIfMissing(
                        "escalationEnabled",
                        """
                        ALTER TABLE reminders
                        ADD COLUMN escalationEnabled INTEGER NOT NULL DEFAULT 0
                        """.trimIndent()
                    )

                    addColumnIfMissing(
                        "snoozeCount",
                        """
                        ALTER TABLE reminders
                        ADD COLUMN snoozeCount INTEGER NOT NULL DEFAULT 0
                        """.trimIndent()
                    )

                    addColumnIfMissing(
                        "lastSnoozedAt",
                        """
                        ALTER TABLE reminders
                        ADD COLUMN lastSnoozedAt INTEGER DEFAULT NULL
                        """.trimIndent()
                    )

                    if (
                        anchorWasMissing
                    ) {

                        database.execSQL(
                            """
                            UPDATE reminders
                            SET repeatAnchorDate = date
                            WHERE repeatRule != 'None'
                            """.trimIndent()
                        )
                    }

                    database.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS reminder_actions (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            reminderId INTEGER NOT NULL,
                            actionType TEXT NOT NULL,
                            timestamp INTEGER NOT NULL,
                            occurrenceDate TEXT,
                            details TEXT,
                            FOREIGN KEY(reminderId)
                                REFERENCES reminders(id)
                                ON UPDATE NO ACTION
                                ON DELETE CASCADE
                        )
                        """.trimIndent()
                    )

                    database.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS
                        index_reminder_actions_reminderId
                        ON reminder_actions(reminderId)
                        """.trimIndent()
                    )
                }
            }

        fun getDatabase(
            context: Context
        ): ReminderDatabase {

            return INSTANCE
                ?: synchronized(this) {

                    val instance =
                        Room.databaseBuilder(
                            context.applicationContext,
                            ReminderDatabase::class.java,
                            "reminder_database"
                        )
                            .addMigrations(
                                MIGRATION_1_2,
                                MIGRATION_2_3,
                                MIGRATION_3_4,
                                MIGRATION_4_5
                            )
                            .build()

                    INSTANCE =
                        instance

                    instance
                }
        }
    }
}