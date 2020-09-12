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
import com.rober.blogapp.entity.UserDocumentUID
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

    suspend fun setCurrentUser() {
        firebaseSource.setCurrentUser()
        firebaseSource.setCurrentUserDocumentsUID()
        firebaseSource.setCurrentFollowing()
        firebaseSource.setCurrentFollower()
    }

    suspend fun getCurrentUser(): Flow<ResultData<User>> = flow {
        val user = firebaseSource.getCurrentUser()

        if (user.isEmpty()) {
            firebaseSource.setCurrentUser()
            emit(ResultData.Error(Exception("Sorry, user is empty"), null))
        } else {
            emit(ResultData.Success(user))
        }
    }

    suspend fun login(email: String, password: String): Flow<ResultAuth> = flow {
        emit(ResultAuth.Loading)
        var loggedIn = false
        try {
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
        } catch (e: FirebaseAuthException) {
            val error = authErrors.find { error -> error[0].contains(e.errorCode) }

            if (error != null) {
                exception = Exception(error[1])
                Log.i(TAG, "Error found: $error")
            } else {
                exception = Exception(firebaseErrors.generalError)
                Log.i(TAG, "Error NOT found: $e")
                Log.i(TAG, "Error NOT found: ${e.errorCode}")
            }
        }


        setCurrentUser()


        if ((loggedIn || firebaseSource.followingList == null || firebaseSource.followerList == null || firebaseSource.userDocumentUID == null)) {
            var tries = 0
            while (firebaseSource.username.isEmpty() && tries < 20) {
                kotlinx.coroutines.delay(200)
                tries += 1
                Log.i("User:", "$tries and username= ${firebaseSource.username}")
            }
        }


        if (!loggedIn) {
            emit(ResultAuth.Error(exception))
        } else if (loggedIn && firebaseSource.username.isEmpty()) {
            exception = Exception("There was an issue with our servers")
            emit(ResultAuth.Error(exception))
        }else if(loggedIn && firebaseSource.username.isNotEmpty()){
            emit(ResultAuth.Success)
        }


    }

    suspend fun signUpWithEmail(email: String, password: String, name: String): Flow<ResultAuth> = flow {
        emit(ResultAuth.Loading)
        var createdAccount = false
        var savedInDatabase = false

        if (checkIfNameIsAlreadyPicked(name)) {
            Log.i(TAG, "Name has been found")
            emit(ResultAuth.Error(Exception("Name is already in use, try other name")))
            return@flow
        }

        try {
            val userAuthResult = firebaseSource.auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    createdAccount = true
                }
                .addOnFailureListener {
                    createdAccount = false
                }.await()

            savedInDatabase = saveNewUserInDatabase(userAuthResult, name)
        } catch (e: FirebaseAuthException) {
            exception = e
        }

        if (createdAccount && savedInDatabase)
            emit(ResultAuth.Success)
        else
            emit(ResultAuth.Error(exception))
    }

    private suspend fun saveNewUserInDatabase(userAuthResult: AuthResult, username: String): Boolean {
        val uid = userAuthResult.user!!.uid

        if (!saveUsername(uid, username)) return false

        if (!saveUser(uid, username)) return false

        if (!createUserDocumentsUID(username)) return false

        return true
    }

    private suspend fun saveUsername(uid: String, name: String): Boolean {
        var success = false

        val userNameCollection = firebaseSource.db.collection("usernames").document(uid)
        val username = Username(uid, name)

        userNameCollection.set(username)
            .addOnSuccessListener { success = true }
            .addOnFailureListener { success = false }
            .await()

        return success
    }

    private suspend fun saveUser(uid: String, name: String): Boolean {
        var success = false

        val userToSave = User(0, uid, name, "", "", 0, 0, "", "", 0)

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

    private suspend fun createUserDocumentsUID(username: String): Boolean {
        val usernameHashMap = hashMapOf("username" to username)
        //Create document for the new user
        val postDocumentUidDocRef = firebaseSource.db.collection("posts").document()
        val followingDocumentUidDocRef = firebaseSource.db.collection("following").document()
        val followerDocumentUidDocRef = firebaseSource.db.collection("follower").document()
        //Set the username who pertains the documents
        postDocumentUidDocRef.set(usernameHashMap).await()
        followingDocumentUidDocRef.set(usernameHashMap).await()
        followerDocumentUidDocRef.set(usernameHashMap).await()

        //Create object with the ID generated and store them
        val userDocumentUID =
            UserDocumentUID(username, postDocumentUidDocRef.id, followingDocumentUidDocRef.id, followerDocumentUidDocRef.id)
        val userDocumentsUidDocRef = firebaseSource.db.collection("user_documents_uid").document()

        var success = false
        userDocumentsUidDocRef
            .set(userDocumentUID)
            .addOnSuccessListener {
                success = true
            }
            .addOnFailureListener {
                success = false
            }.await()

        return success
    }


    private suspend fun checkIfNameIsAlreadyPicked(name: String): Boolean {
        val usersCollection = firebaseSource.db.collection("users")

        val users = usersCollection.get().await().toObjects(User::class.java)

        users.find { user -> user.username.equals(name) } ?: return false

        return true
    }

    suspend fun signOut(): Flow<ResultAuth> = flow {
        firebaseSource.auth.signOut()

        val user = firebaseSource.userAuth

        firebaseSource.userAuth = null

        emit(if (checkUser(user)) ResultAuth.SuccessSignout else ResultAuth.FailureSignout)
    }

    private fun checkUser(user: FirebaseUser?): Boolean {
        if (user != null) {
            return true
        }
        return false
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