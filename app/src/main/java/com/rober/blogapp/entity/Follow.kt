package com.rober.blogapp.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "follows",
    foreignKeys = arrayOf(ForeignKey(
        entity=User::class,
        parentColumns = arrayOf("user_id"),
        childColumns = arrayOf("user_follow_id"),
        onDelete = ForeignKey.CASCADE)))
data class Follow(
    @PrimaryKey
    @ColumnInfo(name = "follow_id")
    val id: Long,
    @ColumnInfo(name="user_follow_id")
    val user_follow_id: Long,
    var users: List<User>?
) {
}