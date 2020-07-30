package com.rober.blogapp.entity

import androidx.room.Embedded
import androidx.room.Relation


data class UserWithBlogs(
    @Embedded val user: User,
    @Relation(
        parentColumn = "user_id",
        entityColumn = "user_creator_id"
    )
    val blogs: List<Blog>
)
{
}