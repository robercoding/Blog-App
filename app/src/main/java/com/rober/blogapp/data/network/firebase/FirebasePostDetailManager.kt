package com.rober.blogapp.data.network.firebase

import android.util.Log
import com.rober.blogapp.data.ResultData
import com.rober.blogapp.data.network.util.FirebasePath
import com.rober.blogapp.entity.Comment
import com.rober.blogapp.entity.Post
import com.rober.blogapp.entity.ReportPost
import com.rober.blogapp.entity.User
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

        if (checkIfUserAlreadyReportedThePost(post.postId, user.userId)) {
            emit(
                ResultData.Error(
                    Exception("Thank you for submitting a report, but you already reported this post!"),
                    false
                )
            )
            return@flow
        }

        val postDocumentRef = firebaseSource.db.collection(firebasePath.reports_col)
            .document(user.userId).collection(firebasePath.posts_reports).document()

        val reportPostObject =
            ReportPost(post.postId, post.userCreatorId, user.userId, reportedCause, message)
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
        val currentUserId = firebaseSource.userId

        if (currentUserId.isEmpty())
            throw Exception("Couldn't get userId in function deletePost")

        val postDocumentReference = firebaseSource.db.collection(firebasePath.posts_col)
            .document(currentUserId)
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
        val currentUserId = firebaseSource.userId

        if (currentUserId.isEmpty())
            throw Exception("Couldn't get userId in function updateEditedPost")

        val mapPostUpdate = mapOf(
            "postID" to post.postId,
            "text" to post.text,
            "title" to post.title,
            "userId" to currentUserId
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
        val currentUserId = firebaseSource.userId

        if (currentUserId.isEmpty())
            throw Exception("Couldn't get userId in function deletePost")

        val postDocument = firebaseSource.db
            .collection(firebasePath.posts_col).document(currentUserId)
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

    suspend fun getPostComments(postId: String): Flow<ResultData<List<Comment>>> = flow {
        var success = false

        val collectionCommentsPost = firebaseSource.db
            .collection(firebasePath.comments_col).document(postId)
            .collection(firebasePath.comments_post)

        var listComments = listOf<Comment>()
        collectionCommentsPost
            .get()
            .addOnSuccessListener { querySnapshot ->
                success = true
                if (querySnapshot.isEmpty) {
                    return@addOnSuccessListener
                }

                listComments = querySnapshot.toObjects(Comment::class.java).toList()
            }
            .addOnFailureListener {
                success = false
            }
            .await()

        if (success)
            emit(ResultData.Success(listComments))
        else {
            emit(ResultData.Error(Exception("There was an error when getting the post comments, try again later")))
        }
    }

    suspend fun getUsersComments(listComment: List<Comment>): Flow<ResultData<List<User>>> = flow {

        val listUsers = mutableListOf<User>()

        for (comment in listComment) {
            val document = firebaseSource.db.collection(firebasePath.users_col)
                .whereEqualTo("userId", comment.commentUserId)

            document.get()
                .addOnSuccessListener { querySnapshot ->
                    if (querySnapshot.isEmpty)
                        return@addOnSuccessListener

                    val usersSnapshot = querySnapshot.toObjects(User::class.java).toList()
                    for (user in usersSnapshot) {
                        listUsers.add(user)
                    }
                }
                .addOnFailureListener {

                }
                .await()
        }

        if (listUsers.size > 0)
            emit(ResultData.Success(listUsers))
        else
            emit(ResultData.Error(Exception("There was an error trying to get the comments")))
    }

    suspend fun addReplyToPost(comment: Comment): Flow<ResultData<Boolean>> = flow {
        emit(ResultData.Loading)
        val documentComment =
            firebaseSource.db.collection(firebasePath.comments_col).document(comment.replyToldId)
                .collection(firebasePath.comments_post).document()

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
                    Exception("There was an error when replying the post, try again later!"),
                    false
                )
            )
    }
}