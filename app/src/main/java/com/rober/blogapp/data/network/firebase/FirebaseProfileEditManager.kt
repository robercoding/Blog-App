package com.rober.blogapp.data.network.firebase

import android.util.Log
import com.google.firebase.firestore.Query
import com.rober.blogapp.data.ResultData
import com.rober.blogapp.entity.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.Exception

class FirebaseProfileEditManager @Inject constructor(
    private val firebaseSource: FirebaseSource
) {
    private val TAG = "FirebaseProfileEditMana"

    suspend fun updateUser(previousUser: User, newUser: User): Flow<ResultData<Boolean>> = flow {
        var successUpdateUser = false
        val differentUsernames = previousUser.username != newUser.username

        try {
            //Check if they are different names
            if (differentUsernames) {
                //Change the username field from the collection usernames
                //Find the document that contains username
                val usernameDocumentID = findDocumentIDByField("username", previousUser.username)

                //Update username
                var successUpdateUsername = false
                usernameDocumentID?.also { documentID ->
                    successUpdateUsername = updateDocumentByField(documentID, "username", newUser.username)
                } ?: kotlin.run {
                    successUpdateUsername = false
                    return@flow
                }

                //Set new user once username is changed
                val successSetNewUser: Boolean
                if (successUpdateUsername) {
                    successSetNewUser = setDocument(newUser.username, newUser)
                } else {
                    return@flow
                }

                //Delete the old one once new user has been set
                var successDelete = false
                if (successSetNewUser) {
                    successDelete = deleteDocument(previousUser.username)
                } else {
                    return@flow
                }

                //Check everything went good
                if (successUpdateUsername && successSetNewUser && successDelete)
                    successUpdateUser = true
            } else {
                val updateUserMap = mapOf(
                    "biography" to newUser.biography,
                    "location" to newUser.location
                )

                firebaseSource.db
                    .collection("users")
                    .document(newUser.username)
                    .update(updateUserMap)
                    .addOnSuccessListener {
                        successUpdateUser = true
                    }
                    .addOnFailureListener {
                        successUpdateUser = false
                    }.await()
            }

            if (differentUsernames && !successUpdateUser) {
                emit(ResultData.Error(Exception("Sorry we couldn't update the user and its username")))

            } else if (differentUsernames && successUpdateUser) {
                emit(ResultData.Success(successUpdateUser))

            } else if (!differentUsernames && successUpdateUser) {
                emit(ResultData.Success(successUpdateUser))

            } else if (!differentUsernames && !successUpdateUser) {
                emit(ResultData.Error(Exception("Sorry we couldn't update the user")))
            }

        } catch (e: Exception) {
            Log.i(TAG, "${e.message}")
        }
    }

    private suspend fun findDocumentIDByField(field: String, fieldValue: Any): String? {
        var usernameDocumentID: String? = null

        firebaseSource.db.collection("usernames")
            .whereEqualTo(field, fieldValue)
            .get()
            .addOnSuccessListener { documents ->
                when (documents.size()) {
                    1 -> {
                        for (document in documents) {
                            usernameDocumentID = document.id
                        }
                    }
                    else -> return@addOnSuccessListener
                }
            }.addOnFailureListener {
                return@addOnFailureListener
            }.await()

        return usernameDocumentID
    }

    private suspend fun updateDocumentByField(documentID: String, field: String, fieldValue: String): Boolean {
        var successUpdate = false
        val usernameDocumentReference = firebaseSource.db.collection("usernames").document(documentID)

        usernameDocumentReference
            .update(field, fieldValue)
            .addOnSuccessListener {
                successUpdate = true
            }.addOnFailureListener {
                successUpdate = false
            }.await()

        return successUpdate
    }

    private suspend fun setDocument(documentUsernameID: String, newUser: User): Boolean {
        var successSetNewUser = false
        val newUserDocumentRef = firebaseSource.db.collection("users").document(documentUsernameID)

        newUserDocumentRef
            .set(newUser)
            .addOnSuccessListener {
                successSetNewUser = true
            }.addOnFailureListener {
                successSetNewUser = false
            }.await()

        return successSetNewUser
    }

    private suspend fun deleteDocument(documentPreviousUsernameID: String): Boolean {
        var successUpdate = false
        val previousUserDocumentRef = firebaseSource.db.collection("users").document(documentPreviousUsernameID)

        previousUserDocumentRef
            .delete()
            .addOnSuccessListener {
                successUpdate = true
            }.addOnFailureListener {
                successUpdate = false
            }.await()

        return successUpdate
    }

    suspend fun checkIfUsernameAvailable(username: String): Flow<ResultData<Boolean>> = flow {
        var nameAvaliable = false

        try {
            val userDocumentRef = firebaseSource.db.collection("users").document(username)
            userDocumentRef
                .get()
                .addOnSuccessListener {
                    nameAvaliable = false
                }.addOnSuccessListener {
                    nameAvaliable = true
                }.await()

            emit(ResultData.Success(nameAvaliable))

        } catch (e: Exception) {
            Log.i(TAG, "${e.message}")
        }
    }
}