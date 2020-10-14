package com.rober.blogapp.data.network.firebase

import android.util.Log
import com.rober.blogapp.data.ResultData
import com.rober.blogapp.data.network.util.FirebasePath
import com.rober.blogapp.entity.CountsPosts
import com.rober.blogapp.entity.ReportPost
import com.rober.blogapp.entity.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirebaseSettingsManager @Inject constructor(
    val firebaseSource: FirebaseSource,
    val firebasePath: FirebasePath
) {
    private val TAG = "FirebaseSettingsManager"

    fun disableAccount(): Flow<ResultData<Boolean>> = flow {
        var result = false
        firebaseSource.functions.getHttpsCallable("disableAccount")
            .call().continueWith { task ->
                if (task.isSuccessful) {
                    val resultTask = task.result ?: throw Exception("Result task doesn't exist")

                    if (resultTask.data == null) {
                        Log.i(TAG, "Result task data is null")
                        return@continueWith
                    } else {
                        result = resultTask.data as Boolean
                        Log.i(TAG, "Result is $result")
                    }
                } else {
                }
            }.await()
        if (result) {
            emit(ResultData.Success(result))
        } else {
            emit(ResultData.Error(Exception("There was an error when trying to disable the account"), result))
        }
    }

    suspend fun getListReportedPosts(user: User): Flow<ResultData<List<ReportPost>>> = flow {
        val collectionReportedPosts =
            firebaseSource.db.collection(firebasePath.reports_col)
                .document(user.userId)
                .collection(firebasePath.posts_reports)

        var reportedPosts = mutableListOf<ReportPost>()
        var success = false
        var exception = Exception()
        collectionReportedPosts
            .get()
            .addOnSuccessListener {
                success = true
                if (it.documents.isNotEmpty()) {
                    reportedPosts = it.toObjects(ReportPost::class.java)
                }
            }
            .addOnFailureListener {
                success = false
                exception = it
            }
            .await()

        if (success)
            emit(ResultData.Success(reportedPosts))
        else
            emit(ResultData.Error(exception, null))
    }

    fun getTotalNumberPosts(user: User): Flow<ResultData<Int>> = flow {

        val countPostsDocumentReference =
            firebaseSource.db.collection(firebasePath.posts_col).document(user.userId)
                .collection(firebasePath.user_count_posts).document(firebasePath.countPosts)

        var countPosts: CountsPosts? = null
        countPostsDocumentReference
            .get()
            .addOnSuccessListener {
                countPosts = it.toObject(CountsPosts::class.java)

            }.await()


        countPosts?.also { tempCountPosts ->
            emit(ResultData.Success(tempCountPosts.countPosts))
        } ?: kotlin.run {
            emit(ResultData.Error(Exception("There was an error in our servers"), 0))
        }
    }
}