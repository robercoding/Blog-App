package com.rober.blogapp.data.network.firebase

import com.rober.blogapp.data.ResultData
import com.rober.blogapp.data.network.util.FirebasePath
import com.rober.blogapp.entity.Comment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirebasePostReplyManager @Inject constructor(
    val firebaseSource: FirebaseSource,
    val firebasePath: FirebasePath
) {

    suspend fun getCommentRepliesById(commentId: String): Flow<ResultData<List<Comment>>> = flow {
        val collectionCommentRef = firebaseSource.db.collection(firebasePath.comments_col).document(commentId)
            .collection(firebasePath.comments_comments)


        var success = false
        var listComments = mutableListOf<Comment>()
        collectionCommentRef
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    success = false
                    return@addOnSuccessListener
                }

                success = true
                listComments = querySnapshot.toObjects(Comment::class.java).toMutableList()

            }
            .addOnFailureListener {
                success = false
            }
            .await()

        if (success) {
            emit(ResultData.Success(listComments))
        } else {
            emit(ResultData.Error(Exception("There's no more replies."), listComments))
        }
    }

    suspend fun addReplyToComment(comment: Comment): Flow<ResultData<Boolean>> = flow {
        emit(ResultData.Loading)
        val documentComment =
            firebaseSource.db.collection(firebasePath.comments_col).document(comment.replyToldId)
                .collection(firebasePath.comments_comments).document()

        comment.commentId = documentComment.id
        var success = false
        documentComment.set(comment)
            .addOnSuccessListener {
                success = true
            }
            .addOnFailureListener {
                success = false
            }
            .await()

        if (success)
            emit(ResultData.Success(success))
        else
            emit(
                ResultData.Error(
                    Exception("There was an error when replying the comment, try again later!"),
                    false
                )
            )
    }
}