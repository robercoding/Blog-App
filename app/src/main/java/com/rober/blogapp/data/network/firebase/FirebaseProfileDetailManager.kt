package com.rober.blogapp.data.network.firebase

import android.util.Log
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.rober.blogapp.data.ResultData
import com.rober.blogapp.data.network.util.FirebasePath
import com.rober.blogapp.entity.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import org.threeten.bp.Instant
import org.threeten.bp.temporal.ChronoUnit
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
    private var savedUserHashMapDatesEpochSecond =
        hashMapOf<String, MutableList<Long>>() //0:DateLessThanEpochSeconds and 1:DateGreaterThanEpochSeconds
    private var savedUserHashMapMinusDays = hashMapOf<String, Long>()
    private var savedUserHashMapRetrieveNewerPostsDateEpochSecond = hashMapOf<String, Long>()

    private var hashMapCurrentUserFollowsOtherUser = hashMapOf<String, Boolean>()
    private var hashMapOthersUsersFollowings = hashMapOf<String, MutableList<Following>>()

//    private var userDocumentUID: UserDocumentUID? = null

    suspend fun retrieveUserPosts(userID: String): Flow<ResultData<List<Post>>> = flow {
        emit(ResultData.Loading)

        //Initialize variable
        var minusDays: Long = 0
        var dateLessThanEpochSeconds = Instant.now().minus(minusDays, ChronoUnit.DAYS).epochSecond
        var dateGreaterThanEpochSeconds = Instant.now().minus(minusDays + 30, ChronoUnit.DAYS).epochSecond

        var userContainsSavedPosts = savedUserHashMapPost[userID]

        userContainsSavedPosts?.let {
            val datesThanEpochSeconds = savedUserHashMapDatesEpochSecond.getValue(userID)
            dateLessThanEpochSeconds = datesThanEpochSeconds[0]
            dateGreaterThanEpochSeconds = datesThanEpochSeconds[1]
            minusDays = savedUserHashMapMinusDays.getValue(userID)

        } ?: run {
            savedUserHashMapRetrieveNewerPostsDateEpochSecond[userID] = Instant.now().epochSecond
            userContainsSavedPosts = mutableListOf()
        }

        val newUserMutableListPosts = mutableListOf<Post>()

//        val minutesDifference = returnMinutesDifference(dateRetrieveNewerPosts.time)
//
//        if (minutesDifference > 30) {
//            val listNewerPosts = getNewerPosts(userID, dateRetrieveNewerPosts)
//
//            if (listNewerPosts.isNotEmpty()) {
//                newUserMutableListPosts.addAll(listNewerPosts)
//                savedUserHashMapRetrieveNewerPostsDateEpochSecond[userID] =
//                    Date() //Save new date to retrieve newer posts
//            }
//        }
        for (x in 0 until 3) {

            val listPosts = getUserPostsByDateLessAndGreater(
                userID,
                dateLessThanEpochSeconds,
                dateGreaterThanEpochSeconds
            )

            newUserMutableListPosts.addAll(listPosts)

            minusDays += 30
            dateLessThanEpochSeconds =
                Instant.ofEpochSecond(dateGreaterThanEpochSeconds).epochSecond
            dateGreaterThanEpochSeconds =
                Instant.ofEpochSecond(dateGreaterThanEpochSeconds)
                    .minus(minusDays, ChronoUnit.DAYS).epochSecond
        }

        //Save dates on local
        savedUserHashMapMinusDays[userID] = minusDays
        savedUserHashMapDatesEpochSecond[userID] =
            mutableListOf(dateLessThanEpochSeconds, dateGreaterThanEpochSeconds)

        for (userPosts in newUserMutableListPosts) {
            userContainsSavedPosts?.add(userPosts)
        }

        var tempUserContainsSavedPosts: List<Post> = emptyList()

        userContainsSavedPosts?.let {
            tempUserContainsSavedPosts = it.toList()
        }

        if (userContainsSavedPosts != null) {
            val userContainedSavedPostsSortedByDescending =
                tempUserContainsSavedPosts.sortedByDescending { post -> post.createdAt }.toMutableList()
            savedUserHashMapPost[userID] = userContainedSavedPostsSortedByDescending

            emit(ResultData.Success(userContainedSavedPostsSortedByDescending))

        } else {
            savedUserHashMapPost.remove(userID)
            savedUserHashMapMinusDays.remove(userID)
            savedUserHashMapDatesEpochSecond.remove(userID)
            emit(ResultData.Error(Exception("Sorry we couldn't load user posts, try again later")))
        }
    }

    suspend fun retrieveUserNewerPosts(userID: String): Flow<ResultData<List<Post>>> = flow {
        val dateUserRetrieveNewerPosts = savedUserHashMapRetrieveNewerPostsDateEpochSecond[userID]

        val listNewerPosts: MutableList<Post>

        if (dateUserRetrieveNewerPosts == null) {
            savedUserHashMapRetrieveNewerPostsDateEpochSecond[userID] = Instant.now().epochSecond
            emit(ResultData.Success(savedUserHashMapPost[userID]))
        } else {
            listNewerPosts = getNewerPosts(userID, dateUserRetrieveNewerPosts).toMutableList()

            if (listNewerPosts.isEmpty()) {
                emit(ResultData.Success(savedUserHashMapPost[userID]))
            } else {
                val newUserListPosts = savedUserHashMapPost[userID]

                listNewerPosts.onEach { post ->
                    newUserListPosts?.add(post)
                }

                val newUserListPostsSortedByDescending =
                    newUserListPosts?.sortedByDescending { post -> post.createdAt }?.toMutableList()

                if (newUserListPostsSortedByDescending != null) {
                    savedUserHashMapPost[userID] = newUserListPostsSortedByDescending
                    emit(ResultData.Success(savedUserHashMapPost[userID]))
                } else {
                    emit(ResultData.Success(savedUserHashMapPost[userID]))

                }
            }
        }
    }

    private suspend fun getNewerPosts(userID: String, dateRetrieveNewerPostsEpochSeconds: Long): List<Post> {
        return getUserPostsByDateLessAndGreater(
            userID,
            Instant.now().epochSecond,
            dateRetrieveNewerPostsEpochSeconds
        )
    }

    private suspend fun getUserPostsByDateLessAndGreater(
        userID: String,
        dateLessThanEpochSeconds: Long,
        dateGreaterThanEpochSeconds: Long
    ): List<Post> {
//        val userDocumentUID = getUserDocumentUID(userID)

        return firebaseSource.db
            .collection("posts")
            .document(userID)
            .collection(firebasePath.user_posts)
            .whereLessThan("createdAt", dateLessThanEpochSeconds)
            .whereGreaterThan("createdAt", dateGreaterThanEpochSeconds)
            .get()
            .await()
            .toObjects(Post::class.java)

    }

    //TODO, SET COOLDOWN 1 MINUTE
    private fun returnMinutesDifference(dateRetrieveNewerPostsTime: Long): Long {
        val diffInMillisecond = Date().time - dateRetrieveNewerPostsTime

        return TimeUnit.MILLISECONDS.toMinutes(diffInMillisecond)
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
//        val userDocumentUID = getUserDocumentUID(userID)

        var countPostsDocRef: DocumentReference? = null
        countPostsDocRef = firebaseSource.db.collection(firebasePath.posts_col).document()
            .collection(firebasePath.user_count_posts).document(firebasePath.countPosts)


        var countPosts: CountsPosts? = null
        countPostsDocRef.let {
            countPosts = it
                .get()
                .await()
                .toObject(CountsPosts::class.java)
        }

        countPosts?.let { it ->
            return it.countPosts
        } ?: kotlin.run {
            return 0
        }
    }

    suspend fun getUserProfile(userUID: String): Flow<ResultData<User>> = flow {
        emit(ResultData.Loading)
        var user: User? = null

        var documentUID: String = ""

        if (userUID == firebaseSource.user?.userId) {
            user = firebaseSource.user
        } else {
            firebaseSource.db.collection("users")
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
                user = firebaseSource.db.collection("users").document(documentUID)
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

    suspend fun checkIfCurrentUserFollowsOtherUser(userID: String): Flow<ResultData<Boolean>> =
        flow {
            emit(ResultData.Loading)

            val isOtherUsernameInHashMap =
                hashMapCurrentUserFollowsOtherUser.containsKey(userID)

            val currentUserId = firebaseSource.userId

            if (currentUserId.isEmpty()) {
                throw Exception("Can't get userID")
            }


            if (isOtherUsernameInHashMap) {
                emit(ResultData.Success(isOtherUsernameInHashMap))
            } else {
                try {
                    val userFollowingRef = firebaseSource.db
                        .collection(firebasePath.following_col)
                        .document(currentUserId)
                        .collection(firebasePath.user_following)

                    val followingUser = userFollowingRef
                        .whereEqualTo("followingId", userID)
                        .get()
                        .await()
                        .toObjects(Following::class.java)

                    when {
                        followingUser.isEmpty() -> {
                            emit(ResultData.Success(false))
                        }

                        followingUser.size >= 2 -> {
                            emit(ResultData.Success(false))
                        }

                        else -> emit(ResultData.Success(true))
                    }
                } catch (e: Exception) {
                    emit(ResultData.Error(e))
                }
            }
        }

    private fun checkIfNewFollowingHasBeenUnfollowedBefore(followingId: String): Boolean {
        return firebaseSource.listNewUnfollowingsUsername.contains(followingId)
    }

    private fun removeNewFollowingFromUnfollowing(followingId: String) {
        firebaseSource.listNewUnfollowingsUsername.remove(followingId)
    }

    suspend fun followOtherUser(otherUser: User): Flow<ResultData<Boolean>> = flow {
        val successAddFollowing = addCurrentUserFollowingOtherUser(otherUser)

        if (!successAddFollowing) {
            emit(ResultData.Error(Exception("Sorry, there was an error in our servers, try again later")))
            return@flow
        }

        val successAddFollower = addOtherUserFollower(otherUser)

        if (successAddFollowing && successAddFollower) {
            emit(ResultData.Success(successAddFollowing))
        } else {
            emit(ResultData.Error(Exception("Sorry, there was an error in our servers, try again later")))
        }
    }

    private suspend fun addCurrentUserFollowingOtherUser(otherUser: User): Boolean {

//        if (isCurrentUserDocumentUidNull())
//            return false

//        val userFollowingDocumentUID = userDocumentUID!!.followingDocumentUid

        val currentUserId = firebaseSource.userId

        if (currentUserId.isEmpty())
            throw Exception("userId is empty, couldn't get addCurrentUserFollowingOtherUser")

        var hasUserBeenFollowed = false
        Log.i(TAG, "UserFollowingDocumentID= $currentUserId")
        try {
            val followingRef =
                firebaseSource.db.collection("following/${currentUserId}/${firebasePath.user_following}")


            val followingUser = Following(otherUser.userId)

            followingRef
                .document()
                .set(followingUser)
                .addOnSuccessListener {
                    hasUserBeenFollowed = true
                }
                .addOnFailureListener {
                    hasUserBeenFollowed = false
                }
                .await()
        } catch (e: Exception) {
            return false
        }

        if (hasUserBeenFollowed) {
            hashMapCurrentUserFollowsOtherUser[otherUser.username] = true
            firebaseSource.addToNewFollowingList(otherUser.userId)
            updateFollowingCount(true)
        }

        return hasUserBeenFollowed
    }

    private suspend fun addOtherUserFollower(otherUser: User): Boolean {
//        if (isCurrentUserDocumentUidNull())
//            return false

//        val userFollowerDocumentUID = getUserDocumentUID(otherUser.userId)

        val currentUserId = firebaseSource.userId

        var otherUserHasFollower = false
        try {
//            val followingRef =
//                firebaseSource.db.collection("${firebasePath.follower_col}/${firebaseSource.user?.userId}/${firebasePath.user_followers}")
            val followingRef =
                firebaseSource.db.collection(firebasePath.follower_col).document(otherUser.userId)
                    .collection(firebasePath.user_followers)


            var followingUser = Follower()
            firebaseSource.user?.run {
                followingUser = Follower(userId)
            }
            if (followingUser.followerId.isEmpty())
                throw Exception("Stop")

            followingRef
                .document()
                .set(followingUser)
                .addOnSuccessListener {
                    otherUserHasFollower = true
                    Log.i(TAG, "Success following")
                }
                .addOnFailureListener {
                    otherUserHasFollower = false
                }
                .await()

        } catch (e: Exception) {
            return false
        }

        if (otherUserHasFollower) {
            updateFollowerCount(otherUser.username, true)
        }

        return otherUserHasFollower
    }

    private fun checkIfNewUnfollowingHasBeenFollowedBefore(followingUsername: String): Boolean {
        return firebaseSource.listNewFollowingsUserID.contains(followingUsername)
    }

    private fun removeNewUnfollowingFromFollowing(followingUsername: String) {
        firebaseSource.listNewFollowingsUserID.remove(followingUsername)
    }

//    private fun checkIfNewUnfollowingHasBeenUnfollowedBefore(followingUsername: String): Boolean{
//        return firebaseSource.listNewUnfollowingsUsername.contains(followingUsername)
//    }
//
//    private fun removeNewUnfollowingFromUnfollowing(followingUsername: String){
//        firebaseSource.listNewUnfollowingsUsername.remove(followingUsername)
//    }

    suspend fun unfollowOtherUser(otherUser: User): Flow<ResultData<Boolean>> = flow {

        val removedFollowing = removeFollowing(otherUser)
        val removedFollower = removeFollower(otherUser)

        Log.i("SeeFollowings", "Following = ${removedFollowing} and Follower = ${removedFollower}")

        if (removedFollowing && removedFollower) {
            emit(ResultData.Success(true))
        } else {
            emit(ResultData.Error(Exception("Sorry, there was an error in our servers, try again later")))
        }
    }

    private suspend fun removeFollowing(otherUser: User): Boolean {
//        if (isCurrentUserDocumentUidNull())
//            return false

//        val userFollowingDocumentUID = userDocumentUID!!.followingDocumentUid
        var removedFollowing = false

        val currentUserId = firebaseSource.userId

        if (currentUserId.isEmpty())
            throw Exception("userId is empty, couldn't get addCurrentUserFollowingOtherUser")

        var documentID = ""
        val followingCollectionRef =
            firebaseSource.db.collection(firebasePath.following_col).document(currentUserId)
                .collection(firebasePath.user_following)
        try {

            followingCollectionRef
                .whereEqualTo("followingId", otherUser.userId)
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        return@addOnSuccessListener
                    }
                    for (document in documents) {
                        documentID = document.id
                    }
                }.await()
            if (documentID.isEmpty()) {
                return false
            }

            followingCollectionRef
                .document(documentID)
                .delete()
                .addOnSuccessListener {
                    removedFollowing = true
                }
                .addOnFailureListener {
                    removedFollowing = false
                }.await()

        } catch (e: Exception) {
            removedFollowing = false
        }

        if (removedFollowing) {
            //Update local lists unfollowing
            if (hashMapCurrentUserFollowsOtherUser.containsKey(otherUser.username))
                hashMapCurrentUserFollowsOtherUser.remove(otherUser.username)

            firebaseSource.addToNewUnfollowingList(otherUser.userId)

            //substract 1 count of following and follower
            updateFollowingCount(false)
        }

        return removedFollowing
    }

    private suspend fun removeFollower(otherUser: User): Boolean {
//        if (isCurrentUserDocumentUidNull())
//            return false

//        val userFollowerDocumentUID = getUserDocumentUID(otherUser.userId)?.followerDocumentUid ?: return false

        var hasUserBeenUnfollowed = false
        var documentID = ""

        val currentUserId = firebaseSource.userId

        val followerCollectionRef =
            firebaseSource.db.collection(firebasePath.follower_col).document(otherUser.userId)
                .collection(firebasePath.user_followers)
        try {
            followerCollectionRef.whereEqualTo("followerId", currentUserId)
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        documentID = document.id
                    }
                    if (documents.isEmpty) {
                        return@addOnSuccessListener
                    }

                    for (document in documents) {
                        documentID = document.id
                    }
                }.addOnFailureListener {
                    Log.i(TAG, "Failure")
                }.await()

            if (documentID.isEmpty()) {
                return false
            }

            followerCollectionRef
                .document(documentID)
                .delete()
                .addOnSuccessListener {
                    hasUserBeenUnfollowed = true
                }
                .addOnFailureListener {
                    hasUserBeenUnfollowed = false
                }.await()

        } catch (e: Exception) {
            hasUserBeenUnfollowed = false
        }

        if (hasUserBeenUnfollowed) {
            //Update local lists unfollowing
            if (checkIfNewUnfollowingHasBeenFollowedBefore(otherUser.username))
                removeNewUnfollowingFromFollowing(otherUser.username)

            firebaseSource.listNewUnfollowingsUsername.add(otherUser.userId)

            //substract 1 count of following and follower
            updateFollowerCount(otherUser.username, false)
        }

        return hasUserBeenUnfollowed
    }

//    private fun isCurrentUserDocumentUidNull(): Boolean {
//        if (userDocumentUID == null) {
//            firebaseSource.userDocumentUID?.let {
//                userDocumentUID = it
//            } ?: kotlin.run {
//                return true
//            }
//        }
//        return false
//    }

    private suspend fun updateFollowingCount(didCurrentUserFollowOtherUser: Boolean) {
        val userDocumentRef =
            firebaseSource.db.collection(firebasePath.users_col).document(firebaseSource.username)
        try {
            if (didCurrentUserFollowOtherUser) {
                userDocumentRef.update("following", FieldValue.increment(1))
                    .addOnSuccessListener {
                        firebaseSource.user?.run {
                            following += 1
                        }
                    }.addOnFailureListener {

                    }.await()
            } else {
                userDocumentRef.update("following", FieldValue.increment(-1))
                    .addOnSuccessListener {
                        firebaseSource.user?.run {
                            following -= 1
                        }
                    }.addOnFailureListener {

                    }.await()
            }
        } catch (e: Exception) {
            Log.i(TAG, "Error Update Follower Count: $e")
        }
    }

    private suspend fun updateFollowerCount(username: String, didOtherUserGetFollowed: Boolean) {
        val userDocumentRef = firebaseSource.db.collection("users").document(username)

        try {
            if (didOtherUserGetFollowed) {
                userDocumentRef.update("follower", FieldValue.increment(1))
                    .addOnSuccessListener {
                        Log.i("CheckUpdate", "Updated")
                    }.addOnFailureListener {
                        Log.i("CheckUpdate", "Not updated")
                    }.await()
            } else {
                userDocumentRef.update("follower", FieldValue.increment(-1))
                    .addOnSuccessListener {
                        Log.i("CheckUpdate", "Updated")
                    }.addOnFailureListener {
                        Log.i("CheckUpdate", "Not updated")
                    }.await()
            }
        } catch (e: Exception) {
            Log.i("CheckUpdate", "Error updateFollowerCount: $e")
        }
    }

//    private suspend fun getUserDocumentUID(userID: String): UserDocumentUID? {
//        return firebaseSource.getUserDocumentUID(userID)
//    }

    suspend fun getCurrentUser(): Flow<ResultData<User>> = flow {
        val user = firebaseSource.getCurrentUser()

        if (user.isEmpty()) {
            firebaseSource.setCurrentUser()
            emit(ResultData.Error(Exception("Sorry, user is empty"), null))
        } else {
            emit(ResultData.Success(user))
        }
    }
}
