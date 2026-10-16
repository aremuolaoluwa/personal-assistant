package com.josepharemu.reminderassistant

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderActionDao {

    @Insert
    suspend fun insertAction(
        action: ReminderAction
    ): Long

    @Query(
        """
        SELECT * FROM reminder_actions
        ORDER BY timestamp DESC
        """
    )
    fun getAllActions():
            Flow<List<ReminderAction>>

    @Query(
        """
        SELECT * FROM reminder_actions
        WHERE reminderId = :reminderId
        ORDER BY timestamp DESC
        """
    )
    fun getActionsForReminder(
        reminderId: Int
    ): Flow<List<ReminderAction>>

    @Query(
        """
        SELECT COUNT(*) FROM reminder_actions
        WHERE reminderId = :reminderId
        AND actionType = :actionType
        """
    )
    suspend fun countActions(
        reminderId: Int,
        actionType: String
    ): Int

    @Query(
        """
        DELETE FROM reminder_actions
        WHERE reminderId = :reminderId
        """
    )
    suspend fun deleteActionsForReminder(
        reminderId: Int
    )
}