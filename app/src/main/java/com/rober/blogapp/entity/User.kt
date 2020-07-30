package com.rober.blogapp.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "users", indices = arrayOf(Index(value = ["username"], unique = true)))
data class User(
    @PrimaryKey(autoGenerate = true)
    val user_id: Long,
    val username: String,
    val location: String?
) {
}