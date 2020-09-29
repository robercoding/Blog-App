package com.rober.blogapp.data.network.firebase

import android.util.Log
import com.rober.blogapp.data.ResultData
import com.rober.blogapp.data.network.util.FirebasePath
import com.rober.blogapp.entity.Post
import com.rober.blogapp.entity.ReportPost
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirebasePostDetailManager @Inject constructor(
    private val firebaseSource: FirebaseSource,
    private val firebasePath: FirebasePath
) {
    fun reportPost(post: Post, reportedCause: String, message: String): Flow<ResultData<Boolean>> = flow {
        emit(ResultData.Loading)

        val user = firebaseSource.user

        if(user == null){
            emit(ResultData.Error(Exception("We couldn't report the post sorry"), false))
            return@flow
        }

        val postDocumentRef = firebaseSource.db.collection(firebasePath.reports_col)
            .document(user.user_id).collection(firebasePath.posts_reports).document()

        val reportPostObject = ReportPost(post.postId, user.user_id, reportedCause, message)
        var savedReport = false
        postDocumentRef.set(reportPostObject)
            .addOnSuccessListener {
                savedReport = true
            }.await()

        emit(ResultData.Success(savedReport))
    }

    suspend fun deletePost(post: Post): Flow<ResultData<Boolean>> = flow {
        emit(ResultData.Loading)
        val postDocumentUID = firebaseSource.userDocumentUID?.postsDocumentUid

        if (postDocumentUID == null) {
            emit(ResultData.Error(Exception("Post couldn't be deleted"), false))
            return@flow
        }

        val postDocumentReference = firebaseSource.db.collection(firebasePath.posts_col)
            .document(postDocumentUID)
            .collection(firebasePath.user_posts)
            .document(post.postId)

        var deleted = false
        postDocumentReference
            .delete()
            .addOnSuccessListener {
                deleted = true
            }
            .await()

        emit(ResultData.Success(deleted))
    }
}