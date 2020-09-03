package com.rober.blogapp.data.network.firebase

import android.util.Log
import com.rober.blogapp.data.ResultData
import com.rober.blogapp.entity.Following
import com.rober.blogapp.entity.Post
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import org.joda.time.DateTime
import java.lang.Exception
import java.util.*
import javax.inject.Inject
import kotlin.collections.HashMap

class FirebaseFeedManager
@Inject
constructor
    (
    private val firebaseSource: FirebaseSource
) {
    private val TAG = "FirebaseFeedManager"

    private var endOfTimeline = false

    private var dateToRetrieveNewerPosts: Date? = null

    private var savedFeedListPosts: MutableList<Post> = mutableListOf()
    private var savedFeedHashMapPosts: HashMap<String, MutableList<Post>> = hashMapOf()
    private var restDays = 0
    private var currentIntervalHoursIndex = 0

    //    private var listIntervalFourHours = listOf(0, 4, 8, 12, 16, 20, 24) //for people who has more followings
    private var listIntervalEightHours = listOf(0, 8, 16, 24) //For people with less following
    private var dateGreaterThan: Date? = null
    private var dateLessThan: Date? = null

    private var savedListFollowing: MutableList<Following>? = null

    suspend fun getInitFeedPosts(): Flow<ResultData<List<Post>>> = flow {
        emit(ResultData.Loading)

        if (hasUserChangedHisUsername()) {
            Log.i("ChangedUsername", "Yes Changed")
            changeHashMapPostsUsername()
        }else{
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
            Log.i(TAG, "We sending database ")
            emit(ResultData.Success(savedFeedListPosts))
        } else { //Get posts
            dateToRetrieveNewerPosts = DateTime.now().toDate()

            var dateLessThanInit = DateTime.now().minusDays(restDays).toDate()
            dateGreaterThan = DateTime.now().minusDays(restDays + 1).toDate()

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
                while (countTotalPosts < 10 && getPostsTries < 10) {
                    getPostsTries += 1

                    if (!newListFollowing.isNullOrEmpty()) {
                        for (following in newListFollowing) {
                            var listUserPostsHashMap = mutableListOf<Post>()
                            if(savedFeedHashMapPosts.containsKey(following.following_id))
                                listUserPostsHashMap = savedFeedHashMapPosts.getValue(following.following_id)

                            val listFollowingNewPosts = getFollowingPostsByLessAndGreaterThan(
                                following.following_id,
                                dateLessThanInit,
                                dateGreaterThan
                            )
                            if(listFollowingNewPosts.isNotEmpty()){
                                listUserPostsHashMap.addAll(listFollowingNewPosts)
                                countTotalPosts.plus(listFollowingNewPosts.size)
                                savedFeedHashMapPosts[following.following_id] = listUserPostsHashMap
                            }
                        }
                    }

                    //Get User Logged In posts
                    val listUserLoggedInNewPosts = getFollowingPostsByLessAndGreaterThan(
                        firebaseSource.username,
                        dateLessThanInit,
                        dateGreaterThan
                    )
                    Log.i("HashMapPosts", "ListUserLoggedIn: ${listUserLoggedInNewPosts}")



                    var listUserLoggedInPostsFromHashMap = mutableListOf<Post>()
                    if(savedFeedHashMapPosts.containsKey(firebaseSource.username))
                        listUserLoggedInPostsFromHashMap = savedFeedHashMapPosts.getValue(firebaseSource.username)

                    if(listUserLoggedInNewPosts.isNotEmpty()){
                        listUserLoggedInPostsFromHashMap.addAll(listUserLoggedInNewPosts)
                        countTotalPosts.plus(listUserLoggedInNewPosts.size)
                        savedFeedHashMapPosts[firebaseSource.username] = listUserLoggedInPostsFromHashMap
                    }

                    restDays += 1
                    if (countTotalPosts < 10) {
                        dateLessThanInit = DateTime.now().minusDays(restDays).toDate()
                        dateGreaterThan = DateTime.now().minusDays(restDays + 1).toDate()
                    }
                }

                val allPosts = mutableListOf<Post>()
                savedFeedHashMapPosts.mapKeys {mapEntry->
                    allPosts.addAll(mapEntry.value)
                }

                Log.i("HashMapPosts", "Hash ${savedFeedHashMapPosts[firebaseSource.username]}")
                Log.i("HashMapPosts", "List ${allPosts}")


                val feedPostsOrdered =
                    allPosts.sortedByDescending { post -> post.created_at.time }
                        .toMutableList()

                savedFeedListPosts = feedPostsOrdered
                emit(ResultData.Success(savedFeedListPosts))
            } catch (exception: Exception) {
                emit(ResultData.Error<List<Post>>(exception, null))
            }
        }
    }

    private suspend fun getFollowingPostsByLessAndGreaterThan(
        followingId: String,
        dateLessThanInit: Date?,
        dateGreaterThanInit: Date?
    ): List<Post> {
        Log.i("Where", "FollowingID = ${followingId}")
        return if (dateLessThanInit != null && dateGreaterThanInit != null)
            firebaseSource.db.collection("posts/${followingId}/user_posts")
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
                    dateToRetrieveNewerPosts
                )

                //Get old local posts from following if exists
                var mapSavedPostsFromFollowing = mutableListOf<Post>()
                if(savedFeedHashMapPosts.containsKey(following.following_id))
                    mapSavedPostsFromFollowing = savedFeedHashMapPosts.getValue(following.following_id)

                //If there are new posts then add to local map and add to outter scope list
                if(tempListNewPostsFromFollowing.isNotEmpty()){
                    mapSavedPostsFromFollowing.addAll(tempListNewPostsFromFollowing)
                    savedFeedHashMapPosts[following.following_id] = mapSavedPostsFromFollowing

                    newListPosts.addAll(tempListNewPostsFromFollowing)
                }
            }

            //
            var mapSavedPostsFromUserLoggedIn = mutableListOf<Post>()
            savedFeedHashMapPosts[firebaseSource.username] = mapSavedPostsFromUserLoggedIn

            val listUserPosts =
                getListPostsFromIdByGreaterDate(firebaseSource.username, dateToRetrieveNewerPosts).toMutableList()

            if(listUserPosts.isNotEmpty()){
                mapSavedPostsFromUserLoggedIn.addAll(listUserPosts)
                savedFeedHashMapPosts[firebaseSource.username] = mapSavedPostsFromUserLoggedIn

                newListPosts.addAll(listUserPosts)
            }

            dateToRetrieveNewerPosts = DateTime.now().toDate()

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
        dateGreater: Date?
    ): List<Post> {
        return if (dateGreater != null)
            firebaseSource.db.collection("posts/${followingId}/user_posts")
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

        dateLessThan = DateTime.now().minusDays(restDays)
            .minusHours(listIntervalEightHours[currentIntervalHoursIndex]).toDate()
        dateGreaterThan = DateTime.now().minusDays(restDays)
            .minusHours(listIntervalEightHours[currentIntervalHoursIndex + 1]).toDate()

//        Log.i("CheckDate", "Date Less than: ${dateLessThan}")
//        Log.i("CheckDate", "Date Greater than: ${dateGreaterThan}")

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
                    dateLessThan,
                    dateGreaterThan
                )
                newListUsersPosts.addAll(listFollowingPosts)
            }

            //Get User Logged In posts
            val listUserLoggedInPosts = getFollowingPostsByLessAndGreaterThan(
                firebaseSource.username,
                dateLessThan,
                dateGreaterThan
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

                    dateLessThan = DateTime.now().minusDays(restDays - 1).toDate()
                    dateGreaterThan = DateTime.now().minusDays(restDays).toDate()
                    Log.i(
                        "TestDatesInTries",
                        "DAY BY DAY NOW: DateLess= ${dateLessThan.toString()} && DateGreater= ${dateGreaterThan.toString()}"
                    )
                } else {
                    dateLessThan = DateTime.now().minusDays(restDays)
                        .minusHours(listIntervalEightHours[currentIntervalHoursIndex]).toDate()

                    dateGreaterThan = DateTime.now().minusDays(restDays)
                        .minusHours(listIntervalEightHours[currentIntervalHoursIndex + 1]).toDate()
                    Log.i(
                        "TestDatesInTries",
                        "HOUR BY HOUR: DateLess= ${dateLessThan.toString()} && DateGreater= ${dateGreaterThan.toString()}"
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
                    newListUsersPosts.sortedByDescending { post -> post.created_at.time }
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
//            .whereGreaterThan("created_at", dateGreaterThan!!)
//            .whereLessThan("created_at", dateLessThan!!)
//            .get()
//            .await()
//            .toObjects(Post::class.java)
//    }

    private suspend fun getUserFollowings(): List<Following> {
        val listFollowing =
            firebaseSource.db.collection("following/${firebaseSource.username}/user_following")
                .get()
                .await()
                .toObjects(Following::class.java).toList()

        return listFollowing
    }

    private fun checkIfNewFollowings(): Boolean {
        return firebaseSource.listNewFollowingsUsername.size > 0
    }

    private suspend fun getPostsFromNewFollowings(): MutableList<Post>{
        val listPostsFromNewFollowings = getListPostsFromNewFollowings().toMutableList()

        return listPostsFromNewFollowings
    }

    //Retrieve all posts from the new followings, the newest one to the oldest one of the actual timeline
    private suspend fun getListPostsFromNewFollowings(): List<Post> {
        val listNewFollowingsUsername = firebaseSource.listNewFollowingsUsername
        val listNewUsernamesFollowingPosts = mutableListOf<Post>()

        if (listNewFollowingsUsername.isNotEmpty()) {
            for (newFollowingUsername in listNewFollowingsUsername) {

                var newFollowingListPosts = emptyList<Post>()

                newFollowingListPosts = getFollowingPostsByLessAndGreaterThan(
                    newFollowingUsername,
                    dateToRetrieveNewerPosts,
                    dateGreaterThan
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
        val followingToSaveInSavedListFollowingDocRef =
            firebaseSource.db.collection("following").document(firebaseSource.username)
                .collection("user_following").document(followingUsername)
        val followingToSaveInSavedListFollowing = followingToSaveInSavedListFollowingDocRef
            .get()
            .await()
            .toObject(Following::class.java)

        if (followingToSaveInSavedListFollowing != null) {
            savedListFollowing?.add(followingToSaveInSavedListFollowing)
        } else {
            savedFeedListPosts.removeAll { post -> post.user_creator_id == followingUsername }
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
            savedFeedListPosts.removeAll { post -> post.user_creator_id == unfollowingUser }
            firebaseSource.listNewFollowingsUsername.remove(unfollowingUser)
        }
    }

    private fun hasUserChangedHisUsername(): Boolean {
        return firebaseSource.userChangedUsername
    }

    //When username change it updates local saved posts
    private fun changeHashMapPostsUsername(){
        val previousUsername = firebaseSource.usernameBeforeChange
        val previousUsernameListPosts = savedFeedHashMapPosts[previousUsername]

        val listPostsOfNewUsername = mutableListOf<Post>()
        if(previousUsernameListPosts != null){
            for(post in previousUsernameListPosts){
                post.user_creator_id = firebaseSource.username
                listPostsOfNewUsername.add(post)
            }

            savedFeedHashMapPosts.remove(previousUsername)
            savedFeedListPosts.removeAll { post-> post.user_creator_id == previousUsername }

            savedFeedHashMapPosts[firebaseSource.username] = listPostsOfNewUsername
            savedFeedListPosts.addAll(listPostsOfNewUsername)

            savedFeedListPosts = savedFeedListPosts.sortedByDescending { post-> post.created_at }.toMutableList()
        }
    }

    fun getEndOfTimeline(): Boolean = endOfTimeline
}