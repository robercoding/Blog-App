package com.rober.blogapp.data.network.firebase

import com.rober.blogapp.data.ResultData
import com.rober.blogapp.entity.Post
import com.rober.blogapp.entity.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import kotlin.Exception

class FirebasePostAddManager
@Inject
constructor(
    private val firebaseSource: FirebaseSource
) {

    suspend fun savePost(post: Post): Flow<ResultData<Unit>> = flow {
        emit(ResultData.Loading)
        val currentUserId = firebaseSource.userId

        if (currentUserId.isEmpty())
            throw Exception("Couldn't get userId in function deletePost")

        val user: User = firebaseSource.getCurrentUser()

        if (user.userId == "") {
            emit(ResultData.Error(Exception("Sorry, we had a problem with your user account"), null))
            return@flow
        }

        val postHashMap = hashMapOf<String, Any>(
            "postTitle" to post.title,
            "postText" to post.text,
            "postLikes" to post.likes,
            "postCreated_at" to post.createdAt,
            "postUserCreatorId" to user.userId
        )

        firebaseSource.functions
            .getHttpsCallable("addNewPost")
            .call(postHashMap)

        emit(ResultData.Success())
    }
}