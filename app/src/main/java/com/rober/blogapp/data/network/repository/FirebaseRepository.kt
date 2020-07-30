package com.rober.blogapp.data.network.repository

import com.google.firebase.firestore.Source
import com.rober.blogapp.data.ResultAuth
import com.rober.blogapp.data.network.firebase.FirebaseAuthManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class FirebaseRepository @Inject constructor(private val firebaseAuthManager: FirebaseAuthManager) {
    val TAG ="FirebaseRepository"

    val source = Source.CACHE

    val usersColl = "users"

    suspend fun login(email: String, password: String): Flow<ResultAuth> = flow {
        emit(ResultAuth.Loading)
        emit(firebaseAuthManager.login(email, password))
    }

    suspend fun signOut(): Boolean = firebaseAuthManager.signOut()

    fun signUpWithEmail(email: String, password: String): Flow<ResultAuth> = flow {
        emit(ResultAuth.Loading)
        emit(firebaseAuthManager.signUpWithEmail(email, password))
    }

    suspend fun checkIfUserAlreadyLoggedIn(): Boolean = firebaseAuthManager.checkIfUserAlreadyLoggedIn()





}