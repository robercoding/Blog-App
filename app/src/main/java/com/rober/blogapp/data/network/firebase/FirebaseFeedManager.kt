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


    suspend fun retrieveFeedPosts(morePosts: Boolean): Flow<ResultData<List<Post>>> = flow {
        emit(ResultData.Loading)
        try{
            if(!morePosts && feedPagination > 0){
                if(savedFeedListPost.size > 0)
                    emit(ResultData.Success(savedFeedListPost.toList()))
            }

            //Get all user followings
            val listFollowing = getUserFollowings()

            //Get userlogged in posts reference

            val newListPosts: MutableList<Post> = mutableListOf()

            //Init post and save
            if(!morePosts && feedPagination == 0) {
                feedPagination++
                val dateGreater = DateTime.now().minusDays(feedPagination * 7).toDate()


                for (following in listFollowing) {
                    val listFollowingPosts = getUserPostsWhereDateGreater(following, dateGreater)

                    for (post in listFollowingPosts) {
                        newListPosts.add(post)
                    }
                }
                val userLoggedInPosts = getUserPostsWhereDateGreater(null, dateGreater)

                for (post in userLoggedInPosts)
                    newListPosts.add(post)

                val postsOrdered = newListPosts.sortedBy { post -> post.created_at.time }.toMutableList()

                savedFeedListPost = postsOrdered

                emit(ResultData.Success(savedFeedListPost))
            }

            //Retrieve post from firestore

            if(morePosts){
                val dateLess = DateTime.now().minusDays(feedPagination*7).toDate()
                feedPagination++
                val dateGreater = DateTime.now().minusDays(feedPagination*7).toDate()

                //Get posts from following
                for (following in listFollowing) {
                    val listFollowingPosts = getUserPostsWhereBetween(following, dateGreater, dateLess)

                    for (post in listFollowingPosts) {
                        newListPosts.add(post)
                    }
                }

                //Get posts user logged in
                val userLoggedInPosts = getUserPostsWhereBetween(null, dateGreater, dateLess)

                for(post in userLoggedInPosts)
                    newListPosts.add(post)

                val postsOrdered = newListPosts.sortedBy { post -> post.created_at.time }.toMutableList()

                savedFeedListPost.addAll(postsOrdered)

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