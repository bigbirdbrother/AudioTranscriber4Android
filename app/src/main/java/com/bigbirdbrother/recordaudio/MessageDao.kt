package com.bigbirdbrother.recordaudio

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query


@Dao
interface MessageDao {
    @Insert
    fun insert(message: Message)

    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    fun getAllMessages(): List<Message>

}