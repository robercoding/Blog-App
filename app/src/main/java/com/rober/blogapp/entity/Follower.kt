package com.rober.blogapp.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "followers")
data class Follower(
    @PrimaryKey
    val follower_id: Long,
    var followers: List<User>
) {
}