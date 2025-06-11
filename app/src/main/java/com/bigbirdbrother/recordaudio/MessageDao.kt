package com.bigbirdbrother.recordaudio

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update


@Dao
interface MessageDao {
    @Insert
    fun insert(message: Message)

    @Update
    fun update(message: Message)

    @Delete
    fun delete(message: Message)

    // 按ID删除
    @Query("DELETE FROM messages WHERE id = :id")
    fun deleteById(id: Int): Int

    @Query("SELECT * FROM messages WHERE id = :id")
    fun getById(id: Int): Message?

    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    fun getAllMessages(): List<Message>

    @Query("SELECT * FROM messages where isBookmarked='true' ORDER BY timestamp ASC")
    fun getMarkMessages(): List<Message>
}