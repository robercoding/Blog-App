package com.rober.blogapp.data.network.repository

import com.google.firebase.firestore.Source
import com.rober.blogapp.data.ResultAuth
import com.rober.blogapp.data.ResultData
import com.rober.blogapp.data.network.firebase.FirebaseAuthManager
import com.rober.blogapp.data.network.firebase.FirebaseFeedManager
import com.rober.blogapp.data.network.firebase.FirebasePostAddManager
import com.rober.blogapp.entity.Post
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class FirebaseRepository @Inject
constructor(
    private val firebaseAuthManager: FirebaseAuthManager,
    private val firebaseFeedManager: FirebaseFeedManager,
    private val firebasePostAddManager: FirebasePostAddManager
) {
    val TAG ="FirebaseRepository"

    val source = Source.CACHE

    //Auth
    suspend fun getAndSetCurrentUser() = firebaseAuthManager.setCurrentUser()

    suspend fun getCurrentUser() = firebaseAuthManager.getCurrentUser()

    suspend fun login(email: String, password: String): Flow<ResultAuth> = firebaseAuthManager.login(email, password)

    suspend fun signOut(): Flow<ResultAuth> = firebaseAuthManager.signOut()

    suspend fun signUpWithEmail(email: String, password: String, name: String): Flow<ResultAuth> = firebaseAuthManager.signUpWithEmail(email, password, name)

    //suspend fun checkIfUserAlreadyLoggedIn(): Boolean = firebaseAuthManager.checkIfUserAlreadyLoggedIn()

    //Feed
    suspend fun retrievePosts(morePosts: Boolean) : Flow<ResultData<List<Post>>> = firebaseFeedManager.retrievePosts(morePosts)

    suspend fun savePost(post: Post): Flow<ResultData<Unit>> = firebasePostAddManager.savePost(post)
}