package com.rober.blogapp.data.network.firebase

import com.google.firebase.firestore.FieldValue
import com.rober.blogapp.data.ResultData
import com.rober.blogapp.data.network.util.FirebasePath
import com.rober.blogapp.entity.Post
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

    suspend fun savePost(post: Post): Flow<ResultData<Unit>> = flow {
        emit(ResultData.Loading)
        val newPostPath = firebaseSource.db.collection(firebasePath.posts).document(firebaseSource.username).collection(firebasePath.user_posts).document()
        var success = false
        var exception: Exception? = Exception("Sorry, we couldn't upload the post, try again later")

        post.user_creator_id = firebaseSource.username
        post.post_id = newPostPath.id

        try{
            newPostPath.set(post)
                .addOnSuccessListener {
                    success = true
                }
                .addOnFailureListener{
                    success = false
                }
                .await()
        }catch (e: Exception){
            exception = e
        }

        if(success){
            val successAddPostCount = addCountPost()

            if(successAddPostCount){
                emit(ResultData.Success<Unit>())
            }
            else{
                newPostPath.delete().await()
                emit(ResultData.Error<Unit>(Exception("Sadly we couldn't add the count post!")))
            }
        }else{
            emit(ResultData.Error<Unit>(exception!!))

        }
    }

    private suspend fun addCountPost(): Boolean{
        val collRef = firebaseSource.db.collection(firebasePath.posts).document(firebaseSource.username).collection(firebasePath.user_count_posts).document(firebasePath.countPosts)

        var success = false
        collRef
            .update("countPosts", FieldValue.increment(1))
            .addOnSuccessListener {
                success = true
            }
            .addOnFailureListener {
                success = false
            }
            .await()

        return success
    }
}