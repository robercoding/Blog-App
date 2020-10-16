package com.rober.blogapp.data.network.firebase

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import com.rober.blogapp.data.ResultData
import com.rober.blogapp.data.network.util.FirebasePath
import com.rober.blogapp.entity.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.Exception

class FirebaseSource @Inject constructor(private val firebasePath: FirebasePath) {
    private val TAG = "FirebaseSource"

    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val storage = FirebaseStorage.getInstance()
    val functions = FirebaseFunctions.getInstance()

    var userAuth: FirebaseUser? = null
    var user: User? = null
    var username = ""
    var userId = ""
    var followingList: MutableList<Following>? = null
    var followerList: MutableList<Follower>? = null
    var listPostsDeleted = mutableListOf<Post>()

    var userChangedUsername = false
    var usernameBeforeChange = ""

//    var feedCheckedUserChangedUsername = false
//    var profileDetailCheckedUserChangedUsername = false

    //When user goes back to FeedFragment check if there's a new activity on following to retrieve or not the their feeds
    val listNewFollowingsUserID = HashSet<String>()
    val listNewUnfollowingsUsername = HashSet<String>()

//    val listNewFollowersUsername = HashSet<String>()
//    val listNewUnfollowersUsername = HashSet<String>()

    suspend fun setCurrentUser() {
        Log.i("User:", "First time user -> $user and $username")
        var tempUsername: Username? = null
        var tempUser: User? = null
        Log.i(TAG, userAuth.toString())
        if (userAuth != null) {
            try {
                Log.i("User:", "FirebaseSource: uid ${userAuth!!.uid}")
                db.collection("usernames").document(userAuth!!.uid)
                    .get()
                    .addOnSuccessListener {
                        Log.i("User:", "Success")
                        if (it.exists())
                            tempUsername = it.toObject(Username::class.java)
                    }.addOnFailureListener {
                        Log.i("User:", "Failure")
                        username = ""
                    }
                    .await()
            } catch (e: Exception) {
                Log.i(TAG, e.message.toString())
            }

            tempUsername?.let { temporaryUsername ->
                username = temporaryUsername.username

                tempUser =
                    db.collection(firebasePath.users_col).document(temporaryUsername.username).get().await()
                        .toObject(User::class.java)
            }

            tempUser?.let { temporaryUser ->
                user = temporaryUser
                userId = temporaryUser.userId
            }

            Log.i(TAG, "User: $user")
        }
    }

    suspend fun setCurrentFollowing() {
        if (userId.isEmpty())
            return

        try {
            val followingRef =
                db.collection(firebasePath.following_col).document(userId)
                    .collection(firebasePath.user_following)

            followingList = followingRef
                .get()
                .await()
                .toObjects(Following::class.java)

        } catch (e: Exception) {
            Log.i(TAG, "$e")
        }
    }

    suspend fun setCurrentFollower() {
        try {
            val followerRef =
                db.collection(firebasePath.follower_col).document(userId)
                    .collection(firebasePath.user_followers)

            followerList = followerRef
                .get()
                .await()
                .toObjects(Follower::class.java)

        } catch (e: Exception) {
            Log.i(TAG, "$e")
        }
    }

    fun getCurrentUser(): User {
        user?.let {
            return it
        } ?: kotlin.run {
            return User()
        }
    }

    suspend fun getCurrentUserRefreshed(): User {
        setCurrentUser()
        return getCurrentUser()
    }

    suspend fun checkIfUsernameAvailable(username: String): Flow<ResultData<Boolean>> = flow {
        var nameAvailable = false

        try {
            val userDocumentRef = db.collection("users").document(username)
            userDocumentRef
                .get()
                .addOnSuccessListener {
                    val user = it.toObject(User::class.java)
                    nameAvailable = user == null
                }.addOnFailureListener {
                    nameAvailable = false
                }.await()
        } catch (e: Exception) {
            Log.i(TAG, "${e.message}")
        }

        emit(ResultData.Success(nameAvailable))
    }

    fun getUserProfile(userUID: String): Flow<ResultData<User>> = flow {
        emit(ResultData.Loading)
        var tempUser: User? = null

        var documentUID = ""

        if (userUID == user?.userId) {
            tempUser = user
        } else {
            db.collection("users")
                .whereEqualTo("userId", userUID)
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty)
                        return@addOnSuccessListener
                    for (document in documents) {
                        documentUID = document.id
                    }
                }
                .await()

            if (documentUID.isEmpty()) {
                emit(ResultData.Error(Exception("We couldn't find the user")))
                return@flow
            }

            try {
                tempUser = db.collection("users").document(documentUID)
                    .get()
                    .await()
                    .toObject(User::class.java)

            } catch (e: Exception) {
                emit(ResultData.Error(e, null))
            }
        }

        if (tempUser == null) {
            emit(ResultData.Error(Exception("We couldn't find the user")))
        } else {
            emit(ResultData.Success(tempUser))
        }
    }

    fun addToNewFollowingList(userUID: String) {
        val userIsAlreadyOnNewFollowing =
            listNewFollowingsUserID.find { newFollowingUserUID -> newFollowingUserUID == userUID }

        if (userIsAlreadyOnNewFollowing != null) {
            return
        }

        val userIsAlreadyOnNewUnfollowing =
            listNewUnfollowingsUsername.find { newUnfollowingUserUID -> newUnfollowingUserUID == userUID }

        if (userIsAlreadyOnNewUnfollowing != null) {
            listNewUnfollowingsUsername.remove(userUID)
        }

        val userIsAlreadyFollowing = followingList?.find { following -> following.followingId == userUID }

        if (userIsAlreadyFollowing != null) {
            return
        }

        listNewFollowingsUserID.add(userUID)
    }

    fun addToNewUnfollowingList(userUID: String) {
        val userIsAlreadyOnNewFollowing =
            listNewFollowingsUserID.find { newFollowingUserUID -> newFollowingUserUID == userUID }

        if (userIsAlreadyOnNewFollowing != null) {
            listNewFollowingsUserID.remove(userUID)

        }

        val userIsAlreadyOnNewUnfollowing =
            listNewUnfollowingsUsername.find { newUnfollowingUserUID -> newUnfollowingUserUID == userUID }

        if (userIsAlreadyOnNewUnfollowing != null) {
            return
        }

        listNewUnfollowingsUsername.add(userUID)
    }

    fun clearFirebaseSource(){
        userAuth = null
        user = null
        username = ""
        userId = ""
        followingList = null
        followerList = null
        listPostsDeleted = mutableListOf<Post>()
    }
}