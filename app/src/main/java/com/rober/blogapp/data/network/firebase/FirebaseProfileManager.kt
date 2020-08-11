package com.rober.blogapp.data.network.firebase

import com.rober.blogapp.data.ResultData
import com.rober.blogapp.entity.Post
import com.rober.blogapp.entity.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.Exception

class FirebaseProfileManager @Inject constructor(
    private val firebaseSource: FirebaseSource
) {

    var userPaginationLimit = 0
    var savedUserListPost : MutableList<Post> = mutableListOf()

    suspend fun retrieveProfileUserPosts(morePosts: Boolean): Flow<ResultData<List<Post>>> = flow {
        try{
            if(!morePosts && userPaginationLimit == 0){
                var userPosts = getProfileUserPostsLimit()

                emit(ResultData.Success(userPosts))
            }

            if(!morePosts && userPaginationLimit > 0){
                emit(ResultData.Success(savedUserListPost))
            }

            if(morePosts){
                var userPosts = getProfileUserPostsLimit()

                emit(ResultData.Success(userPosts))
            }
        }catch (e: Exception){
            emit(ResultData.Error(e, null))
        }

    }

    private suspend fun getProfileUserPostsLimit(): List<Post>{
        userPaginationLimit++

        val userPostsCollection = firebaseSource.db.collection("posts/${firebaseSource.username}/user_posts")

        val userPosts = userPostsCollection
            .limit((userPaginationLimit * 8).toLong())
            .get()
            .await()
            .toObjects(Post::class.java)

        savedUserListPost = userPosts

        return savedUserListPost
    }

    suspend fun getUserProfile(username: String): Flow<ResultData<User>> = flow  {
        emit(ResultData.Loading)
        var user: User? = null

        if(username == firebaseSource.username){
            user = firebaseSource.user
        }else{
            val userProfileRef = firebaseSource.db.collection("users").document(username)

            try{
                user = userProfileRef
                    .get()
                    .await()
                    .toObject(User::class.java)

            }catch (e: Exception) {
                emit(ResultData.Error(e, null))
            }
        }

        if(user == null){
            emit(ResultData.Error(Exception("We couldn't find the user")))
        }else{
            emit(ResultData.Success(user))
        }
    }
}