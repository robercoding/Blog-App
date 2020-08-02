package com.rober.blogapp.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.rober.blogapp.entity.Post

//@Dao
//interface BlogDao {
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun insertBlog(post: Post): Long
//}