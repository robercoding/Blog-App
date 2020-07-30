package com.rober.blogapp.data.network.firebase

import android.util.Log
import com.google.firebase.auth.FirebaseUser
import com.rober.blogapp.data.ResultAuth
import kotlinx.coroutines.tasks.await
import java.lang.Exception
import javax.inject.Inject

class FirebaseAuthManager @Inject constructor(private val firebaseSource: FirebaseSource) {
    private val TAG = "FirebaseAuthManager"

    suspend fun login(email: String, password: String): ResultAuth {
        var loggedIn = false
        try{
            firebaseSource.auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    loggedIn = true
                    Log.i(TAG, "$loggedIn")

                }
                .addOnFailureListener {
                    loggedIn = false
                }.await()
        }catch (exception: Exception){
            Log.i(TAG, "${exception}")
            return ResultAuth.Error(exception)
        }

        if(!loggedIn) return ResultAuth.Error(Exception("There was an error in the server"))

        return ResultAuth.Success
    }


    fun signUpWithEmail(email: String, password: String): ResultAuth{
        var createdAccount = false
        try{
            firebaseSource.auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    createdAccount = true
                }
                .addOnFailureListener {
                    createdAccount = false
                }
        }catch (exception: Exception){
            Log.i(TAG, "$exception")
            return ResultAuth.Error(exception)
        }

        return ResultAuth.Success
    }

    suspend fun signOut(): Boolean{
        firebaseSource.auth.signOut()

        val user = firebaseSource.userAuth

        return checkUser(user)
    }

    suspend fun checkIfUserAlreadyLoggedIn(): Boolean{
        val user = firebaseSource.userAuth
        return checkUser(user)
    }

    //    suspend fun getCurrentUser(): User{
//        var user: User? = null
//        val docRef = db.collection(usersColl).document(userAuth!!.uid)
//        docRef.get()
//            .addOnSuccessListener {document->
//                if(document!=null){
//                    val userObject = document.toObject(User::class.java)
//                    if(userObject!=null){
//                        user = userObject
//                    }
//                }
//            }
//        return user!!
//    }



    private fun checkUser(user: FirebaseUser?): Boolean{
        if(user != null){
            return true
        }
        return false
    }
}