package com.rober.blogapp.data.network.firebase

import com.rober.blogapp.data.ResultData
import com.rober.blogapp.data.network.util.FirebasePath
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

    //Actually I'll disable the account instead of deleting it.
    fun deleteAccount(): Flow<ResultData<Boolean>> = flow {

    }

    suspend fun getListReportedPosts(user: User): Flow<ResultData<List<ReportPost>>> = flow {
        val collectionReportedPosts =
            firebaseSource.db.collection(firebasePath.reports_col)
                .document(user.user_id)
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

    private fun getTotalNumberPosts(): Flow<ResultData<Int>> = flow {

    }
}