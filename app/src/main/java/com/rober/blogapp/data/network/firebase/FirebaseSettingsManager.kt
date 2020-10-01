package com.rober.blogapp.data.network.firebase

import com.rober.blogapp.data.ResultData
import com.rober.blogapp.entity.ReportPost
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class FirebaseSettingsManager @Inject constructor(
    firebaseSource: FirebaseSource
) {

    //Actually I'll disable the account instead of deleting it.
    fun deleteAccount(): Flow<ResultData<Boolean>> = flow {

    }

    private fun getListReportPosts() : Flow<ResultData<List<ReportPost>>> = flow {

    }

    private fun getTotalNumberPosts() : Flow<ResultData<Int>> = flow {

    }
}