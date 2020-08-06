package com.rober.blogapp.data.network.firebase

import com.rober.blogapp.data.ResultData
import com.rober.blogapp.entity.Post
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.lang.Exception
import javax.inject.Inject

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
}