package com.rober.blogapp.data.network.firebase

import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FieldValue
import com.rober.blogapp.data.ResultData
import com.rober.blogapp.data.network.util.FirebasePath
import com.rober.blogapp.entity.CountsPosts
import com.rober.blogapp.entity.Post
import com.rober.blogapp.entity.UserDocumentUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.Exception

class FirebasePostAddManager
@Inject
constructor(
    private val firebaseSource: FirebaseSource,
    private val firebasePath: FirebasePath
) {
    private var userDocumentUID: UserDocumentUID? = null


    suspend fun savePost(post: Post): Flow<ResultData<Unit>> = flow {
        emit(ResultData.Loading)
        if (isUserDocumentUIDNull()) {
            emit(ResultData.Error(Exception("User documents are null"), null))
            return@flow
        }

        val userPostsDocumentUID = userDocumentUID!!.postsDocumentUid

        val newPostPath =
            firebaseSource.db
                .collection(firebasePath.posts_col)
                .document(userPostsDocumentUID)
                .collection(firebasePath.user_posts)
                .document()

        var success = false
        val exception: Exception? = Exception("Sorry, we couldn't upload the post, try again later")

        post.user_creator_id = firebaseSource.username
        post.post_id = newPostPath.id

        try {
            newPostPath.set(post)
                .addOnSuccessListener {
                    success = true
                }
                .addOnFailureListener {
                    success = false
                }
                .await()
        } catch (e: Exception) {
            Log.i("CountPosts", "$e ")
        }

        if (success) {
            val successAddPostCount = addCountPost()

            if (successAddPostCount) {
                emit(ResultData.Success<Unit>())
            } else {
                newPostPath.delete().await()
                emit(ResultData.Error<Unit>(Exception("Sadly we couldn't add the count post!")))
            }
        } else {
            emit(ResultData.Error<Unit>(exception!!))
        }
    }

    private suspend fun addCountPost(): Boolean {
        if (isUserDocumentUIDNull())
            return false

        val userPostDocumentUID = userDocumentUID!!.postsDocumentUid
        val collRef =
            firebaseSource.db.collection(firebasePath.posts_col).document(userPostDocumentUID)
                .collection(firebasePath.user_count_posts).document(firebasePath.countPosts)
        var success = false

        val doesCountPostsPathExists = doesCountPostsPathExists()

        Log.i("CountPosts", "Exists?= $doesCountPostsPathExists")

        if (doesCountPostsPathExists) {
            collRef
                .update("countPosts", FieldValue.increment(1))
                .addOnSuccessListener {
                    success = true
                }
                .addOnFailureListener {
                    success = false
                }
                .await()
        } else {
            collRef
                .set(CountsPosts(1))
                .addOnSuccessListener {
                    success = true
                }
                .addOnFailureListener {
                    Log.i("CountPosts", "$it")
                    success = false
                }
                .await()
        }

        return success
    }

    private suspend fun doesCountPostsPathExists(): Boolean {

        val userPostDocumentUID = userDocumentUID!!.postsDocumentUid

        val pathExists =
            firebaseSource.db.collection(firebasePath.posts_col).document(userPostDocumentUID)
                .collection(firebasePath.user_count_posts).document(firebasePath.countPosts)

        var documentExists = false
        pathExists.get()
            .addOnSuccessListener {
                documentExists = it.exists()
            }.addOnFailureListener {
                documentExists = false
            }
            .await()

        return documentExists
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