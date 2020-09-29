package com.rober.blogapp.data.network.repository

import android.net.Uri
import com.google.firebase.firestore.Source
import com.rober.blogapp.data.ResultAuth
import com.rober.blogapp.data.ResultData
import com.rober.blogapp.data.network.firebase.*
import com.rober.blogapp.entity.Post
import com.rober.blogapp.entity.User
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class FirebaseRepository @Inject
constructor(
    private val firebaseSource: FirebaseSource,
    private val firebaseAuthManager: FirebaseAuthManager,
    private val firebaseFeedManager: FirebaseFeedManager,
    private val firebasePostAddManager: FirebasePostAddManager,
    private val firebasePostDetailManager: FirebasePostDetailManager,
    private val firebaseSearchManager: FirebaseSearchManager,
    private val firebaseProfileDetailManager: FirebaseProfileDetailManager,
    private val firebaseProfileEditManager: FirebaseProfileEditManager

) {
    val TAG = "FirebaseRepository"

    val source = Source.CACHE

    //Global
    fun getCurrentUser(): User = firebaseSource.getCurrentUser()

    suspend fun checkIfUsernameAvailable(username: String): Flow<ResultData<Boolean>> =
        firebaseSource.checkIfUsernameAvailable(username)

    suspend fun getCurrentUserRefreshed(): User = firebaseSource.getCurrentUserRefreshed()

    suspend fun getUserProfile(userUID: String): Flow<ResultData<User>> = firebaseSource.getUserProfile(userUID)

    //Auth
    suspend fun getAndSetCurrentUser() = firebaseAuthManager.setCurrentUser()

    suspend fun login(email: String, password: String): Flow<ResultAuth> = firebaseAuthManager.login(email, password)

    suspend fun signOut(): Flow<ResultAuth> = firebaseAuthManager.signOut()

    suspend fun signUpWithEmail(email: String, password: String, name: String): Flow<ResultAuth> =
        firebaseAuthManager.signUpWithEmail(email, password, name)

    suspend fun checkIfEmailAlreadyExists(email: String): Flow<ResultAuth> =
        firebaseAuthManager.checkIfEmailAlreadyExists(email)

    //suspend fun checkIfUserAlreadyLoggedIn(): Boolean = firebaseAuthManager.checkIfUserAlreadyLoggedIn()

    //Feed
    suspend fun retrieveInitFeedPosts(): Flow<ResultData<List<Post>>> = firebaseFeedManager.getInitFeedPosts()

    suspend fun retrieveNewFeedPosts(): Flow<ResultData<List<Post>>> = firebaseFeedManager.getNewFeedPosts()

    suspend fun retrieveOldFeedPosts(): Flow<ResultData<List<Post>>> = firebaseFeedManager.getOldFeedPosts()

    fun getEndOfTimeline(): Boolean = firebaseFeedManager.getEndOfTimeline()

    suspend fun getUsersFromCurrentFollowings(listUsers: MutableList<User>): Flow<ResultData<List<User>>> =
        firebaseFeedManager.getUsersFromCurrentFollowings(listUsers)

    fun clearListsAndMapsLocalDatabase() = firebaseFeedManager.cleanListsAndMapsLocalDatabase()

    //suspend fun retrieveSavedLocalPosts(): Flow<ResultData<List<Post>>> = firebaseFeedManager.getSavedLocalPosts()

    //PostAdd
    suspend fun savePost(post: Post): Flow<ResultData<Unit>> = firebasePostAddManager.savePost(post)

    //PostDetail
    suspend fun reportPost(post: Post, reportedCause: String, message: String): Flow<ResultData<Boolean>> = firebasePostDetailManager.reportPost(post, reportedCause, message)
    
    suspend fun deletePost(post: Post): Flow<ResultData<Boolean>> = firebasePostDetailManager.deletePost(post)

    suspend fun saveEditedPost(post: Post): Flow<ResultData<Boolean>> = firebasePostDetailManager.updateEditedPost(post)

    //Search
    suspend fun getUserByString(searchUsername: String) = firebaseSearchManager.getUsersByString(searchUsername)

    //ProfileDetail
    suspend fun retrieveProfileUsersPosts(userID: String): Flow<ResultData<List<Post>>> =
        firebaseProfileDetailManager.retrieveUserPosts(userID)

    suspend fun retrieveNewerPostsUserProfile(userID: String): Flow<ResultData<List<Post>>> =
        firebaseProfileDetailManager.retrieveUserNewerPosts(userID)

    suspend fun getCurrentUserProfileDetail() = firebaseProfileDetailManager.getCurrentUser()

    suspend fun checkIfCurrentUserFollowsOtherUser(userID: String): Flow<ResultData<Boolean>> =
        firebaseProfileDetailManager.checkIfCurrentUserFollowsOtherUser(userID)

    suspend fun followOtherUser(user: User): Flow<ResultData<Boolean>> = firebaseProfileDetailManager.followOtherUser(user)

    suspend fun unfollowOtherUser(user: User): Flow<ResultData<Boolean>> =
        firebaseProfileDetailManager.unfollowOtherUser(user)

    //ProfileEdit
    suspend fun updateUser(previousUser: User, newUser: User): Flow<ResultData<Boolean>> =
        firebaseProfileEditManager.updateUser(previousUser, newUser)

    suspend fun saveImage(uri: Uri, intentImageCode: Int): Flow<ResultData<String>> =
        firebaseProfileEditManager.saveImage(uri, intentImageCode)
}