package com.rober.blogapp.data.network.firebase

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.rober.blogapp.data.ResultData
import com.rober.blogapp.data.network.util.FirebasePath
import com.rober.blogapp.entity.CountsPosts
import com.rober.blogapp.entity.Post
import com.rober.blogapp.entity.User
import com.rober.blogapp.entity.UserDocumentUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.Exception

class FirebasePostAddManager
@Inject
constructor(
    private val firebaseSource: FirebaseSource
) {
    private var userDocumentUID: UserDocumentUID? = null


    suspend fun savePost(post: Post): Flow<ResultData<Unit>> = flow {
        emit(ResultData.Loading)
        if (isUserDocumentUIDNull()) {
            emit(ResultData.Error(Exception("User documents are null"), null))
            return@flow
        }

        val userPostsDocumentUID = userDocumentUID!!.postsDocumentUid

        val user: User = firebaseSource.getCurrentUser()

        if (user.user_id == "") {
            emit(ResultData.Error(Exception("Sorry, we had a problem with your user account"), null))
            return@flow
        }

        val postHashMap = hashMapOf<String, Any>(
            "postTitle" to post.title,
            "postText" to post.text,
            "postLikes" to post.likes,
            "postCreated_at" to post.created_at,
            "postUserCreatorId" to user.user_id,
            "userPostsDocUid" to userPostsDocumentUID
        )

        firebaseSource.functions
            .getHttpsCallable("addNewPost")
            .call(postHashMap)

        emit(ResultData.Success<Unit>())
    }

    private fun isUserDocumentUIDNull(): Boolean {
        if (userDocumentUID == null) {
            firebaseSource.userDocumentUID?.let {
                userDocumentUID = it
            } ?: kotlin.run {
                return true
            }
        }
        return false
    }
}