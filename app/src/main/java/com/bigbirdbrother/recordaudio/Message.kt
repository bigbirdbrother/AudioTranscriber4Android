package com.bigbirdbrother.recordaudio

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "messages")
class Message {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0

    var content: String? = null
    var isUser: Boolean = false // true=用户消息, false=系统回复
    var timestamp: Long = 0
    var isBookmarked: Boolean = false
    var ref_id: Int = -1
} // MessageDao.java (数据访问对象)