package com.rober.blogapp.data.network.firebase

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.rober.blogapp.entity.User
import com.rober.blogapp.entity.Username
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.lang.Exception

class FirebaseSource {
    private val TAG = "FirebaseSource"


    var db = FirebaseFirestore.getInstance()
    var auth = FirebaseAuth.getInstance()

    var userAuth: FirebaseUser? = null
    var user: User? = null
    var username  = ""

    suspend fun setCurrentUser(){
        Log.i("User:", "First time user -> $user and $username")
        var tempUsername = User()
        Log.i(TAG, "${userAuth.toString()}")
        if(userAuth!=null){
            try{
                Log.i("User:","FirebaseSource: uid ${userAuth!!.uid}" )
                db.collection("usernames").document(userAuth!!.uid)
                    .get()
                    .addOnSuccessListener {
                        if(it != null)
                            tempUsername = it.toObject(User::class.java)!!
                    }.addOnFailureListener { username = "noname" }
                    .await()

            }catch (e: Exception){ Log.i(TAG, e.message.toString()) }

            user = tempUsername
            if(!tempUsername.isEmpty())
                username = tempUsername.username

            Log.i(TAG, "User: $user")
        }
    }

    fun getCurrentUser(){
        userAuth = auth.currentUser
    }

    fun checkUser(): Boolean{
        if(user?.isEmpty()!!){
            return false
        }
        Log.i("User:", "FirebaseSource check: $user")
        return true
    }
}