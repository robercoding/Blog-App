package com.rober.blogapp.data.network.firebase

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

    fun getTotalNumberPosts(user: User): Flow<ResultData<Int>> = flow {
        val postsDocumentUID = firebaseSource.getUserDocumentUID(user.user_id)?.postsDocumentUid

        if (postsDocumentUID == null) {
            emit(ResultData.Error(Exception("Sorry, there was an error in our servers"), 0))
            return@flow
        }

        val countPostsDocumentReference =
            firebaseSource.db.collection(firebasePath.posts_col).document(postsDocumentUID)
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