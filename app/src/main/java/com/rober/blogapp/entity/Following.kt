package com.rober.blogapp.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity
data class Following(
    @PrimaryKey
    val following_id: String
    /*@ColumnInfo(name="user_follow_id")
    val user_follow_id: Long,
    var users: List<User>?*/
) {
    constructor() : this("")
}