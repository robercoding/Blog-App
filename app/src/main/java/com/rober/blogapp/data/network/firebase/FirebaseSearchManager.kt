package com.rober.blogapp.data.network.firebase

import android.util.Log
import com.rober.blogapp.data.ResultData
import com.rober.blogapp.entity.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

class FirebaseSearchManager constructor(
    private val firebaseSource: FirebaseSource
) {

    private val TAG = "FirebaseSearchManager"
    private val mapUsersCache = mutableMapOf<String, List<User>>()

    suspend fun getUsersByString(searchUsername: String): Flow<ResultData<List<User>>> = flow {
        emit(ResultData.Loading)

        val isKeyOnCache = mapUsersCache.containsKey(searchUsername)

        if (!isKeyOnCache) {
            val listUsers = firebaseSource.db.collection("users")
                .whereGreaterThanOrEqualTo("username", searchUsername)
                .whereLessThanOrEqualTo("username", "$searchUsername~")
                .limit(5)
                .get()
                .await()
                .toObjects(User::class.java).toList()

            mapUsersCache[searchUsername] = listUsers

            emit(
                ResultData.Success(
                    mapUsersCache.getValue(searchUsername).sortedBy { it.username })
            )
        } else {
            Log.i(TAG, "We got something in our database, sending back there")
            emit(ResultData.Success(mapUsersCache[searchUsername]?.sortedBy { it.username }))
        }
    }
}