package com.rober.blogapp.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "blogs",
    foreignKeys = arrayOf(ForeignKey(
        entity = User::class,
        parentColumns = arrayOf("user_id"),
        childColumns = arrayOf("user_creator_id"),
        onDelete = ForeignKey.CASCADE
    )))

data class Blog (
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "blog_id")
    var blog_id: Long,
    @ColumnInfo(name="user_creator_id")
    val user_creator_id: Long,
    var title: String,
    var text: String,
    var likes: Int
){
}