package com.rober.blogapp.data.room.dao

import androidx.room.*
import com.rober.blogapp.entity.User
//import com.rober.blogapp.entity.UserWithBlogs
//
//@Dao
//interface UserDao {

//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun insertUser(user: User): Long
//
//    @Query("SELECT * FROM users")
//    suspend fun getUsers(): List<User>

//    @Transaction
//    @Query("SELECT * FROM users")
//    suspend fun getUsersWithBlogs(): List<UserWithBlogs>
//
//    @Transaction
//    @Query("SELECT * FROM users WHERE userId =:userId ")
//    suspend fun getUserWithBlogs(userId: Long): UserWithBlogs

//    @Delete
//    suspend fun deleteUser(user:User): Int
//}