package com.rober.blogapp.data.network.firebase

import com.rober.blogapp.data.ResultData
import com.rober.blogapp.entity.Post
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class FirebasePostDetailManager @Inject constructor(
    private val firebaseSource: FirebaseSource
) {
    fun reportPost(post: Post): Flow<ResultData<Boolean>> = flow {
        //TODO
    }

    fun deletePost(post: Post): Flow<ResultData<Boolean>> = flow {
        //TODO
    }

}