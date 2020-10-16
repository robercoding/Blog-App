package com.rober.blogapp.data.network.firebase

import android.util.Log
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.functions.FirebaseFunctionsException
import com.rober.blogapp.data.ResultAuth
import com.rober.blogapp.data.network.util.FirebaseErrors
import com.rober.blogapp.data.network.util.FirebasePath
import com.rober.blogapp.data.network.util.FirebaseUtils
import com.rober.blogapp.entity.Disable
import com.rober.blogapp.entity.User
import com.rober.blogapp.entity.Username
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import org.threeten.bp.Instant
import javax.inject.Inject
import kotlin.Exception

class FirebaseAuthManager @Inject constructor(
    private val firebaseSource: FirebaseSource,
    private val firebasePath: FirebasePath,
    private val firebaseErrors: FirebaseErrors,
    private val firebaseUtils: FirebaseUtils
) {
    private val TAG = "FirebaseAuthManager"
    private var exception = Exception("There was an error in our servers")
    private var uidToEnableAccount = ""

    suspend fun setCurrentUser() {
        firebaseSource.setCurrentUser()
        firebaseSource.setCurrentFollowing()
        firebaseSource.setCurrentFollower()
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
                        0 -> throw Exception("We couldn't find an account with that username")
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

            val errorCode = when (e.errorCode) {
                firebaseErrors.ERROR_USER_DISABLED -> {
                    Log.i("SeeError", "We going disabled.")
                    checkDisabledError(email)
                }
                firebaseErrors.ERROR_WRONG_PASSWORD -> firebaseErrors.ERROR_WRONG_PASSWORD_CODE
                firebaseErrors.ERROR_NOT_FOUND -> firebaseErrors.ERROR_NOT_FOUND_CODE
                else -> firebaseErrors.GENERAL_ERROR
            }

            Log.i("SeeError", "we are going to return ${errorCode}")
            return when (errorCode) {
                firebaseErrors.ACCOUNT_DISABLED_LESS_30_DAYS -> {
                    Log.i("SeeError", "We going disabled. 30 less")
                    ResultAuth.Error(Exception(firebaseUtils.ACCOUNT_DISABLED_LESS_30_DAYS_MESSAGE))
                }
                firebaseErrors.ACCOUNT_DISABLED_MORE_30_DAYS -> {
                    Log.i("SeeError", "We going disabled. 30 more")
                    deleteAccount(uidToEnableAccount)
                    ResultAuth.Error(Exception(firebaseUtils.ACCOUNT_DISABLED_MORE_30_DAYS_MESSAGE))
                }
                firebaseErrors.ERROR_WRONG_PASSWORD_CODE -> ResultAuth.Error(Exception(firebaseUtils.ERROR_WRONG_PASSWORD))
                firebaseErrors.ERROR_NOT_FOUND_CODE -> ResultAuth.Error(Exception(firebaseUtils.ERROR_NOT_FOUND_MESSAGE))
                firebaseErrors.ACCOUNT_NOT_DISABLED -> ResultAuth.Error(Exception(firebaseUtils.GENERAL_MESSAGE_ERROR))
                firebaseErrors.GENERAL_ERROR -> ResultAuth.Error(Exception(firebaseUtils.GENERAL_MESSAGE_ERROR))
                else -> ResultAuth.Error(Exception(firebaseUtils.GENERAL_MESSAGE_ERROR))
            }
        }

        Log.i("SeeUID", "ALL GOOD UID= ${firebaseSource.auth.uid}")
        setCurrentUser()

        if ((loggedIn || firebaseSource.followingList == null || firebaseSource.followerList == null)) {
            var tries = 0
            while (firebaseSource.username.isEmpty() && tries < 20) {
                kotlinx.coroutines.delay(200)
                tries += 1
            }
        }

        return if (!loggedIn) {
            ResultAuth.Error(exception)
        } else if (loggedIn && firebaseSource.username.isEmpty()) {
            exception = Exception("There was an issue with our servers")
            ResultAuth.Error(exception)
        } else if (loggedIn && firebaseSource.username.isNotEmpty()) {
            ResultAuth.Success
        } else {
            ResultAuth.Error(Exception(exception))
        }
    }

    private suspend fun checkDisabledError(email: String): Int {

        uidToEnableAccount = getUidByEmail(email)

        var errorCode = 0
        val uidHashMap = hashMapOf("uid" to uidToEnableAccount)
        firebaseSource.functions.getHttpsCallable("checkIfAccountIsDisabled")
            .call(uidHashMap)
            .continueWith { task ->
                try {
                    if (!task.isSuccessful) {
                        errorCode = firebaseErrors.GENERAL_ERROR
                        return@continueWith
                    }
                    val result = task.result ?: throw Exception("There was an error getting the result")

                    //Cloud function send an object and we receive it as a map
                    val data = result.data as Map<String, *>
                    val isAccountDisabled = data["disabled"] as Boolean

                    errorCode = if (isAccountDisabled) {
                        data["errorCode"] as Int
                    } else {
                        firebaseErrors.ACCOUNT_NOT_DISABLED
                    }

                } catch (e: Exception) {

                }
            }.await()

        return errorCode

//        val isAccountDisabled =
//            firebaseSource.db.collection(firebasePath.disabled_col).document(uid).get().await()
//
//        if (!isAccountDisabled.exists()) {
//            return firebaseErrors.ACCOUNT_NOT_DISABLED
//        }
//
//        val disable = isAccountDisabled.toObject(Disable::class.java) ?: return firebaseErrors.GENERAL_ERROR
//
//        //Compare days
//        val now = Instant.now().toEpochMilli()
//        val dateDisable = disable.dateDisabledMilliseconds
//        if (!disable.disabled) return firebaseErrors.ACCOUNT_NOT_DISABLED
//
//        val secondsDifference = (now - dateDisable) / 1000
//        val daysDifference = (secondsDifference / 86400)
//
//        return if (daysDifference <= 30) {
//            uidToEnableAccount = uid
//            firebaseErrors.ACCOUNT_DISABLED_LESS_30_DAYS
//        } else {
//            Log.i("SeeDisable", "MORE 30 days")
//            firebaseErrors.ACCOUNT_DISABLED_MORE_30_DAYS
//        }
    }

    private suspend fun getUidByEmail(email: String): String {
        var uid = ""
        val emailHashMap = hashMapOf("email" to email)
        firebaseSource.functions.getHttpsCallable("getUidByEmail")
            .call(emailHashMap)
            .continueWith { task ->
                try {
                    if (!task.isSuccessful) {
                        return@continueWith
                    }
                    val result = task.result ?: throw Exception("We couldn't get the value")

                    val data = result.data as Map<*, *>
                    val status = data["status"] as Boolean
                    if (status) {
                        uid = data["uid"] as String
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error: ${e.message}")
                }
            }.await()

        return uid
//        val snapshotUid =
//            firebaseSource.db.collection("usernames").whereEqualTo("email", identifier).get().await()
//
//        if (snapshotUid.isEmpty)
//            return ""
//
//        val username = snapshotUid.toObjects(Username::class.java)[0]
//        return username.uid
    }

    fun enableAccount(): Flow<ResultAuth> = flow {
        emit(ResultAuth.Loading)
        if (uidToEnableAccount.isEmpty()) {
            emit(ResultAuth.Error(Exception(firebaseUtils.ERROR_ENABLING_ACCOUNT_MESSAGE)))
            return@flow
        }


        var enabled = false
        val hashMap = hashMapOf("uid" to uidToEnableAccount)
        firebaseSource.functions.getHttpsCallable("enableAccount")
            .call(hashMap)
            .continueWith { task ->
                try {
                    if (task.isSuccessful) {
                        val result = task.result ?: throw Exception("We didn't get a confirmation")
                        enabled = result.data as Boolean
                    }
                } catch (e: Exception) {
                }
            }.await()

        if (enabled)
            emit(ResultAuth.Success)
        else {
            emit(ResultAuth.Error(Exception(firebaseUtils.ERROR_ENABLING_ACCOUNT_MESSAGE)))
        }
    }

    private fun deleteAccount(uid: String) {
        val hashMap = hashMapOf(
            "uid" to uid
        )
        firebaseSource.functions.getHttpsCallable("deleteAccount")
            .call(hashMap)
    }

    suspend fun signUpWithEmailCloud(
        email: String,
        password: String,
        username: String
    ): Flow<ResultAuth> = flow {

        val mapSignUp = hashMapOf(
            "username" to username,
            "email" to email,
            "password" to password
        )


        var exception = Exception()
        var resultSignUp = false
        firebaseSource.functions
            .getHttpsCallable("signUpWithEmail")
            .call(mapSignUp)
            .continueWith { task ->
                try {
                    if (task.isSuccessful) {
                        val result = task.result ?: throw Exception("There isn't a result")

                        resultSignUp = result.data as Boolean

                    } else {
                        val e = task.exception
                        if (e is FirebaseFunctionsException) throw Exception(e.message)
                    }
                } catch (e: Exception) {
                    exception = e
                    return@continueWith
                }
            }.await()


        if (resultSignUp) {
            emit(ResultAuth.Success)
        } else {
            emit(ResultAuth.Error(exception))
        }
    }

    suspend fun signUpWithEmail(email: String, password: String, username: String): Flow<ResultAuth> =
        flow {
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

    private suspend fun checkIfNameIsAlreadyPicked(name: String): Boolean {
        val usersCollection = firebaseSource.db.collection("users")

        val users = usersCollection.get().await().toObjects(User::class.java)

        users.find { user -> user.username.equals(name) } ?: return false

        return true
    }

    suspend fun signOut(): Flow<ResultAuth> = flow {
        firebaseSource.auth.signOut()
        if(firebaseSource.auth.currentUser == null)
            emit(ResultAuth.Success)
        else
            throw Exception("There was an eror when trying to sign out.")
    }

    private fun checkUser(user: FirebaseUser?): Boolean {
        if (user != null) {
            Log.i("SeeUID", "True")
            return true
        }
        Log.i("SeeUID", "False")
        return false
    }
}
