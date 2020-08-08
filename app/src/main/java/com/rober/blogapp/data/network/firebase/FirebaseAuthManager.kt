package com.rober.blogapp.data.network.firebase

import android.util.Log
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.QuerySnapshot
import com.rober.blogapp.data.ResultAuth
import com.rober.blogapp.data.ResultData
import com.rober.blogapp.data.network.util.FirebaseErrors
import com.rober.blogapp.entity.User
import com.rober.blogapp.entity.Username
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.Exception

class FirebaseAuthManager @Inject constructor(
    private val firebaseSource: FirebaseSource,
    private val firebaseErrors: FirebaseErrors
) {
    private val TAG = "FirebaseAuthManager"
    private var exception = Exception("There was an error in our servers")
    private val authErrors = firebaseErrors.authErrors

    suspend fun setCurrentUser(){
        Log.i("User", "AuthManager = Setting ")
        firebaseSource.setCurrentUser()
    }

    suspend fun getCurrentUser(): Flow<ResultData<User>> = flow {
        val user =  firebaseSource.getCurrentUser()
        Log.i("User", "AuthManager = Getting currentUser")
        Log.i("User", "AuthManager = Checking currentUser")
        if(user!=null){
            emit(if(user.isEmpty()) ResultData.Error(Exception("Sorry, user is empty"), null) else ResultData.Success(user))
            Log.i("User", "AuthManager = NOT NULL")
        }
        else{
            Log.i("User", "AuthManager = NULL")
            firebaseSource.setCurrentUser()
        }
    }

    suspend fun login(email: String, password: String): Flow<ResultAuth> = flow {
        emit(ResultAuth.Loading)
        var loggedIn = false
        try{
            firebaseSource.auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    firebaseSource.userAuth = it.user
                    loggedIn = true
                    Log.i(TAG, "$loggedIn")
                }
                .addOnFailureListener {
                    exception = Exception(it.message)
                    loggedIn = false
                }
                .await()
        }catch (e: FirebaseAuthException){
            val error = authErrors.find { error -> error[0].contains(e.errorCode)}

            if(error!=null){
                exception = Exception(error[1])
                Log.i(TAG, "Error found: $error")
            } else{
                exception = Exception(firebaseErrors.generalError)
                Log.i(TAG, "Error NOT found: $e")
                Log.i(TAG, "Error NOT found: ${e.errorCode}")
            }
        }

        firebaseSource.setCurrentUser()

        if(loggedIn){
            while (firebaseSource.username.equals("") ){
                kotlinx.coroutines.delay(500)
            }
        }

        if(!loggedIn)
            emit(ResultAuth.Error(exception))
        else
            emit(ResultAuth.Success)
    }

    suspend fun signUpWithEmail(email: String, password: String, name: String): Flow<ResultAuth> = flow {
        emit(ResultAuth.Loading)
        var createdAccount = false
        var savedInDatabase = false

        if(checkIfNameIsAlreadyPicked(name)){
            Log.i(TAG, "Name has been found")
            emit(ResultAuth.Error(Exception("Name is already in use, try other name")))
            return@flow
        }

        try{
            val userAuthResult = firebaseSource.auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    createdAccount = true
                }
                .addOnFailureListener {
                    createdAccount = false
                }.await()

            savedInDatabase = saveNewUserInDatabase(userAuthResult, name)
        }catch (e: FirebaseAuthException){
            exception = e
        }

        if(createdAccount && savedInDatabase)
            emit(ResultAuth.Success)
        else
            emit(ResultAuth.Error(exception))
    }

    private suspend fun saveNewUserInDatabase(userAuthResult: AuthResult, name: String): Boolean{
        val uid = userAuthResult.user!!.uid

        if(!saveUsername(uid, name)) return false

        if(!saveUser(uid, name)) return false

        return true
    }

    private suspend fun saveUsername(uid:String, name: String): Boolean{
        var success = false

        val userNameCollection = firebaseSource.db.collection("usernames").document(uid)
        val username = Username(uid, name)

        userNameCollection.set(username)
            .addOnSuccessListener { success = true }
            .addOnFailureListener { success = false }
            .await()

        return success
    }

    private suspend fun saveUser(uid: String, name: String): Boolean{
        var success = false

        val userToSave = User(0, uid, name, "", "")

        val usersCollection = firebaseSource.db.collection("users").document(name)

        usersCollection.set(userToSave)
            .addOnSuccessListener {
                success = true
            }
            .addOnFailureListener {
                exception = Exception(it.message)
                success = false
            }
            .await()

        return success
    }

    private suspend fun checkIfNameIsAlreadyPicked(name: String): Boolean{
        val usersCollection = firebaseSource.db.collection("users")

        val users = usersCollection.get().await().toObjects(User::class.java)

        users.find { user -> user.username.equals(name) } ?: return false

        return true
    }

    suspend fun signOut(): Flow<ResultAuth> = flow {
        firebaseSource.auth.signOut()

        val user = firebaseSource.userAuth

        firebaseSource.userAuth = null

       emit(if(checkUser(user)) ResultAuth.SuccessSignout else ResultAuth.FailureSignout)
    }

    private fun checkUser(user: FirebaseUser?): Boolean{
        if(user != null){
            return true
        }
        return false
    }

    fun checkUserLoggedIn(): Boolean{
        return firebaseSource.checkUser()
    }

//    private fun checkIfUserAlreadyLoggedIn(): Boolean{
//        val user = firebaseSource.userAuth
//        return checkUser(user)
//    }

    //Modify method to return ResultAuth
//    suspend fun getCurrentUser(): User {
//        var user: User? = null
//        val docRef = firebaseSource.db.collection("users").document(firebaseSource.username!!)
//        user = docRef
//            .get()
//            .await()
//            .toObject(User::class.java)
//
//        return user!!
//    }
}