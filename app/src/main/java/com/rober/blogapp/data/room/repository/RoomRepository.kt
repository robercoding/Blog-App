package com.rober.blogapp.data.room.repository

//import com.rober.blogapp.data.room.dao.BlogDao
//import com.rober.blogapp.data.room.dao.UserDao
import com.rober.blogapp.entity.Post
import com.rober.blogapp.entity.User
//import com.rober.blogapp.entity.UserWithBlogs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.lang.Exception
import javax.inject.Inject

//class RoomRepository
//@Inject constructor(
//    private val userDao: UserDao,
//    private val blogDao: BlogDao
//){
//    suspend fun getAllUsers(): Flow<DataState<List<User>>> = flow {
//        emit(DataState.Loading)
//        kotlinx.coroutines.delay(1000)
//        try {
//            emit(DataState.Success(userDao.getUsers()))
//
//        }catch (e: Exception){
//            emit(DataState.Error(e))
//        }
//    }
//
//    suspend fun insertUser(user: User) = userDao.insertUser(user)
//
//    suspend fun deleteUser(user: User) = userDao.deleteUser(user)
//
//    suspend fun insertBlog(post: Post) = blogDao.insertBlog(post)

//    suspend fun getUsersWithBlogs(): Flow<DataState<List<UserWithBlogs>>> = flow {
//        emit(DataState.Loading)
//        kotlinx.coroutines.delay(1000)
//        emit(DataState.Success(userDao.getUsersWithBlogs()))
//    }
//    suspend fun getUserWithBlogs(userId: Long): Flow<DataState<UserWithBlogs>> = flow {
//        emit(DataState.Loading)
//        kotlinx.coroutines.delay(4000)
//        emit(DataState.Success(userDao.getUserWithBlogs(userId)))
//    }
//}