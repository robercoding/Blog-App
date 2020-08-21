package com.rober.blogapp.data.network.firebase

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.rober.blogapp.data.ResultData
import com.rober.blogapp.entity.Following
import com.rober.blogapp.entity.Post
import com.rober.blogapp.entity.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.Exception

class FirebaseProfileManager @Inject constructor(
    private val firebaseSource: FirebaseSource
) {
    private val TAG = "FirebaseProfileManager"

    private var userPaginationLimit = 0
    private var savedUserListPost: MutableList<Post> = mutableListOf()

    private var hashMapCurrentUserFollowsOtherUser = hashMapOf<String, Boolean>()
    private var hashMapOthersUsersFollowings = hashMapOf<String, MutableList<Following>>()

    suspend fun retrieveProfileUserPosts(morePosts: Boolean): Flow<ResultData<List<Post>>> = flow {
        try {
            if (!morePosts && userPaginationLimit == 0) {
                val userPosts = getProfileUserPostsLimit()

                emit(ResultData.Success(userPosts))
            }

            if (!morePosts && userPaginationLimit > 0) {
                emit(ResultData.Success(savedUserListPost))
            }

            if (morePosts) {
                val userPosts = getProfileUserPostsLimit()

                emit(ResultData.Success(userPosts))
            }
        } catch (e: Exception) {
            emit(ResultData.Error(e, null))
        }

    }

    private suspend fun getProfileUserPostsLimit(): List<Post> {
        userPaginationLimit++

        val userPostsCollection =
            firebaseSource.db.collection("posts/${firebaseSource.username}/user_posts")

        val userPosts = userPostsCollection
            .limit((userPaginationLimit * 8).toLong())
            .get()
            .await()
            .toObjects(Post::class.java)

        savedUserListPost = userPosts

        return savedUserListPost
    }

    suspend fun getUserProfile(username: String): Flow<ResultData<User>> = flow {
        emit(ResultData.Loading)
        var user: User? = null

        if (username == firebaseSource.username) {
            user = firebaseSource.user
        } else {
            val userProfileRef = firebaseSource.db.collection("users").document(username)

            try {
                user = userProfileRef
                    .get()
                    .await()
                    .toObject(User::class.java)

            } catch (e: Exception) {
                emit(ResultData.Error(e, null))
            }
        }

        if (user == null) {
            emit(ResultData.Error(Exception("We couldn't find the user")))
        } else {
            emit(ResultData.Success(user))
        }
    }

    suspend fun currentUserFollowsOtherUser(otherUsername: String): Flow<ResultData<Boolean>> =
        flow {
            emit(ResultData.Loading)

            val isOtherUsernameInHashMap =
                hashMapCurrentUserFollowsOtherUser.containsKey(otherUsername)

            if (isOtherUsernameInHashMap) {
                emit(ResultData.Success(isOtherUsernameInHashMap))
            } else {
                try {
                    val userFollowingRef =
                        firebaseSource.db.collection("following/${firebaseSource.username}/user_following")

                    val followingUser = userFollowingRef
                        .whereEqualTo("following_id", otherUsername)
                        .get()
                        .await()
                        .toObjects(Following::class.java)

                    when {
                        followingUser.isEmpty() -> {
                            Log.i(TAG, "Is empty")
                            emit(ResultData.Success(false))
                        }

                        followingUser.size >= 2 -> {
                            Log.i(TAG, "Is 2 or more")
                            emit(ResultData.Success(false))
                        }

                        else -> emit(ResultData.Success(true))
                    }
                } catch (e: Exception) {
                    emit(ResultData.Error(e))
                }
            }
        }

    private fun getOtherUserFollowingSize(otherUsername: String): Flow<ResultData<Int>> = flow {
        emit(ResultData.Loading)

//        val otherUserFollowingRef = firebaseSource.db.collection("following/$otherUsername/user_following")
    }


    private fun checkIfNewFollowingHasBeenUnfollowedBefore(followingId: String): Boolean {
        return firebaseSource.listNewUnfollowingsUsername.contains(followingId)
    }

    private fun removeNewFollowingFromUnfollowing(followingId: String) {
        firebaseSource.listNewUnfollowingsUsername.remove(followingId)
    }

    suspend fun followOtherUser(user: User): Flow<ResultData<Boolean>> = flow {
        var hasUserBeenFollowed = false
        try {
            val followingRef =
                firebaseSource.db.collection("following/${firebaseSource.username}/user_following")

            val followingUser = Following(user.username)

            followingRef
                .document(followingUser.following_id)
                .set(followingUser)
                .addOnSuccessListener {
                    hasUserBeenFollowed = true
                }
                .addOnFailureListener {
                    hasUserBeenFollowed = false
                }
                .await()
        } catch (e: Exception) {
            emit(ResultData.Error(Exception("Sorry, there was an error in our servers, try again later")))
        }

        if(hasUserBeenFollowed){
            firebaseSource.listNewFollowingsUsername.add(user.username)
            if (checkIfNewFollowingHasBeenUnfollowedBefore(user.username))
                removeNewFollowingFromUnfollowing(user.username)
            updateFollowingCount(true)
            updateFollower(user.username, true)
        }

        emit(ResultData.Success(hasUserBeenFollowed))
    }

    private fun checkIfNewUnfollowingHasBeenFollowedBefore(followingUsername: String): Boolean {
        return firebaseSource.listNewFollowingsUsername.contains(followingUsername)
    }

    private fun removeNewUnfollowingFromFollowing(followingUsername: String) {
        firebaseSource.listNewFollowingsUsername.remove(followingUsername)
    }

//    private fun checkIfNewUnfollowingHasBeenUnfollowedBefore(followingUsername: String): Boolean{
//        return firebaseSource.listNewUnfollowingsUsername.contains(followingUsername)
//    }
//
//    private fun removeNewUnfollowingFromUnfollowing(followingUsername: String){
//        firebaseSource.listNewUnfollowingsUsername.remove(followingUsername)
//    }

    suspend fun unfollowOtherUser(user: User): Flow<ResultData<Boolean>> = flow {
        var hasUserBeenUnfollowed = false

        try {
            val followingRef =
                firebaseSource.db.collection("following/${firebaseSource.username}/user_following")

            followingRef
                .document(user.username)
                .delete()
                .addOnSuccessListener {
                    hasUserBeenUnfollowed = true
                }
                .addOnFailureListener {
                    hasUserBeenUnfollowed = false
                }.await()

        } catch (e: Exception) {
            emit(ResultData.Error(Exception("Sorry, there was an error in our servers, try again later")))
        }

        if(hasUserBeenUnfollowed){
            if (checkIfNewUnfollowingHasBeenFollowedBefore(user.username))
                removeNewUnfollowingFromFollowing(user.username)

            firebaseSource.listNewUnfollowingsUsername.add(user.username)
            updateFollowingCount(false)
            updateFollower(user.username, false)

        }

        emit(ResultData.Success(hasUserBeenUnfollowed))
    }

    suspend fun updateFollowingCount(didUserFollowOtherUser: Boolean) {
        val userDocumentRef = firebaseSource.db.collection("users").document(firebaseSource.username)
        try {
            if(didUserFollowOtherUser){
                userDocumentRef.update("following", FieldValue.increment(1))
                    .addOnSuccessListener {
                        Log.i("CheckUpdate", "Updated")
                    }.addOnFailureListener {
                        Log.i("CheckUpdate", "NOT UPDATED")
                    }.await()
            }else{
                userDocumentRef.update("following", FieldValue.increment(-1))
                    .addOnSuccessListener {
                        Log.i("CheckUpdate", "Updated")
                    }.addOnFailureListener {
                        Log.i("CheckUpdate", "NOT UPDATED")
                    }.await()
            }
        }catch (e: Exception){
            Log.i("CheckUpdate", "Exception")
        }
    }

    suspend fun updateFollower(userID: String, didOtherUserGetFollowed: Boolean) {
        val userDocumentRef = firebaseSource.db.collection("users").document(userID)

        try {
            if(didOtherUserGetFollowed){
                userDocumentRef.update("follower", FieldValue.increment(1))
                    .addOnSuccessListener {
                        Log.i("CheckUpdate", "Updated")
                    }.addOnFailureListener {
                        Log.i("CheckUpdate", "NOT UPDATED")
                    }.await()
            }else{
                userDocumentRef.update("follower", FieldValue.increment(-1))
                    .addOnSuccessListener {
                        Log.i("CheckUpdate", "Updated")
                    }.addOnFailureListener {
                        Log.i("CheckUpdate", "NOT UPDATED")
                    }.await()
            }
        }catch (e: Exception){
            Log.i("CheckUpdate", "Exception")
        }
    }

}