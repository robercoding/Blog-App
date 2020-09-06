package com.rober.blogapp.data.network.firebase

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.rober.blogapp.R
import com.rober.blogapp.data.ResultData
import com.rober.blogapp.data.network.util.FirebasePath
import com.rober.blogapp.entity.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import org.joda.time.DateTime
import java.net.URL
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.Exception


class FirebaseProfileDetailManager @Inject constructor(
    private val firebaseSource: FirebaseSource,
    private val firebasePath: FirebasePath,
    private val application: Application
) {
    private val TAG = "FirebaseProfileManager"

    private var savedUserHashMapPost = hashMapOf<String, MutableList<Post>>()
    private var savedUserHashMapDates = hashMapOf<String, MutableList<Date>>()
    private var savedUserHashMapMinusDays = hashMapOf<String, Int>()
    private var savedUserHashMapRetrieveNewerPostsDate = hashMapOf<String, Date>()

    private var hashMapCurrentUserFollowsOtherUser = hashMapOf<String, Boolean>()
    private var hashMapOthersUsersFollowings = hashMapOf<String, MutableList<Following>>()

    private var savedUsersBitmaps = hashMapOf<String, Bitmap>()
    private var savedUsersBackgroundImageUrl = hashMapOf<String, String>()

    private var userDocumentUID: UserDocumentUID? = null
    private var bitmap: Bitmap? = null

    suspend fun retrieveUserPosts(userID: String): Flow<ResultData<List<Post>>> = flow {
        emit(ResultData.Loading)
        Log.i("UserRequestPosts", "UserRequestPosts = ${userID}")

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
            Log.i("UserRequestPosts", "UserRequestPosts = ${savedUserHashMapPost[userID]}")
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

                listNewerPosts.onEach { post ->
                    newUserListPosts?.add(post)
                }

                val newUserListPostsSortedByDescending =
                    newUserListPosts?.sortedByDescending { post -> post.created_at }?.toMutableList()

                newUserListPostsSortedByDescending?.let { list ->
                    savedUserHashMapPost[userID] = list
                    emit(ResultData.Success(savedUserHashMapPost[userID]))
                } ?: kotlin.run {
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
        val countPostsDocRef = firebaseSource.db.collection(firebasePath.posts_col).document(userID)
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

    suspend fun checkIfCurrentUserFollowsOtherUser(otherUsername: String): Flow<ResultData<Boolean>> =
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

    private fun checkIfNewFollowerHasBeenFollowedBefore(followerId: String) {

    }

    suspend fun followOtherUser(otherUser: User): Flow<ResultData<Boolean>> = flow {
        val successAddFollowing = addCurrentUserFollowingOtherUser(otherUser)

        val successAddFollower = addOtherUserFollower(otherUser)

        if (successAddFollowing && successAddFollower) {
            emit(ResultData.Success(successAddFollowing))
        } else {
            emit(ResultData.Error(Exception("Sorry, there was an error in our servers, try again later")))
        }
    }

    private suspend fun addCurrentUserFollowingOtherUser(otherUser: User): Boolean {

        if (isUserDocumentUidNull())
            return false

        val userFollowingDocumentUID = userDocumentUID!!.followingDocumentUid

        var hasUserBeenFollowed = false
        try {
            val followingRef =
                firebaseSource.db.collection("following/${userFollowingDocumentUID}/${firebasePath.user_following}")

            val followingUser = Following(otherUser.username)

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
            return false
        }

        if (hasUserBeenFollowed) {
            firebaseSource.listNewFollowingsUsername.add(otherUser.username)
            if (checkIfNewFollowingHasBeenUnfollowedBefore(otherUser.username))
                removeNewFollowingFromUnfollowing(otherUser.username)
            updateFollowingCount(true)
        }

        return hasUserBeenFollowed
    }

    private suspend fun addOtherUserFollower(otherUser: User): Boolean {
        if (isUserDocumentUidNull())
            return false

        val userFollowerDocumentUID = getUserDocumentUID(otherUser.username)

        var otherUserHasFollower = false
        try {
            val followingRef =
                firebaseSource.db.collection("${firebasePath.follower_col}/$userFollowerDocumentUID/${firebasePath.user_followers}")

            val followingUser = Follower(otherUser.username)

            followingRef
                .document(followingUser.follower_id)
                .set(followingUser)
                .addOnSuccessListener {
                    otherUserHasFollower = true
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

    suspend fun unfollowOtherUser(otherUser: User): Flow<ResultData<Boolean>> = flow {

        val removedFollowing = removeFollowing(otherUser)
        val removedFollower = removeFollower(otherUser)

        if (removedFollowing && removedFollower) {
            emit(ResultData.Success(true))
        } else {
            emit(ResultData.Error(Exception("Sorry, there was an error in our servers, try again later")))
        }
    }

    suspend fun removeFollowing(otherUser: User): Boolean {
        if (isUserDocumentUidNull())
            return false

        val userFollowingDocumentUID = userDocumentUID!!.followingDocumentUid
        var removedFollowing = false

        try {
            val followingRef =
                firebaseSource.db.collection("${firebasePath.following_col}/$userFollowingDocumentUID/${firebasePath.user_following}")

            followingRef
                .document(otherUser.username)
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
            if (checkIfNewUnfollowingHasBeenFollowedBefore(otherUser.username))
                removeNewUnfollowingFromFollowing(otherUser.username)

            firebaseSource.listNewUnfollowingsUsername.add(otherUser.username)

            //substract 1 count of following and follower
            updateFollowingCount(false)
        }

        return removedFollowing
    }

    suspend fun removeFollower(otherUser: User): Boolean {
        if (isUserDocumentUidNull())
            return false

        val userFollowerDocumentUID = getUserDocumentUID(otherUser.username) ?: return false

        var hasUserBeenUnfollowed = false
        try {
            val followerRef =
                firebaseSource.db.collection("${firebasePath.follower_col}/${userFollowerDocumentUID}/${firebasePath.user_followers}")

            followerRef
                .document(firebaseSource.username)
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

            firebaseSource.listNewUnfollowingsUsername.add(otherUser.username)

            //substract 1 count of following and follower
            updateFollowerCount(otherUser.username, false)
        }

        return hasUserBeenUnfollowed
    }

    private fun isUserDocumentUidNull(): Boolean {
        if (userDocumentUID == null) {
            firebaseSource.userDocumentUID?.let {
                userDocumentUID = it
            } ?: kotlin.run {
                return true
            }
        }
        return false
    }

    suspend fun updateFollowingCount(didCurrentUserFollowOtherUser: Boolean) {
        val userDocumentRef =
            firebaseSource.db.collection(firebasePath.users_col).document(firebaseSource.username)
        try {
            if (didCurrentUserFollowOtherUser) {
                userDocumentRef.update("following", FieldValue.increment(1))
                    .addOnSuccessListener {

                    }.addOnFailureListener {

                    }.await()
            } else {
                userDocumentRef.update("following", FieldValue.increment(-1))
                    .addOnSuccessListener {

                    }.addOnFailureListener {

                    }.await()
            }
        } catch (e: Exception) {
            Log.i("CheckUpdate", "Exception")
        }
    }

    suspend fun updateFollowerCount(userID: String, didOtherUserGetFollowed: Boolean) {
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

    private suspend fun getUserDocumentUID(userID: String): UserDocumentUID? {
        var userDocumentUID: UserDocumentUID? = null

        val userDocumentUIDQuery =
            firebaseSource.db.collection(firebasePath.user_documents_uid).whereEqualTo("username", userID)

        userDocumentUIDQuery
            .get()
            .addOnSuccessListener {
                if (it.isEmpty)
                    return@addOnSuccessListener

                val listUserDocumentUID = it.toObjects(UserDocumentUID::class.java)
                if (listUserDocumentUID.isEmpty())
                    return@addOnSuccessListener

                when (listUserDocumentUID.size) {
                    1 -> userDocumentUID = listUserDocumentUID[0]
                    else -> return@addOnSuccessListener
                }
            }.await()
        return userDocumentUID
    }

    suspend fun getCurrentUser(): Flow<ResultData<User>> = flow {
        val user = firebaseSource.getCurrentUser()

        if (user.isEmpty()) {
            firebaseSource.setCurrentUser()
            emit(ResultData.Error(Exception("Sorry, user is empty"), null))
        } else {
            emit(ResultData.Success(user))
        }
    }

    suspend fun getBitmapLightWeight(user: User): Flow<ResultData<Bitmap>> = flow {



        if(bitmap != null){
            bitmap?.recycle()
            bitmap = null
        }

        bitmap = createBitmap(user.backgroundImageUrl)
        emit(ResultData.Success(bitmap))
    }

    suspend fun getBitmap(user: User): Flow<ResultData<Bitmap>> = flow {

        Log.i("BackgroundBitmap", "Contains? ${savedUsersBitmaps.containsKey(user.username)}")
        if (savedUsersBackgroundImageUrl.containsKey(user.username) && savedUsersBitmaps.containsKey(user.username)) {
            if (user.backgroundImageUrl != savedUsersBackgroundImageUrl.get(user.username)) {
                Log.i("BackgroundBitmap", "DifferentbackgroundUrl lets create")

                savedUsersBitmaps[user.username] = createBitmap(user.backgroundImageUrl)
                savedUsersBackgroundImageUrl[user.username] = user.backgroundImageUrl
            }
        } else {
            Log.i("BackgroundBitmap", "No contains, lets create")
            savedUsersBitmaps[user.username] = createBitmap(user.backgroundImageUrl)
            savedUsersBackgroundImageUrl[user.username] = user.backgroundImageUrl
            Log.i("BackgroundBitmap", "Now contains? ${savedUsersBitmaps.containsKey(user.username)}")
        }

        Log.i("BackgroundBitmap", "Send")
        emit(ResultData.Success(savedUsersBitmaps[user.username]))
    }

    private fun createBitmap(newBackgroundImageUrl: String): Bitmap {
        var bitmapTemp: Bitmap? = null

        Thread(Runnable {
            try {
                val url = URL(newBackgroundImageUrl)
                val options = BitmapFactory.Options()
                options.inSampleSize = 8
                bitmapTemp = BitmapFactory.decodeStream(url.openConnection().getInputStream(), null, options)
            } catch (e: Exception) {
                System.out.println(e)
            }
        }).start()

        return bitmapTemp ?: return BitmapFactory.decodeResource(application.resources, R.drawable.black_screen)
    }
}