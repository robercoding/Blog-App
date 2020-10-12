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

        if (user == null) {
            emit(ResultData.Error(Exception("We couldn't report the post due to an error, sorry."), false))
            return@flow
        }

        if (checkIfUserAlreadyReportedThePost(post.postId, user.user_id)) {
            emit(
                ResultData.Error(
                    Exception("Thank you for submitting a report, but you already reported this post!"),
                    false
                )
            )
            return@flow
        }

        val postDocumentRef = firebaseSource.db.collection(firebasePath.reports_col)
            .document(user.user_id).collection(firebasePath.posts_reports).document()

        val reportPostObject =
            ReportPost(post.postId, post.userCreatorId, user.user_id, reportedCause, message)
        var savedReport = false
        postDocumentRef.set(reportPostObject)
            .addOnSuccessListener {
                savedReport = true
            }.await()


        if (savedReport) {
            emit(ResultData.Success(savedReport))
        } else {
            emit(ResultData.Error(Exception("There was an error reporting the post, try again later"), false))
        }
    }

    private suspend fun checkIfUserAlreadyReportedThePost(postID: String, userID: String): Boolean {

        val reportUserCollection =
            firebaseSource.db.collection(firebasePath.reports_col).document(userID)
                .collection(firebasePath.posts_reports)

        var reportedPost = false
        reportUserCollection.whereEqualTo("reportedPostId", postID)
            .get()
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    reportedPost = false
                }

                task.result?.run {
                    if (documents.size >= 1) {
                        reportedPost = true
                    }
                } ?: kotlin.run {
                    reportedPost = false
                }
            }
            .await()

        return reportedPost
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

        if (deleted) {
            firebaseSource.listPostsDeleted.add(post)
        }
        emit(ResultData.Success(deleted))
    }

    suspend fun updateEditedPost(post: Post): Flow<ResultData<Boolean>> = flow {
        val user = firebaseSource.user
        val postDocumentUID = firebaseSource.userDocumentUID?.postsDocumentUid

        if (postDocumentUID == null || user == null) {
            emit(ResultData.Error(Exception("Post couldn't be updated"), false))
            return@flow
        }
        val mapPostUpdate = mapOf(
            "postID" to post.postId,
            "text" to post.text,
            "title" to post.title,
            "postDocumentUID" to postDocumentUID
        )

        var exception = Exception("There was an error in our servers")
        var postUpdated = false

        firebaseSource.functions
            .getHttpsCallable("updatePost")
            .call(mapPostUpdate)
            .continueWith { task ->
                try {
                    if (task.isSuccessful) {
                        val resultTask = task.result
                            ?: throw Exception("There was an error when trying to update the post")

                        postUpdated = resultTask.data as Boolean
                    } else {
                        exception = task.exception
                            ?: throw Exception("There was an error when trying to update the post")
                    }
                } catch (e: Exception) {
                    exception = e
                }
            }.await()

        if (!postUpdated) {
            emit(ResultData.Error(exception))
            return@flow
        }

        emit(ResultData.Success(postUpdated))

        //Update post from client
//        val postDocumentReference = firebaseSource.db
//            .collection(firebasePath.posts_col).document(postDocumentUID)
//            .collection(firebasePath.user_posts).document(post.postId)


//        postDocumentReference.update(mapPostUpdate)
//            .addOnSuccessListener { postUpdated = true }
//            .await()
    }

    suspend fun getPost(reportedPost: ReportPost): Flow<ResultData<Post>> = flow {
        val postsDocumentUID =
            firebaseSource.getUserDocumentUID(reportedPost.userIdOwnerReportedPost)?.postsDocumentUid

        if (postsDocumentUID == null) {
            emit(ResultData.Error(Exception("There was an error with the user"), null))
            return@flow
        }

        val postDocument = firebaseSource.db
            .collection(firebasePath.posts_col).document(postsDocumentUID)
            .collection(firebasePath.user_posts).document(reportedPost.reportedPostId)

        var post: Post? = null
        postDocument
            .get()
            .addOnSuccessListener {
                post = it.toObject(Post::class.java)
            }
            .await()

        post?.run {
            emit(ResultData.Success(this))
        } ?: kotlin.run {
            emit(ResultData.Error(Exception("We couldn't get the post, sorry"), null))
        }
    }
}