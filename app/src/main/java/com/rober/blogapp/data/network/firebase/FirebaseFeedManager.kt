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

class FirebaseFeedManager
@Inject
constructor
    (
    private val firebaseSource: FirebaseSource
) {
    private val TAG = "FirebaseFeedManager"

//    private var dateGreaterThan = DateTime.now().minus(4).toDate()
//    private var dateLessThan = DateTime.now().toDate()
    //var instant = Instant()
    //private var format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    //private var date = format.parse()

    private var dateInit: Date? = null

    private var savedFeedListPosts: MutableList<Post> = mutableListOf()
    private var restDays = 0
    private var currentIntervalHoursIndex = 0
    private var listIntervalHours = listOf(0, 4, 8, 12, 16, 20, 24)
    private var dateGreaterThan: Date? = null
    private var dateLessThan: Date? = null

    private var savedListFollowing: MutableList<Following>? = null

    suspend fun getInitFeedPosts(): Flow<ResultData<List<Post>>> = flow {
        emit(ResultData.Loading)

        if (!savedListFollowing.isNullOrEmpty()) {
            Log.i(TAG, "We sending database ")
            emit(ResultData.Success(savedFeedListPosts))
        } else {
            dateInit = DateTime.now().toDate()

            var dateLessThanInit = DateTime.now().minusDays(restDays).toDate()
            var dateGreaterThanInit = DateTime.now().minusDays(restDays + 1).toDate()

            val listFollowing: MutableList<Following>

            try {
                //Get all user followings
                if (savedListFollowing.isNullOrEmpty()) {
                    listFollowing = getUserFollowings().toMutableList()
                    savedListFollowing = listFollowing
                } else {
                    listFollowing = savedListFollowing!!
                }

                val newListUsersPosts: MutableList<Post> = mutableListOf()

                while (newListUsersPosts.size < 10) {
                    for (following in savedListFollowing!!) {
                        val listFollowingPosts = getFollowingPostsByDateInit(
                            following.following_id,
                            dateLessThanInit,
                            dateGreaterThanInit
                        )
                        for (followingPost in listFollowingPosts)
                            newListUsersPosts.add(followingPost)
                    }

                    //Get User Logged In posts
                    val listUserLoggedInPosts = getFollowingPostsByDateInit(
                        firebaseSource.username,
                        dateLessThanInit,
                        dateGreaterThanInit
                    )
                    for (userLoggedInPost in listUserLoggedInPosts)
                        newListUsersPosts.add(userLoggedInPost)

                    Log.i("InitPosts", "So there's now a size of ${newListUsersPosts.size}")
                    restDays += 1
                    if (newListUsersPosts.size < 10) {
                        dateLessThanInit = DateTime.now().minusDays(restDays).toDate()
                        dateGreaterThanInit = DateTime.now().minusDays(restDays + 1).toDate()
                    }
                }

                val feedPostsOrdered =
                    newListUsersPosts.sortedByDescending { post -> post.created_at.time }
                        .toMutableList()

                savedFeedListPosts = feedPostsOrdered

                emit(ResultData.Success(savedFeedListPosts))

            } catch (exception: Exception) {
                emit(ResultData.Error<List<Post>>(exception, null))
            }
        }
    }

    private suspend fun getFollowingPostsByDateInit(
        followingId: String,
        dateLessThanInit: Date,
        dateGreaterThanInit: Date
    ): List<Post> {
        return firebaseSource.db.collection("posts/${followingId}/user_posts")
            .whereGreaterThan("created_at", dateGreaterThanInit)
            .whereLessThan("created_at", dateLessThanInit)
            .get()
            .await()
            .toObjects(Post::class.java)
    }

    suspend fun getOldFeedPosts(): Flow<ResultData<List<Post>>> = flow {
        emit(ResultData.Loading)
        Log.i("TryOldPosts", "Try")

        var triesRetrieveOldFeedPost = 0

        if (dateGreaterThan == null || dateLessThan == null) {
            dateLessThan = DateTime.now().minusDays(restDays)
                .plusHours(listIntervalHours[currentIntervalHoursIndex]).toDate()
            dateGreaterThan = DateTime.now().minusDays(restDays)
                .plusHours(listIntervalHours[currentIntervalHoursIndex + 1]).toDate()
        }

        //Get Followings
        if (savedListFollowing.isNullOrEmpty()) {
            val listFollowing = getUserFollowings()
            savedListFollowing = listFollowing.toMutableList()
        }

        val newListUsersPosts: MutableList<Post> = mutableListOf()

        while (newListUsersPosts.size < 10 && triesRetrieveOldFeedPost <= 7) {
            triesRetrieveOldFeedPost += 1
            Log.i("CheckOldPosts", "Try: ${triesRetrieveOldFeedPost}")

            Log.i("OldPosts", "Date Less than: ${dateLessThan}")
            Log.i("OldPosts", "Date Greater than: ${dateGreaterThan}")
            //Get following posts
            for (following in savedListFollowing!!) {
                val listFollowingPosts = getFollowingPostsByDate(following.following_id)
                newListUsersPosts.addAll(listFollowingPosts)
            }

            //Get User Logged In posts
            val listUserLoggedInPosts = getFollowingPostsByDate(firebaseSource.username)
            newListUsersPosts.addAll(listUserLoggedInPosts)

            if (currentIntervalHoursIndex != 5) {
                currentIntervalHoursIndex += 1
            } else {
                currentIntervalHoursIndex = 0
                restDays += 1
            }

            if (newListUsersPosts.size < 10 && triesRetrieveOldFeedPost <= 7) {

                dateLessThan = DateTime.now().minusDays(restDays)
                    .minusHours(listIntervalHours[currentIntervalHoursIndex]).toDate()

                dateGreaterThan = DateTime.now().minusDays(restDays)
                    .plusHours(listIntervalHours[currentIntervalHoursIndex + 1]).toDate()
            } else {
                Log.i("CheckOldPosts", "${newListUsersPosts.size}")
                val feedPostsOrdered =
                    newListUsersPosts.sortedByDescending { post -> post.created_at.time }
                        .toMutableList()

                for (postOrdered in feedPostsOrdered)
                    savedFeedListPosts.add(postOrdered)

                emit(ResultData.Success(savedFeedListPosts))
            }
        }
    }

    private suspend fun getFollowingPostsByDate(followingId: String): List<Post> {
        return firebaseSource.db.collection("posts/${followingId}/user_posts")
            .whereGreaterThan("created_at", dateGreaterThan!!)
            .whereLessThan("created_at", dateLessThan!!)
            .get()
            .await()
            .toObjects(Post::class.java)
    }

    private suspend fun getUserFollowings(): List<Following> {
        val listFollowing =
            firebaseSource.db.collection("following/${firebaseSource.username}/user_following")
                .get()
                .await()
                .toObjects(Following::class.java).toList()

        return listFollowing
    }
}