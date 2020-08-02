package com.rober.blogapp.data.network.firebase

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.rober.blogapp.entity.Username
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.lang.Exception

class FirebaseSource {
    private val TAG = "FirebaseSource"


    var db = FirebaseFirestore.getInstance()
    var auth = FirebaseAuth.getInstance()

    var userAuth = auth.currentUser
    var username  = ""

    suspend fun getCurrentUser(){
        var tempUsername = ""
        Log.i(TAG, "${userAuth.toString()}")
        if(userAuth!=null){
            try{
                db.collection("usernames").document(userAuth!!.uid)
                    .get()
                    .addOnSuccessListener {
                        if(it != null)
                            tempUsername = it.toObject(Username::class.java)!!.username
                    }.addOnFailureListener { username = "noname" }
                    .await()

            }catch (e: Exception){ Log.i(TAG, e.message.toString()) }

            username = tempUsername
        }
    }
}