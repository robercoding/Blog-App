package com.rober.blogapp.data.network.repository

import android.net.Uri
import com.google.firebase.firestore.Source
import com.rober.blogapp.data.ResultAuth
import com.rober.blogapp.data.ResultData
import com.rober.blogapp.data.network.firebase.*
import com.rober.blogapp.entity.Comment
import com.rober.blogapp.entity.Post
import com.rober.blogapp.entity.ReportPost
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
    private val firebaseProfileEditManager: FirebaseProfileEditManager,
    private val firebaseSettingsManager: FirebaseSettingsManager,
    private val firebasePostReplyManager: FirebasePostReplyManager
) {
    val TAG = "FirebaseRepository"

    val source = Source.CACHE

    //Global
    fun getCurrentUser(): User = firebaseSource.getCurrentUser()

    suspend fun checkIfUsernameAvailable(username: String): Flow<ResultData<Boolean>> =
        firebaseSource.checkIfUsernameAvailable(username)

    suspend fun getCurrentUserRefreshed(): User = firebaseSource.getCurrentUserRefreshed()

    suspend fun getUserProfile(userUID: String): Flow<ResultData<User>> =
        firebaseSource.getUserProfile(userUID)

    fun clearFirebaseSource() = firebaseSource.clearFirebaseSource()


    //Auth
    suspend fun getAndSetCurrentUser() = firebaseAuthManager.setCurrentUser()

    fun loginByEmail(email: String, password: String): Flow<ResultAuth> =
        firebaseAuthManager.loginByEmail(email, password)

    suspend fun loginByUsername(username: String, password: String): Flow<ResultAuth> =
        firebaseAuthManager.loginByUsername(username, password)

    suspend fun signOut(): Flow<ResultAuth> = firebaseAuthManager.signOut()

    suspend fun signUpWithEmail(email: String, password: String, name: String): Flow<ResultAuth> =
        firebaseAuthManager.signUpWithEmail(email, password, name)

    suspend fun signUpWithEmailCloud(email: String, password: String, name: String): Flow<ResultAuth> =
        firebaseAuthManager.signUpWithEmailCloud(email, password, name)

    suspend fun checkIfEmailAlreadyExists(email: String): Flow<ResultAuth> =
        firebaseAuthManager.checkIfEmailAlreadyExists(email)

    fun enableAccount(): Flow<ResultAuth> = firebaseAuthManager.enableAccount()

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
    fun reportPost(post: Post, reportedCause: String, message: String): Flow<ResultData<Boolean>> =
        firebasePostDetailManager.reportPost(post, reportedCause, message)

    suspend fun deletePost(post: Post): Flow<ResultData<Boolean>> = firebasePostDetailManager.deletePost(post)

    suspend fun saveEditedPost(post: Post): Flow<ResultData<Boolean>> =
        firebasePostDetailManager.updateEditedPost(post)

    suspend fun getPost(reportPost: ReportPost): Flow<ResultData<Post>> =
        firebasePostDetailManager.getPost(reportPost)

    suspend fun getPostComments(postId: String): Flow<ResultData<List<Comment>>> =
        firebasePostDetailManager.getPostComments(postId)

    suspend fun getUsersComments(listComments: List<Comment>): Flow<ResultData<List<User>>> =
        firebasePostDetailManager.getUsersComments(listComments)

    suspend fun addReply(comment: Comment): Flow<ResultData<Boolean>> =
        firebasePostDetailManager.addReplyToPost(comment)

    //PostReply
    suspend fun getCommentRepliesById(commentId: String): Flow<ResultData<List<Comment>>> =
        firebasePostReplyManager.getCommentRepliesById(commentId)

    suspend fun addReplyToComment(comment: Comment): Flow<ResultData<Boolean>> =
        firebasePostReplyManager.addReplyToComment(comment)

    //Search
    suspend fun getUserByString(searchUsername: String) =
        firebaseSearchManager.getUsersByString(searchUsername)

    //ProfileDetail
    suspend fun retrieveProfileUsersPosts(userID: String): Flow<ResultData<List<Post>>> =
        firebaseProfileDetailManager.retrieveUserPosts(userID)

    suspend fun retrieveNewerPostsUserProfile(userID: String): Flow<ResultData<List<Post>>> =
        firebaseProfileDetailManager.retrieveUserNewerPosts(userID)

    suspend fun getCurrentUserProfileDetail() = firebaseProfileDetailManager.getCurrentUser()

    suspend fun checkIfCurrentUserFollowsOtherUser(userID: String): Flow<ResultData<Boolean>> =
        firebaseProfileDetailManager.checkIfCurrentUserFollowsOtherUser(userID)

    suspend fun followOtherUser(user: User): Flow<ResultData<Boolean>> =
        firebaseProfileDetailManager.followOtherUser(user)

    suspend fun unfollowOtherUser(user: User): Flow<ResultData<Boolean>> =
        firebaseProfileDetailManager.unfollowOtherUser(user)

    //ProfileEdit
    suspend fun updateUser(previousUser: User, newUser: User): Flow<ResultData<Boolean>> =
        firebaseProfileEditManager.updateUser(previousUser, newUser)

    suspend fun saveImage(uri: Uri, intentImageCode: Int): Flow<ResultData<String>> =
        firebaseProfileEditManager.saveImage(uri, intentImageCode)

    //General Settings
    suspend fun getListReportedPosts(user: User): Flow<ResultData<List<ReportPost>>> =
        firebaseSettingsManager.getListReportedPosts(user)

    fun getTotalNumberPosts(user: User): Flow<ResultData<Int>> =
        firebaseSettingsManager.getTotalNumberPosts(user)

    fun disableAccount(): Flow<ResultData<Boolean>> = firebaseSettingsManager.disableAccount()

}