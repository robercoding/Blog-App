package com.rober.blogapp.data.network.firebase

import android.util.Log
import com.rober.blogapp.data.ResultData
import com.rober.blogapp.data.network.util.FirebasePath
import com.rober.blogapp.entity.Following
import com.rober.blogapp.entity.Post
import com.rober.blogapp.entity.User
import com.rober.blogapp.entity.UserDocumentUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import org.threeten.bp.Instant
import org.threeten.bp.temporal.ChronoUnit
import java.lang.Exception
import javax.inject.Inject
import kotlin.collections.HashMap

class FirebaseFeedManager
@Inject
constructor
    (
    private val firebaseSource: FirebaseSource,
    private val firebasePath: FirebasePath
) {
    private val TAG = "FirebaseFeedManager"

    //Local "DB", in every session
    private var savedFeedHashMapPosts: HashMap<String, MutableList<Post>> = hashMapOf()
    private var savedFeedListPosts: MutableList<Post> = mutableListOf()
    private var savedListFollowing: MutableList<Following>? = null

    //Time
    private var dateToRetrieveNewerPostsEpochSeconds: Long? = null //
    private var dateLessThanEpochSeconds: Long = 0
    private var dateGreaterThanEpochSeconds: Long = 0

    private var restDays: Long = 0
    private var currentIntervalHoursIndex = 0
    private var listIntervalEightHours = listOf(0, 8, 16, 24) //For people with less following
    //    private var listIntervalFourHours = listOf(0, 4, 8, 12, 16, 20, 24) //for people who has more followings

    //Boolean flags
    private var endOfTimeline = false
    private var user = User()

    suspend fun getInitFeedPosts(): Flow<ResultData<List<Post>>> = flow {
        emit(ResultData.Loading)

        firebaseSource.user?.run {
            user = this
        }

        if (hasUserChangedHisUsername()) {
            changeHashMapPostsUsername()
        }

        if (checkIfNewFollowings()) {
            val listPostsFromNewFollowings = getPostsFromNewFollowings()
            savedFeedListPosts.addAll(listPostsFromNewFollowings)

            savedFeedListPosts = savedFeedListPosts.sortedByDescending { post -> post.created_at }.toMutableList()
        }

        if (checkIfNewUnfollowings()) {
            removeUnfollowingsFromLocalLists()
        }

        if (!savedFeedListPosts.isNullOrEmpty()) { //Send listPosts if there's already posts saved in cache
            emit(ResultData.Success(savedFeedListPosts))
        } else { //Get posts every 30 days
            //If dateNewer is not null, then set from datenewer and rest days
            dateToRetrieveNewerPostsEpochSeconds?.also {tempDateToRetrieveNewerPostsEpochSeconds->
                dateLessThanEpochSeconds = Instant.ofEpochSecond(tempDateToRetrieveNewerPostsEpochSeconds).epochSecond
                dateGreaterThanEpochSeconds = Instant.ofEpochSecond(tempDateToRetrieveNewerPostsEpochSeconds).minus(restDays + 30, ChronoUnit.DAYS).epochSecond
            }?: kotlin.run {
                dateToRetrieveNewerPostsEpochSeconds = Instant.now().epochSecond
                dateLessThanEpochSeconds = Instant.now().minus(restDays, ChronoUnit.DAYS).epochSecond
                dateGreaterThanEpochSeconds = Instant.now().minus(restDays + 30, ChronoUnit.DAYS).epochSecond
            }


            try {
                //Get all user followings
                if (savedListFollowing.isNullOrEmpty()) {
                    savedListFollowing = firebaseSource.followingList
                }

                var newListFollowing = emptyList<Following>()
                if (!savedListFollowing.isNullOrEmpty())
                    newListFollowing = savedListFollowing!!.toList()

                var countTotalPosts = 0
                var getPostsTries = 0
                while (getPostsTries < 5) {
                    getPostsTries += 1

                    if (!newListFollowing.isNullOrEmpty()) {
                        for (following in newListFollowing) {
                            var tempListUserPostsHashMap = mutableListOf<Post>()

                            if (savedFeedHashMapPosts.containsKey(following.following_id))
                                tempListUserPostsHashMap = savedFeedHashMapPosts.getValue(following.following_id)

                            val listFollowingNewPosts = getFollowingPostsByLessAndGreaterThan(
                                following.following_id,
                                dateLessThanEpochSeconds,
                                dateGreaterThanEpochSeconds
                            )

                            if (listFollowingNewPosts.isNotEmpty()) {
                                tempListUserPostsHashMap.addAll(listFollowingNewPosts)
                                countTotalPosts += listFollowingNewPosts.size
                                savedFeedHashMapPosts[following.following_id] = tempListUserPostsHashMap
                            }
                        }
                    }

                    //Get User Logged In posts
                    val listUserLoggedInNewPosts = getFollowingPostsByLessAndGreaterThan(
                        user.user_id,
                        dateLessThanEpochSeconds,
                        dateGreaterThanEpochSeconds
                    )

                    var listPostsCurrentUserFromHashMap = mutableListOf<Post>()
                    //Get localPosts from current user
                    if (savedFeedHashMapPosts.containsKey(user.user_id))
                        listPostsCurrentUserFromHashMap = savedFeedHashMapPosts.getValue(user.user_id)

                    if (listUserLoggedInNewPosts.isNotEmpty()) {
                        listPostsCurrentUserFromHashMap.addAll(listUserLoggedInNewPosts)
                        countTotalPosts += listUserLoggedInNewPosts.size
                        savedFeedHashMapPosts[user.user_id] = listPostsCurrentUserFromHashMap
                    }

                    restDays += 30
                    dateLessThanEpochSeconds = dateGreaterThanEpochSeconds
                    Log.i("DateLess", "before $dateLessThanEpochSeconds")
//                        dateLessThanInitEpochSeconds = Instant.now().minus(restDays, ChronoUnit.DAYS).epochSecond
                    dateGreaterThanEpochSeconds =
                        Instant.ofEpochSecond(dateGreaterThanEpochSeconds).minus(restDays + 30, ChronoUnit.DAYS).epochSecond
                    Log.i("DateLess", "after $dateLessThanEpochSeconds")

                }
                val allPosts = mutableListOf<Post>()
                //get all actual values from Map
                savedFeedHashMapPosts.mapKeys { mapEntry ->
                    allPosts.addAll(mapEntry.value)
                }

                Log.i("CheckPosts", "Final = ${allPosts.size}")

                //Order values
                val feedPostsOrdered =
                    allPosts.sortedByDescending { post -> post.created_at }
                        .toMutableList()

                //Send them
                savedFeedListPosts = feedPostsOrdered
                emit(ResultData.Success(savedFeedListPosts))
            } catch (exception: Exception) {
                emit(ResultData.Error<List<Post>>(exception, null))
            }
        }
    }

    private suspend fun getFollowingPostsByLessAndGreaterThan(
        followingId: String,
        dateLessThanInit: Long?,
        dateGreaterThanInit: Long?
    ): List<Post> {
//        Log.i("CheckFollowing", "DateLess = $dateLessThanInit and $dateGreaterThanInit")
        var followingDocumentUID = UserDocumentUID()
        if (followingId == user.user_id)
            firebaseSource.userDocumentUID?.run { followingDocumentUID = this }!!
        else
            followingDocumentUID = getUserDocumentUID(followingId) ?: return emptyList()

        if (followingDocumentUID.userUid == "")
            return emptyList()

        return if (dateLessThanInit != null && dateGreaterThanInit != null)
            firebaseSource.db.collection("${firebasePath.posts_col}/${followingDocumentUID.postsDocumentUid}/${firebasePath.user_posts}")
                .whereLessThan("created_at", dateLessThanInit)
                .whereGreaterThan("created_at", dateGreaterThanInit)
                .get()
                .await()
                .toObjects(Post::class.java)
        else
            emptyList()
    }

    suspend fun getNewFeedPosts(): Flow<ResultData<List<Post>>> = flow {
        emit(ResultData.Loading)

        try {
            if (savedListFollowing.isNullOrEmpty())
                savedListFollowing = getUserFollowings().toMutableList()

            val newListPosts = mutableListOf<Post>()

            for (following in savedListFollowing!!) {
                val tempListNewPostsFromFollowing = getListPostsFromIdByGreaterDate(
                    following.following_id,
                    dateToRetrieveNewerPostsEpochSeconds
                )

                //Get old local posts from following if exists
                var mapSavedPostsFromFollowing = mutableListOf<Post>()
                if (savedFeedHashMapPosts.containsKey(following.following_id))
                    mapSavedPostsFromFollowing = savedFeedHashMapPosts.getValue(following.following_id)

                //If there are new posts then add to local map and add to outter scope list
                if (tempListNewPostsFromFollowing.isNotEmpty()) {
                    mapSavedPostsFromFollowing.addAll(tempListNewPostsFromFollowing)
                    savedFeedHashMapPosts[following.following_id] = mapSavedPostsFromFollowing

                    newListPosts.addAll(tempListNewPostsFromFollowing)
                    savedFeedListPosts.addAll(tempListNewPostsFromFollowing)
                }
            }

            val listUserPosts =
                getListPostsFromIdByGreaterDate(
                    user.user_id,
                    dateToRetrieveNewerPostsEpochSeconds
                ).toMutableList()

            if (listUserPosts.isNotEmpty()) {
                var mapSavedPostsFromUserLoggedIn = mutableListOf<Post>()
                if(savedFeedHashMapPosts.containsKey(user.user_id))
                    mapSavedPostsFromUserLoggedIn.addAll(savedFeedHashMapPosts.getValue(user.user_id))
                mapSavedPostsFromUserLoggedIn.addAll(listUserPosts)
                mapSavedPostsFromUserLoggedIn =
                    mapSavedPostsFromUserLoggedIn.sortedByDescending { post -> post.created_at }.toMutableList()
                savedFeedHashMapPosts[user.user_id] = mapSavedPostsFromUserLoggedIn

                newListPosts.addAll(listUserPosts)
                savedFeedListPosts.addAll(listUserPosts)
            }

            savedFeedListPosts = savedFeedListPosts.sortedByDescending { post -> post.created_at }.toMutableList()

            if (newListPosts.size > 0) {
                dateToRetrieveNewerPostsEpochSeconds = Instant.now().epochSecond
                newListPosts.sortedByDescending { post -> post.created_at }
//                for (post in newListPosts)
//                    savedFeedListPosts.add(0, post)
                //Add the newest posts the first and the next posts follow the queue
            }
            emit(ResultData.Success(newListPosts))

        } catch (e: Exception) {
            emit(ResultData.Error(e))
        }
    }

    private suspend fun getListPostsFromIdByGreaterDate(
        followingId: String,
        dateGreater: Long?
    ): List<Post> {
        val followingUserDocumentUID: UserDocumentUID?

        if (followingId != user.user_id) {
            followingUserDocumentUID = getUserDocumentUID(followingId) ?: return emptyList()
        } else {
            followingUserDocumentUID = firebaseSource.userDocumentUID?.run { this } ?: return emptyList()
        }

        return if (dateGreater != null)
            firebaseSource.db.collection("${firebasePath.posts_col}/${followingUserDocumentUID.postsDocumentUid}/${firebasePath.user_posts}")
                .whereGreaterThan("created_at", dateGreater)
                .get()
                .await()
                .toObjects(Post::class.java)
        else
            emptyList()
    }

    suspend fun getOldFeedPosts(): Flow<ResultData<List<Post>>> = flow {
        emit(ResultData.Loading)

        var triesRetrieveOldFeedPost = 0

        dateLessThanEpochSeconds = Instant.now().minus(restDays, ChronoUnit.DAYS)
            .minus(listIntervalEightHours[currentIntervalHoursIndex].toLong(), ChronoUnit.HOURS).epochSecond

        dateGreaterThanEpochSeconds = Instant.now().minus(restDays, ChronoUnit.HOURS)
            .minus(listIntervalEightHours[currentIntervalHoursIndex + 1].toLong(), ChronoUnit.HOURS).epochSecond

        //Get Followings
        if (savedListFollowing.isNullOrEmpty()) {
            val listFollowing = getUserFollowings()
            savedListFollowing = listFollowing.toMutableList()
        }

        val newListUsersPosts: MutableList<Post> = mutableListOf()

        while (newListUsersPosts.size < 10 && triesRetrieveOldFeedPost <= 15) {
            //Get following posts
            for (following in savedListFollowing!!) {
                val listFollowingPosts = getFollowingPostsByLessAndGreaterThan(
                    following.following_id,
                    dateLessThanEpochSeconds,
                    dateLessThanEpochSeconds
                )
                newListUsersPosts.addAll(listFollowingPosts)
            }

            //Get User Logged In posts
            val listUserLoggedInPosts = getFollowingPostsByLessAndGreaterThan(
                user.user_id,
                dateLessThanEpochSeconds,
                dateGreaterThanEpochSeconds
            )

            newListUsersPosts.addAll(listUserLoggedInPosts)

            if (currentIntervalHoursIndex != 2) {
                currentIntervalHoursIndex += 1
            } else {
                currentIntervalHoursIndex = 0
                restDays += 1
            }

            if (newListUsersPosts.size < 10 && triesRetrieveOldFeedPost < 15) {
                triesRetrieveOldFeedPost += 1

                if (triesRetrieveOldFeedPost >= 3 && newListUsersPosts.size < 5) {
                    restDays += 1

                    dateLessThanEpochSeconds = Instant.now().minus(restDays - 1, ChronoUnit.DAYS).epochSecond
                    dateGreaterThanEpochSeconds = Instant.now().minus(restDays, ChronoUnit.DAYS).epochSecond

                } else {
                    dateLessThanEpochSeconds = Instant.now().minus(restDays, ChronoUnit.DAYS)
                        .minus(listIntervalEightHours[currentIntervalHoursIndex].toLong(), ChronoUnit.DAYS).epochSecond

                    dateGreaterThanEpochSeconds = Instant.now().minus(restDays, ChronoUnit.DAYS)
                        .minus(listIntervalEightHours[currentIntervalHoursIndex + 1].toLong(), ChronoUnit.DAYS).epochSecond

                }
            } else {
                //Leave ready the search by hour when user retrieve old posts again
                if (currentIntervalHoursIndex != 2) {
                    currentIntervalHoursIndex += 1
                } else {
                    currentIntervalHoursIndex = 0
                    restDays += 1
                }

                val feedPostsOrdered =
                    newListUsersPosts.sortedByDescending { post -> post.created_at }
                        .toMutableList()

                for (postOrdered in feedPostsOrdered)
                    savedFeedListPosts.add(postOrdered)

                //Add end of timeline
                if (feedPostsOrdered.size < 10) {
                    endOfTimeline = true
                }

                emit(ResultData.Success(savedFeedListPosts))
                break //Stop the while loop
            }
        }
    }

    //Get users that
    suspend fun getUsersFromCurrentFollowings(listUsersFollowing: List<User>): Flow<ResultData<List<User>>> = flow {
        val listUsersFollowingUserID = listUsersFollowing.map { user -> user.user_id }
        val savedListFollowingID = savedListFollowing?.map { following -> following.following_id }

        var listUsersFollowingNotContain = listOf<String>()
        savedListFollowingID?.let { savedListFollowingIDString ->
            listUsersFollowingNotContain =
                savedListFollowingIDString.filter { followingID -> !listUsersFollowingUserID.contains(followingID) }
        }

        if (listUsersFollowingNotContain.isEmpty()) {
            emit(ResultData.Success(emptyList<User>()))
            return@flow
        }

        val listNewUsersFollowingNotContain = mutableListOf<User>()
        for (userFollowingNotContain in listUsersFollowingNotContain) {
            firebaseSource.db.collection(firebasePath.users_col)
                .whereEqualTo("user_id", userFollowingNotContain)
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        val newUser = document.toObject(User::class.java)
                        listNewUsersFollowingNotContain.add(newUser)
                    }
                }
                .await()
        }

        val listToReturn = mutableListOf<User>()
        for (newUserFollowing in listNewUsersFollowingNotContain) {
            listToReturn.add(newUserFollowing)
        }

        emit(ResultData.Success(listToReturn))
    }

//    private suspend fun getFollowingPostsByDatee(followingId: String): List<Post> {
//        return firebaseSource.db.collection("posts/${followingId}/user_posts")
//            .whereGreaterThan("created_at", dateGreaterThanEpochSeconds!!)
//            .whereLessThan("created_at", dateLessThanEpochSeconds!!)
//            .get()
//            .await()
//            .toObjects(Post::class.java)
//    }

    private suspend fun getUserFollowings(): List<Following> {
        if (firebaseSource.userDocumentUID == null)
            return emptyList()

        return firebaseSource.db.collection("${firebasePath.following_col}/${firebaseSource.userDocumentUID!!.followingDocumentUid}/${firebasePath.user_following}")
            .get()
            .await()
            .toObjects(Following::class.java).toList()
    }

    private fun checkIfNewFollowings(): Boolean {
        return firebaseSource.listNewFollowingsUserID.size > 0
    }

    private suspend fun getPostsFromNewFollowings(): MutableList<Post> {
        return getListPostsFromNewFollowings().toMutableList()
    }

    //Retrieve all posts from the new followings, the newest one to the oldest one of the actual timeline
    private suspend fun getListPostsFromNewFollowings(): List<Post> {
        val listNewFollowingsUserID = firebaseSource.listNewFollowingsUserID
        val listNewUsernamesFollowingPosts = mutableListOf<Post>()

        if (listNewFollowingsUserID.isNotEmpty()) {
            for (newFollowingUserID in listNewFollowingsUserID) {

                var newFollowingListPosts: List<Post>

                newFollowingListPosts = getFollowingPostsByLessAndGreaterThan(
                    newFollowingUserID,
                    dateToRetrieveNewerPostsEpochSeconds,
                    dateGreaterThanEpochSeconds
                ).toMutableList()

                savedFeedHashMapPosts[newFollowingUserID] = newFollowingListPosts
                listNewUsernamesFollowingPosts.addAll(newFollowingListPosts)

                firebaseSource.listNewFollowingsUserID.remove(newFollowingUserID)
                addFollowingToSavedListFollowing(newFollowingUserID)
            }
        }
        return listNewUsernamesFollowingPosts
    }

    //Get Following object from firestore and add local list following
    private suspend fun addFollowingToSavedListFollowing(followingUserUID: String) {
//        val followingDocumentUID = getUserDocumentUID(followingUserUID) ?: return
        var followingDocumentUID = ""
        firebaseSource.userDocumentUID?.followingDocumentUid?.also { tempFollowingDocumentUID ->
            followingDocumentUID = tempFollowingDocumentUID
        }
        if (followingDocumentUID.isEmpty()) {
            return
        }
        var followingToSave: Following? = null

        firebaseSource.db.collection(firebasePath.following_col).document(followingDocumentUID)
            .collection(firebasePath.user_following)
            .whereEqualTo("following_id", followingUserUID)
            .get()
            .addOnSuccessListener { documents ->

                for (document in documents) {
                    followingToSave = document.toObject(Following::class.java)
                }
            }
            .await()

        followingToSave?.run {
            savedListFollowing?.add(this)

        } ?: kotlin.run {
            savedFeedListPosts.removeAll { post -> post.userCreatorId == followingUserUID }
        }
    }

    private fun checkIfNewUnfollowings(): Boolean {
        return firebaseSource.listNewUnfollowingsUsername.size > 0
    }

    private fun removeUnfollowingsFromLocalLists() {
        val unfollowingUsers = firebaseSource.listNewUnfollowingsUsername

        for (unfollowingUser in unfollowingUsers) {
            savedFeedHashMapPosts.remove(unfollowingUser)
            savedListFollowing?.removeAll { following -> following.following_id == unfollowingUser }
            savedFeedListPosts.removeAll { post -> post.userCreatorId == unfollowingUser }
            firebaseSource.listNewFollowingsUserID.remove(unfollowingUser)
        }
    }

    private fun hasUserChangedHisUsername(): Boolean {
        return firebaseSource.userChangedUsername
    }

    private suspend fun getUserDocumentUID(userID: String): UserDocumentUID? {
        return firebaseSource.getUserDocumentUID(userID)
    }

    //When username change it updates local saved posts
    private fun changeHashMapPostsUsername() {
        val previousUsername = firebaseSource.usernameBeforeChange
        val previousUsernameListPosts = savedFeedHashMapPosts[previousUsername]

        val listPostsOfNewUsername = mutableListOf<Post>()
        if (previousUsernameListPosts != null) {
            for (post in previousUsernameListPosts) {
                post.userCreatorId = user.user_id
                listPostsOfNewUsername.add(post)
            }

            savedFeedHashMapPosts.remove(previousUsername)
            savedFeedListPosts.removeAll { post -> post.userCreatorId == previousUsername }

            savedFeedHashMapPosts[user.user_id] = listPostsOfNewUsername
            savedFeedListPosts.addAll(listPostsOfNewUsername)

            savedFeedListPosts = savedFeedListPosts.sortedByDescending { post -> post.created_at }.toMutableList()
        }
    }

    fun getEndOfTimeline(): Boolean = endOfTimeline
}