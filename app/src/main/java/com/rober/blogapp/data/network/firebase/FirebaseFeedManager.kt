package com.rober.blogapp.data.network.firebase

import android.util.Log
import com.google.firebase.Timestamp
import com.rober.blogapp.data.ResultData
import com.rober.blogapp.entity.Following
import com.rober.blogapp.entity.Post
import com.rober.blogapp.entity.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import org.joda.time.DateTime
import org.joda.time.LocalDate
import org.threeten.bp.LocalDateTime
import java.lang.Exception
import java.util.*
import javax.inject.Inject

class FirebaseFeedManager
@Inject
constructor
(
    private val firebaseSource: FirebaseSource
)
{
    private val TAG = "FirebaseFeedManager"

    var savedFeedListPost: MutableList<Post> = mutableListOf()
    var feedPagination = 0


    suspend fun getFeedPosts(): Flow<ResultData<List<Post>>> = flow {
        emit(ResultData.Loading)
        try{
            if(savedFeedListPost.isNotEmpty()){
                emit(ResultData.Success(savedFeedListPost))
            }else{
                //Get all user followings
                val listFollowing = getUserFollowings()
                Log.i(TAG, "List Following: ${listFollowing}")

                //Get userlogged in posts reference

                val newListUserPosts: MutableList<Post> = mutableListOf()

                //val dateGreater = DateTime.now().minusDays(7).toDate()

                for (following in listFollowing) {
                    val listFollowingPosts = firebaseSource.db.collection("posts/${following.following_id}/user_posts")
                        .get()
                        .await()
                        .toObjects(Post::class.java)

                    Log.i(TAG, "List Following Posts: ${listFollowingPosts}")
                    for(followingPost in listFollowingPosts){
                        Log.i(TAG, "Adding following post: ${followingPost}")
                        newListUserPosts.add(followingPost)
                    }
                }

                val listUserLoggedInPosts = firebaseSource.db.collection("posts/${firebaseSource.username}/user_posts")
                    .get()
                    .await()
                    .toObjects(Post::class.java)

                for(userLogedInPost in listUserLoggedInPosts)
                    newListUserPosts.add(userLogedInPost)

                val feedPostsOrdered = newListUserPosts.sortedBy { post -> post.created_at.time }.toMutableList()

                savedFeedListPost = feedPostsOrdered

                emit(ResultData.Success(savedFeedListPost))

            }
        }catch (exception: Exception){
            emit(ResultData.Error<List<Post>>(exception, null))
        }
    }

    private suspend fun getUserFollowings(): List<Following>{
        val listFollowing =
            firebaseSource.db.collection("following/${firebaseSource.username}/user_following")
                .get()
                .await()
                .toObjects(Following::class.java).toList()

        return listFollowing
    }


    private suspend fun getUserPostsWhereDateGreater(following: Following? = null, dateGreater: Date): List<Post>{
        if(following != null){
            val listFollowingPosts =
                firebaseSource.db.collection("posts/${following.following_id}/user_posts")
                    .whereGreaterThan("created_at", dateGreater)
                    .get()
                    .await()
                    .toObjects(Post::class.java)

            return listFollowingPosts
        }

        val userLoggedInPosts = firebaseSource.db.collection("posts/${firebaseSource.username}/user_posts")
            .whereGreaterThan("created_at", dateGreater)
            .get()
            .await()
            .toObjects(Post::class.java)

        return userLoggedInPosts
    }

    private suspend fun getUserPostsWhereBetween(following: Following? = null, dateGreater: Date, dateLess: Date): List<Post>{

        if(following != null){
            val listFollowingPosts =
                firebaseSource.db.collection("posts/${following.following_id}/user_posts")
                    .whereGreaterThan("created_at", dateGreater)
                    .whereLessThan("created_at", dateLess)
                    .get()
                    .await()
                    .toObjects(Post::class.java)

            return listFollowingPosts
        }

        val userLoggedInPosts = firebaseSource.db.collection("posts/${firebaseSource.username}/user_posts")
            .whereGreaterThan("created_at", dateGreater)
            .whereLessThan("created_at", dateLess)
            .get()
            .await()
            .toObjects(Post::class.java)

        return userLoggedInPosts
    }

}