package com.rober.blogapp.data.network.firebase

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.rober.blogapp.entity.Follower
import com.rober.blogapp.entity.Following
import com.rober.blogapp.entity.User
import com.rober.blogapp.entity.Username
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.lang.Exception

class FirebaseSource {
    private val TAG = "FirebaseSource"

    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()

    var userAuth: FirebaseUser? = null
    var user: User? = null
    var username  = ""
    var followingList: List<Following>? = null
    var followerList: List<Follower>? = null


    suspend fun setCurrentUser(){
        Log.i("User:", "First time user -> $user and $username")
        var tempUsername = Username()
        var tempUser = User()
        Log.i(TAG, "${userAuth.toString()}")
        if(userAuth!=null){
            try{
                Log.i("User:","FirebaseSource: uid ${userAuth!!.uid}" )
                db.collection("usernames").document(userAuth!!.uid)
                    .get()
                    .addOnSuccessListener {
                        if(it != null)
                            tempUsername = it.toObject(Username::class.java)!!
                    }.addOnFailureListener { username = "noname" }
                    .await()

                if(!tempUsername.isEmpty()) {
                    tempUser = db.collection("users").document(tempUsername.username).get().await().toObject(User::class.java)!!
                }

            }catch (e: Exception){ Log.i(TAG, e.message.toString()) }

            if(!tempUser.isEmpty())
                user = tempUser

            if(!tempUsername.isEmpty())
                username = tempUsername.username

            Log.i(TAG, "User: $user")
        }
    }

    suspend fun setCurrentFollowing(){
        try{
            val followingRef = db.collection("following").document(username)
                .collection("user_following")

            followingList = followingRef
                .get()
                .await()
                .toObjects(Following::class.java)

        }catch (e:Exception){
            Log.i(TAG, "$e")
        }
    }

    suspend fun setCurrentFollower(){
        try{
            val followerRef = db.collection("follower").document(username)
                .collection("user_follower")

            followerList = followerRef
                .get()
                .await()
                .toObjects(Follower::class.java)

        }catch (e:Exception){
            Log.i(TAG, "$e")
        }
    }

    fun getCurrentUser(): User{
        if(user != null)
            return user!!
        else
            return User()
    }

    fun checkUser(): Boolean{
        if(user?.isEmpty()!!){
            return false
        }
        Log.i("User:", "FirebaseSource check: $user")
        return true
    }
}