package com.rober.blogapp.data.network.firebase

import android.util.Log
import com.rober.blogapp.data.ResultData
import com.rober.blogapp.data.network.util.FirebasePath
import com.rober.blogapp.entity.Following
import com.rober.blogapp.entity.Post
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
    private var dateLessThanEpochSeconds: Long? = null
    private var dateGreaterThanEpochSeconds: Long? = null

    private var restDays : Long = 0
    private var currentIntervalHoursIndex = 0
    private var listIntervalEightHours = listOf(0, 8, 16, 24) //For people with less following
    //    private var listIntervalFourHours = listOf(0, 4, 8, 12, 16, 20, 24) //for people who has more followings

    //Boolean flags
    private var endOfTimeline = false

    suspend fun getInitFeedPosts(): Flow<ResultData<List<Post>>> = flow {
        emit(ResultData.Loading)

        if (hasUserChangedHisUsername()) {
            Log.i("ChangedUsername", "Yes Changed")
            changeHashMapPostsUsername()
        } else {
            Log.i("ChangedUsername", "No Changed")
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
        } else { //Get posts
            dateToRetrieveNewerPostsEpochSeconds = Instant.now().epochSecond

            var dateLessThanInitEpochSeconds = Instant.now().minus(restDays, ChronoUnit.DAYS).epochSecond
            dateGreaterThanEpochSeconds = Instant.now().minus(restDays+1, ChronoUnit.DAYS).epochSecond

            try {
                //Get all user followings
                if (savedListFollowing.isNullOrEmpty()) {
                    savedListFollowing = firebaseSource.followingList
                }

                var newListFollowing = emptyList<Following>()

                if (!savedListFollowing.isNullOrEmpty()) //what?
                    newListFollowing = savedListFollowing!!.toList()

                Log.i("CheckDateLong", "Following = $savedListFollowing")


                var countTotalPosts = 0
                var getPostsTries = 0
                while (countTotalPosts < 10 && getPostsTries < 10) {
                    getPostsTries += 1

                    if (!newListFollowing.isNullOrEmpty()) {
                        for (following in newListFollowing) {
                            var tempListUserPostsHashMap = mutableListOf<Post>()

                            if (savedFeedHashMapPosts.containsKey(following.following_id))
                                tempListUserPostsHashMap = savedFeedHashMapPosts.getValue(following.following_id)

                            val listFollowingNewPosts = getFollowingPostsByLessAndGreaterThan(
                                following.following_id,
                                dateLessThanInitEpochSeconds,
                                dateGreaterThanEpochSeconds
                            )
                            Log.i("CheckDateLong", "Posts from OldRober = $listFollowingNewPosts")

                            if (listFollowingNewPosts.isNotEmpty()) {
                                tempListUserPostsHashMap.addAll(listFollowingNewPosts)
                                countTotalPosts += listFollowingNewPosts.size
                                savedFeedHashMapPosts[following.following_id] = tempListUserPostsHashMap
                            }
                        }
                    }

                    //Get User Logged In posts
                    val listUserLoggedInNewPosts = getFollowingPostsByLessAndGreaterThan(
                        firebaseSource.username,
                        dateLessThanInitEpochSeconds,
                        dateGreaterThanEpochSeconds
                    )
                    Log.i("HashMapPosts", "ListUserLoggedIn: $listUserLoggedInNewPosts")


                    var listPostsCurrentUserFromHashMap = mutableListOf<Post>()
                    //Get localPosts from current user
                    if (savedFeedHashMapPosts.containsKey(firebaseSource.username))
                        listPostsCurrentUserFromHashMap = savedFeedHashMapPosts.getValue(firebaseSource.username)

                    if (listUserLoggedInNewPosts.isNotEmpty()) {
                        listPostsCurrentUserFromHashMap.addAll(listUserLoggedInNewPosts)
                        countTotalPosts += listUserLoggedInNewPosts.size
                        savedFeedHashMapPosts[firebaseSource.username] = listPostsCurrentUserFromHashMap
                    }

                    restDays += 1
                    if (countTotalPosts < 10) {
                        dateLessThanInitEpochSeconds = Instant.now().minus(restDays, ChronoUnit.DAYS).epochSecond
                        dateGreaterThanEpochSeconds = Instant.now().minus(restDays + 1, ChronoUnit.DAYS).epochSecond
                    }
                }

                val allPosts = mutableListOf<Post>()
                savedFeedHashMapPosts.mapKeys { mapEntry ->
                    allPosts.addAll(mapEntry.value)
                }

                Log.i("HashMapPosts", "Hash ${savedFeedHashMapPosts[firebaseSource.username]}")
                Log.i("HashMapPosts", "List $allPosts")


                val feedPostsOrdered =
                    allPosts.sortedByDescending { post -> post.created_at }
                        .toMutableList()

                savedFeedListPosts = feedPostsOrdered
                Log.i(TAG, "saved feed list = $savedFeedListPosts")
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
        Log.i("CheckFollowing", "DateLess = $dateLessThanInit and $dateGreaterThanInit")
        val followingDocumentUID = getUserDocumentUID(followingId) ?: return emptyList()

//        val dateLessThanInitEpochSeconds =

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
                }
            }

            //
            val mapSavedPostsFromUserLoggedIn = mutableListOf<Post>()
            savedFeedHashMapPosts[firebaseSource.username] = mapSavedPostsFromUserLoggedIn

            val listUserPosts =
                getListPostsFromIdByGreaterDate(firebaseSource.username, dateToRetrieveNewerPostsEpochSeconds).toMutableList()

            if (listUserPosts.isNotEmpty()) {
                mapSavedPostsFromUserLoggedIn.addAll(listUserPosts)
                savedFeedHashMapPosts[firebaseSource.username] = mapSavedPostsFromUserLoggedIn

                newListPosts.addAll(listUserPosts)
            }

            dateToRetrieveNewerPostsEpochSeconds = Instant.now().epochSecond

            if (newListPosts.size > 0) {
                newListPosts.sortedByDescending { post -> post.created_at }
                for (post in newListPosts)
                    savedFeedListPosts.add(0, post)

                //Add the newest posts the first and the next posts follow the queue
                emit(ResultData.Success(savedFeedListPosts))
            } else {
                emit(ResultData.Success(savedFeedListPosts))
            }

        } catch (e: Exception) {
            emit(ResultData.Error(e))
        }
    }

    private suspend fun getListPostsFromIdByGreaterDate(
        followingId: String,
        dateGreater: Long?
    ): List<Post> {
        val followingUserDocumentUID = getUserDocumentUID(followingId) ?: return emptyList()

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

//        Log.i("CheckDate", "Date Less than: ${dateLessThanEpochSeconds}")
//        Log.i("CheckDate", "Date Greater than: ${dateGreaterThanEpochSeconds}")

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
                firebaseSource.username,
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
                    Log.i(
                        "TestDatesInTries",
                        "DAY BY DAY NOW: DateLess= ${dateLessThanEpochSeconds.toString()} && DateGreater= ${dateGreaterThanEpochSeconds.toString()}"
                    )
                } else {
                    dateLessThanEpochSeconds = Instant.now().minus(restDays, ChronoUnit.DAYS)
                        .minus(listIntervalEightHours[currentIntervalHoursIndex].toLong(), ChronoUnit.DAYS).epochSecond

                    dateGreaterThanEpochSeconds = Instant.now().minus(restDays, ChronoUnit.DAYS)
                        .minus(listIntervalEightHours[currentIntervalHoursIndex + 1].toLong(), ChronoUnit.DAYS).epochSecond

                    Log.i(
                        "TestDatesInTries",
                        "HOUR BY HOUR: DateLess= ${dateLessThanEpochSeconds.toString()} && DateGreater= ${dateGreaterThanEpochSeconds.toString()}"
                    )
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

                Log.i("Where", "Older $feedPostsOrdered")
                emit(ResultData.Success(savedFeedListPosts))
                break //Stop the while loop
            }
        }
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
        if(firebaseSource.userDocumentUID== null)
            return emptyList()

        return firebaseSource.db.collection("${firebasePath.following_col}/${firebaseSource.userDocumentUID!!.followingDocumentUid}/${firebasePath.user_following}")
            .get()
            .await()
            .toObjects(Following::class.java).toList()
    }

    private fun checkIfNewFollowings(): Boolean {
        return firebaseSource.listNewFollowingsUsername.size > 0
    }

    private suspend fun getPostsFromNewFollowings(): MutableList<Post> {
        return getListPostsFromNewFollowings().toMutableList()
    }

    //Retrieve all posts from the new followings, the newest one to the oldest one of the actual timeline
    private suspend fun getListPostsFromNewFollowings(): List<Post> {
        val listNewFollowingsUsername = firebaseSource.listNewFollowingsUsername
        val listNewUsernamesFollowingPosts = mutableListOf<Post>()

        if (listNewFollowingsUsername.isNotEmpty()) {
            for (newFollowingUsername in listNewFollowingsUsername) {

                var newFollowingListPosts : List<Post>

                newFollowingListPosts = getFollowingPostsByLessAndGreaterThan(
                    newFollowingUsername,
                    dateToRetrieveNewerPostsEpochSeconds,
                    dateGreaterThanEpochSeconds
                ).toMutableList()

                savedFeedHashMapPosts[newFollowingUsername] = newFollowingListPosts
                listNewUsernamesFollowingPosts.addAll(newFollowingListPosts)

                Log.i("CheckFollowing", "Here's all the posts $newFollowingListPosts")

                firebaseSource.listNewFollowingsUsername.remove(newFollowingUsername)
                addFollowingToSavedListFollowing(newFollowingUsername)
            }
        }
        return listNewUsernamesFollowingPosts
    }

    //Get Following object from firestore and add local list following
    private suspend fun addFollowingToSavedListFollowing(followingUsername: String) {
        val followingDocumentUID = getUserDocumentUID(followingUsername) ?: return

        val followingToSaveInSavedListFollowingDocRef =
            firebaseSource.db.collection(firebasePath.following_col).document(followingDocumentUID.followingDocumentUid)
                .collection(firebasePath.user_following).document(followingUsername)

        val followingToSaveInSavedListFollowing = followingToSaveInSavedListFollowingDocRef
            .get()
            .await()
            .toObject(Following::class.java)

        if (followingToSaveInSavedListFollowing != null) {
            savedListFollowing?.add(followingToSaveInSavedListFollowing)
        } else {
            savedFeedListPosts.removeAll { post -> post.userCreatorId == followingUsername }
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
            firebaseSource.listNewFollowingsUsername.remove(unfollowingUser)
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
                post.userCreatorId = firebaseSource.username
                listPostsOfNewUsername.add(post)
            }

            savedFeedHashMapPosts.remove(previousUsername)
            savedFeedListPosts.removeAll { post -> post.userCreatorId == previousUsername }

            savedFeedHashMapPosts[firebaseSource.username] = listPostsOfNewUsername
            savedFeedListPosts.addAll(listPostsOfNewUsername)

            savedFeedListPosts = savedFeedListPosts.sortedByDescending { post -> post.created_at }.toMutableList()
        }
    }

    fun getEndOfTimeline(): Boolean = endOfTimeline
}