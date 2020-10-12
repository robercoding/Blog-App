package com.rober.blogapp.data.network.firebase

import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.rober.blogapp.data.ResultAuth
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

    private suspend fun login(email: String, password: String): ResultAuth {
        var loggedIn = false
        try {
            firebaseSource.auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    firebaseSource.userAuth = it.user
                    loggedIn = true
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
            } else {
                exception = Exception(firebaseErrors.generalError)
            }
        }

        setCurrentUser()

        if ((loggedIn || firebaseSource.followingList == null || firebaseSource.followerList == null || firebaseSource.userDocumentUID == null)) {
            var tries = 0
            while (firebaseSource.username.isEmpty() && tries < 20) {
                kotlinx.coroutines.delay(200)
                tries += 1
            }
        }

        if (!loggedIn) {
            return ResultAuth.Error(exception)
        } else if (loggedIn && firebaseSource.username.isEmpty()) {
            exception = Exception("There was an issue with our servers")
            return ResultAuth.Error(exception)
        } else if (loggedIn && firebaseSource.username.isNotEmpty()) {
            return ResultAuth.Success
        } else {
            return ResultAuth.Error(Exception(exception))
        }
    }

    fun loginByEmail(email: String, password: String): Flow<ResultAuth> = flow {
        emit(ResultAuth.Loading)
        val resultAuth = login(email, password)
        emit(resultAuth)
    }

    suspend fun loginByUsername(username: String, password: String): Flow<ResultAuth> = flow {
        emit(ResultAuth.Loading)
        var email = ""

        firebaseSource.db.collection("usernames")
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { querySnapshot ->
                try {
                    when (querySnapshot.size()) {
                        0 -> throw Exception("We couldn't an account with that username")
                        1 -> email = querySnapshot.toObjects(Username::class.java)[0].email
                        else -> throw Exception("Sorry, there was an when trying to get the username")
                    }
                } catch (e: Exception) {
                    exception = e
                }
            }.await()


        if (email.isEmpty()) {
            emit(ResultAuth.Error(exception))
            return@flow
        }

        val resultAuth = login(email, password)
        emit(resultAuth)
    }

    suspend fun signUpWithEmail(email: String, password: String, username: String): Flow<ResultAuth> = flow {
        emit(ResultAuth.Loading)
        var createdAccount = false
        var savedInDatabase = false

        if (checkIfNameIsAlreadyPicked(username)) {
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

            val authResultUID =
                userAuthResult.user?.uid ?: throw Exception("Error signing up the new account")
            val authResultEmail =
                userAuthResult.user?.email ?: throw Exception("Error signing up the new account")

            savedInDatabase = saveNewUserInDatabase(authResultUID, authResultEmail, username)
        } catch (e: FirebaseAuthException) {
            exception = e
        }

        if (createdAccount && savedInDatabase)
            emit(ResultAuth.Success)
        else
            emit(ResultAuth.Error(exception))
    }

    suspend fun checkIfEmailAlreadyExists(email: String): Flow<ResultAuth> = flow {
        var isNewUser = false

        firebaseSource.auth.fetchSignInMethodsForEmail(email)
            .addOnCompleteListener { task ->
                task.result?.signInMethods?.run {
                    isNewUser = isEmpty()
                }
            }.await()


        if (isNewUser) {
            emit(ResultAuth.Success)
        } else {
            emit(ResultAuth.Error(Exception("Email already exists")))
        }
    }

    private suspend fun saveNewUserInDatabase(uid: String, email: String, username: String): Boolean {

        if (!saveUsername(uid, username, email)) return false

        if (!saveUser(uid, username)) return false

        if (!createUserDocumentsUID(username, uid)) return false

        return true
    }

    private suspend fun saveUsername(uid: String, name: String, email: String): Boolean {
        var success = false

        val userNameCollection = firebaseSource.db.collection("usernames").document(uid)
        val username = Username(uid, name, email)

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

    private suspend fun createUserDocumentsUID(username: String, uid: String): Boolean {
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
            UserDocumentUID(
                username,
                postDocumentUidDocRef.id,
                followingDocumentUidDocRef.id,
                followerDocumentUidDocRef.id,
                uid
            )
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