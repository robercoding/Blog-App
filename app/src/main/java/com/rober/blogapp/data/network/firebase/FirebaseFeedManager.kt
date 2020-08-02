package com.rober.blogapp.data.network.firebase

import android.util.Log
import com.google.firebase.Timestamp
import com.rober.blogapp.data.ResultData
import com.rober.blogapp.entity.Following
import com.rober.blogapp.entity.Post
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.lang.Exception
import javax.inject.Inject

class FirebaseFeedManager
@Inject
constructor
(
    private val firebaseSource: FirebaseSource
)
{
    private val TAG = "FirebaseFeedManager"

    suspend fun retrievePosts(): Flow<ResultData<List<Post>>> = flow {
        //Log.i(TAG, "Username: ${firebaseSource.getCurrentUser()}")

        val date = org.threeten.bp.LocalDateTime.now().minusDays(7)

        var listFollowings: List<Following>? = null
        //var listFollowings : List<Following>? = null
        //+firebaseSource.username+
        try{
            if(firebaseSource.username.equals(""))
                firebaseSource.getCurrentUser()

            Log.i(TAG, "DEBUG: following/${firebaseSource.username}/user_following")

            val followingReference = firebaseSource.db.collection("following/${firebaseSource.username}/user_following")

            listFollowings = followingReference
                .get()
                .await()
                .toObjects(Following::class.java).toList()

            Log.i(TAG, "Following size: ${listFollowings.size}, $listFollowings")

            val newListPost: MutableList<Post> = mutableListOf()
            //+following.following_id
            for(following in listFollowings){
                val listUserPosts = firebaseSource.db.collection("posts/${following.following_id}/user_posts").get().await().toObjects(Post::class.java).toList()
                Log.i(TAG, "Following size: ${listUserPosts.size}, $listUserPosts")
                for(post in listUserPosts){
                    newListPost.add(post)
                }
            }
            emit(ResultData.Success(newListPost))
//
//            val postsReference = firebaseSource.db.collection("posts")
//
//            Log.i(TAG, "ListFollowing: ${listFollowings.size}")
//            listPosts = postsReference
//                .whereGreaterThan("created_at", date)
//                .get()
//                .await()
//                .toObjects(Post::class.java)
//
//            Log.i(TAG, "ListPosts: ${listPosts.size}, $listPosts")


        }catch (exception: Exception){
            emit(ResultData.Error<List<Post>>(exception, null))
        }

//        val newListPost: MutableList<Post> = mutableListOf()
//
//        Log.i(TAG, "List Following = Size: ${listFollowings!!.size}, content: $listFollowings!!")
//        Log.i(TAG, "List Following = Size: ${listPosts!!.size}, content: $listPosts!!")
//
//
//        for(post in listPosts){
//            for(following in listFollowings){
//                if(post.user_creator_id == following.following_id){
//                    newListPost.add(post)
//                }
//            }
//        }
//
//        Log.i(TAG, "New List Post = Size: ${newListPost.size}, content: $newListPost")
//        emit(ResultData.Success(newListPost))
    }

}