package com.rober.blogapp.data.network.firebase

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.rober.blogapp.data.ResultData
import com.rober.blogapp.data.network.util.FirebasePath
import com.rober.blogapp.entity.CountsPosts
import com.rober.blogapp.entity.Following
import com.rober.blogapp.entity.Post
import com.rober.blogapp.entity.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import org.joda.time.DateTime
import org.threeten.bp.Duration
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.Exception

class FirebaseProfileDetailManager @Inject constructor(
    private val firebaseSource: FirebaseSource,
    private val firebasePath: FirebasePath
) {
    private val TAG = "FirebaseProfileManager"

    private var savedUserHashMapPost = hashMapOf<String, MutableList<Post>>()
    private var savedUserHashMapDates = hashMapOf<String, MutableList<Date>>()
    private var savedUserHashMapMinusDays = hashMapOf<String, Int>()
    private var savedUserHashMapRetrieveNewerPostsDate = hashMapOf<String, Date>()

    private var hashMapCurrentUserFollowsOtherUser = hashMapOf<String, Boolean>()
    private var hashMapOthersUsersFollowings = hashMapOf<String, MutableList<Following>>()

    suspend fun retrieveUserPosts(userID: String): Flow<ResultData<List<Post>>> = flow {
        emit(ResultData.Loading)

        //Initialize variable
        var minusDays = 0
        var dateLessThan = DateTime.now().minusDays(minusDays).toDate()
        var dateGreaterThan = DateTime.now().minusDays(minusDays + 1).toDate()

        var userContainsSavedPosts = savedUserHashMapPost[userID]

        val countPosts = getCountPostsFromOtherUser(userID)
        var userContainsSavedPostsSize = 0

        userContainsSavedPosts?.let {
            Log.i("RequestPosts", " UserContainsSavedPosts TRUE")
            dateLessThan = savedUserHashMapDates[userID]?.get(0)
            dateGreaterThan = savedUserHashMapDates[userID]?.get(1)
            minusDays = savedUserHashMapMinusDays[userID]!!
//            dateRetrieveNewerPosts = savedUserHashMapRetrieveNewerPostsDate[userID]!!
            Log.i("MinusDays", "Minus days enter = $minusDays")
            userContainsSavedPostsSize = it.size
        } ?: run {
            savedUserHashMapRetrieveNewerPostsDate[userID] = Date()
            Log.i(TAG, "Save retrieve = ${savedUserHashMapRetrieveNewerPostsDate[userID]}")
            userContainsSavedPosts = mutableListOf()
            userContainsSavedPostsSize = 0
        }

        val newUserMutableListPosts = mutableListOf<Post>()

//        val minutesDifference = returnMinutesDifference(dateRetrieveNewerPosts.time)
//
//        if (minutesDifference > 30) {
//            val listNewerPosts = getNewerPosts(userID, dateRetrieveNewerPosts)
//
//            if (listNewerPosts.isNotEmpty()) {
//                newUserMutableListPosts.addAll(listNewerPosts)
//                savedUserHashMapRetrieveNewerPostsDate[userID] =
//                    Date() //Save new date to retrieve newer posts
//            }
//        }

        while (newUserMutableListPosts.size < 6 && userContainsSavedPostsSize < countPosts) {
            val listPosts = getUserPostsByDateLessAndGreater(userID, dateLessThan, dateGreaterThan)

            Log.i(
                TAG,
                "We got these dates: DatesLess $dateLessThan and DateGreater = $dateGreaterThan"
            )

            listPosts.let {
                for (post in listPosts) {
                    newUserMutableListPosts.add(post)
                    userContainsSavedPostsSize += 1
                }
            }

            minusDays += 1
            dateLessThan = DateTime.now().minusDays(minusDays).toDate()
            dateGreaterThan = DateTime.now().minusDays(minusDays + 1).toDate()
        }

        //Save dates on local
        savedUserHashMapMinusDays[userID] = minusDays
        savedUserHashMapDates[userID] = mutableListOf(dateLessThan, dateGreaterThan)

        for (userPosts in newUserMutableListPosts) {
            userContainsSavedPosts?.add(userPosts)
        }

        userContainsSavedPosts?.let {
            val userContainedSavedPostsSortedByDescending =
                it.sortedByDescending { post -> post.created_at }.toMutableList()
            savedUserHashMapPost[userID] = userContainedSavedPostsSortedByDescending
            emit(ResultData.Success(userContainedSavedPostsSortedByDescending))
        } ?: kotlin.run {
            savedUserHashMapPost.remove(userID)
            savedUserHashMapMinusDays.remove(userID)
            savedUserHashMapDates.remove(userID)
            emit(com.rober.blogapp.data.ResultData.Error(kotlin.Exception("Sorry we couldn't load user posts, try again later")))
        }
    }

    suspend fun retrieveUserNewerPosts(userID: String): Flow<ResultData<List<Post>>> = flow {
        val dateUserRetrieveNewerPosts = savedUserHashMapRetrieveNewerPostsDate[userID]

        val listNewerPosts: MutableList<Post>

        if (dateUserRetrieveNewerPosts == null) {
            Log.i(TAG, "Is null")
            savedUserHashMapRetrieveNewerPostsDate[userID] = Date()
            emit(ResultData.Success(savedUserHashMapPost[userID]))
        } else {
            Log.i(TAG, "Date to retrieve: $dateUserRetrieveNewerPosts")
            listNewerPosts = getNewerPosts(userID, dateUserRetrieveNewerPosts).toMutableList()
            Log.i(TAG, "Save again = ${savedUserHashMapRetrieveNewerPostsDate[userID]}")

            if (listNewerPosts.isEmpty()) {
                emit(ResultData.Success(savedUserHashMapPost[userID]))
            } else {
                val newUserListPosts = savedUserHashMapPost[userID]

                listNewerPosts.onEach {post ->
                    newUserListPosts?.add(post)
                }

                val newUserListPostsSortedByDescending = newUserListPosts?.sortedByDescending { post -> post.created_at }?.toMutableList()

                newUserListPostsSortedByDescending?.let {list ->
                    savedUserHashMapPost[userID] = list
                    emit(ResultData.Success(savedUserHashMapPost[userID]))
                }?: kotlin.run {
                    emit(com.rober.blogapp.data.ResultData.Success(savedUserHashMapPost[userID]))
                }
            }
        }
    }

    private suspend fun getNewerPosts(userID: String, dateRetrieveNewerPosts: Date): List<Post> {
        return getUserPostsByDateLessAndGreater(userID, Date(), dateRetrieveNewerPosts)
    }

    private suspend fun getUserPostsByDateLessAndGreater(
        userID: String,
        dateLessThan: Date,
        dateGreaterThan: Date
    ): List<Post> {

        return firebaseSource.db
            .collection("posts")
            .document(userID)
            .collection(firebasePath.user_posts)
            .whereLessThan("created_at", dateLessThan)
            .whereGreaterThan("created_at", dateGreaterThan)
            .get()
            .await()
            .toObjects(Post::class.java)
    }

    private fun returnMinutesDifference(dateRetrieveNewerPostsTime: Long): Long {
        val diffInMillisec = Date().time - dateRetrieveNewerPostsTime

        return TimeUnit.MILLISECONDS.toMinutes(diffInMillisec)
    }

//Without date by
//    private suspend fun getProfilePosts(userID: String) : List<Post>{
//        var listOtherUserPosts =
//            firebaseSource.db
//                .collection("posts")
//                .document(userID)
//                .collection(firebasePath.user_posts)
//                .get()
//                .await()
//                .toObjects(Post::class.java)
//
//        return listOtherUserPosts
//    }

    private suspend fun getCountPostsFromOtherUser(userID: String): Int {
        val countPostsDocRef = firebaseSource.db.collection(firebasePath.posts).document(userID)
            .collection(firebasePath.user_count_posts).document(firebasePath.countPosts)

        Log.i("CountPosts", "Path =${countPostsDocRef.path}")

        val countPosts = countPostsDocRef
            .get()
            .await()
            .toObject(CountsPosts::class.java)

        Log.i("CountPosts", "CountPosts =$countPosts")

        countPosts?.let { it ->
            return it.countPosts
        } ?: kotlin.run {
            return 0
        }
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

        if (hasUserBeenFollowed) {
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

        if (hasUserBeenUnfollowed) {
            if (checkIfNewUnfollowingHasBeenFollowedBefore(user.username))
                removeNewUnfollowingFromFollowing(user.username)

            firebaseSource.listNewUnfollowingsUsername.add(user.username)
            updateFollowingCount(false)
            updateFollower(user.username, false)

        }

        emit(ResultData.Success(hasUserBeenUnfollowed))
    }

    suspend fun updateFollowingCount(didUserFollowOtherUser: Boolean) {
        val userDocumentRef =
            firebaseSource.db.collection("users").document(firebaseSource.username)
        try {
            if (didUserFollowOtherUser) {
                userDocumentRef.update("following", FieldValue.increment(1))
                    .addOnSuccessListener {
                        Log.i("CheckUpdate", "Updated")
                    }.addOnFailureListener {
                        Log.i("CheckUpdate", "NOT UPDATED")
                    }.await()
            } else {
                userDocumentRef.update("following", FieldValue.increment(-1))
                    .addOnSuccessListener {
                        Log.i("CheckUpdate", "Updated")
                    }.addOnFailureListener {
                        Log.i("CheckUpdate", "NOT UPDATED")
                    }.await()
            }
        } catch (e: Exception) {
            Log.i("CheckUpdate", "Exception")
        }
    }

    suspend fun updateFollower(userID: String, didOtherUserGetFollowed: Boolean) {
        val userDocumentRef = firebaseSource.db.collection("users").document(userID)

        try {
            if (didOtherUserGetFollowed) {
                userDocumentRef.update("follower", FieldValue.increment(1))
                    .addOnSuccessListener {
                        Log.i("CheckUpdate", "Updated")
                    }.addOnFailureListener {
                        Log.i("CheckUpdate", "NOT UPDATED")
                    }.await()
            } else {
                userDocumentRef.update("follower", FieldValue.increment(-1))
                    .addOnSuccessListener {
                        Log.i("CheckUpdate", "Updated")
                    }.addOnFailureListener {
                        Log.i("CheckUpdate", "NOT UPDATED")
                    }.await()
            }
        } catch (e: Exception) {
            Log.i("CheckUpdate", "Exception")
        }
    }
}