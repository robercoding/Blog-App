package com.rober.blogapp.data.network.firebase

import android.net.Uri
import android.util.Log
import com.google.firebase.storage.StorageReference
import com.rober.blogapp.data.ResultData
import com.rober.blogapp.entity.User
import com.rober.blogapp.ui.main.profile.profileedit.util.IntentImageCodes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import org.threeten.bp.Instant
import org.threeten.bp.temporal.ChronoUnit
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class FirebaseProfileEditManager @Inject constructor(
    private val firebaseSource: FirebaseSource
) {
    private val TAG = "FirebaseProfileEditMana"

    private val profileImagePath = "profile_image/"
    private val backgroundImagePath = "background_image/"

    suspend fun updateUser(previousUser: User, newUser: User): Flow<ResultData<Boolean>> = flow {
        val successUpdateUser: Boolean
        val differentUsernames = previousUser.username != newUser.username

        try {
            //Check if they are different names
            successUpdateUser = if (differentUsernames) {
                firebaseSource.user?.also {tempUser->

                    if(tempUser.lastDateUsernameChange == 0.toLong()){
                        return@also
                    }

                    val dateMonthAgoEpochSecond = Instant.now().minus(31, ChronoUnit.DAYS).epochSecond
                    if(tempUser.lastDateUsernameChange > dateMonthAgoEpochSecond){
                        val formatter = SimpleDateFormat.getDateInstance()
                        val dateMonthPlusEpochSecond = Instant.now().plus(31, ChronoUnit.DAYS).epochSecond
                        val date = formatter.format(Date(dateMonthPlusEpochSecond*1000))

                        emit(ResultData.Error(Exception("Sorry, you can't change the username until date: $date")))
                        return@flow
                    }
                }

                //Find the  document that contains username
                val usernameDocumentID = findDocumentIDByField("username", previousUser.username)

                //if usernameDocumentID exists then proceed to change it on server-side
                if(usernameDocumentID != null){
                    updateUserWithUsername(previousUser, newUser)
                }else{
                    return@flow
                }

            } else {
                updateUserDetails(newUser)
            }

            when(successUpdateUser){
                true -> {
                    if(differentUsernames){ //Change firebase source, so it updates in the rest of fragments.
                        firebaseSource.userChangedUsername = true
                        firebaseSource.usernameBeforeChange = previousUser.username

                        firebaseSource.user = newUser
                        firebaseSource.username = newUser.username
                        emit(ResultData.Success(successUpdateUser))
                    }else{
                        firebaseSource.user = newUser
                        emit(ResultData.Success(successUpdateUser))
                    }
                }
                false -> {
                    if(differentUsernames){
                        emit(ResultData.Error(Exception("Sorry we couldn't update the username")))
                    }else{
                        emit(ResultData.Error(Exception("Sorry we couldn't update the user")))
                    }
                }
            }

        } catch (e: Exception) {
            Log.i(TAG, "${e.message}")
        }
    }

    private suspend fun updateUserWithUsername(previousUser: User, newUser: User): Boolean{
        var successUpdate = false

        val updateUsernameMap = hashMapOf(
            "previousUsername" to previousUser.username,
            "newUsername" to newUser.username,
            "follower" to newUser.follower,
            "following" to newUser.following,
            "biography" to newUser.biography,
            "location" to newUser.location,
            "profileImageUrl" to newUser.profileImageUrl,
            "backgroundImageUrl" to newUser.backgroundImageUrl,
            "lastDateUsernameChange" to newUser.lastDateUsernameChange
        )

        //Change it on server-side
            firebaseSource.functions
                .getHttpsCallable("changeUsername")
                .call(updateUsernameMap)
                .addOnCompleteListener {
                    Log.i("ChangeUsername", "SuccessChangeUsername = ${it.isSuccessful}")
                    successUpdate = it.isSuccessful
                }.await()


        return successUpdate
    }

    private suspend fun updateUserDetails(newUser: User): Boolean{
        var successUpdateUser = false

        val updateUserMap = mapOf(
            "username" to newUser.username,
            "biography" to newUser.biography,
            "location" to newUser.location,
            "profileImageUrl" to newUser.profileImageUrl,
            "backgroundImageUrl" to newUser.backgroundImageUrl
        )
        Log.i("ChangeUsername", "Change bio and loc = $updateUserMap")

        //Proceed to change it on server-side
        firebaseSource.functions
            .getHttpsCallable("changeUserDetails")
            .call(updateUserMap)
            .addOnCompleteListener {
                successUpdateUser = it.isSuccessful
            }.await()

        return successUpdateUser
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

    suspend fun checkIfUsernameAvailable(username: String): Flow<ResultData<Boolean>> = flow {
        var nameAvailable = false

        try {
            val userDocumentRef = firebaseSource.db.collection("users").document(username)
            userDocumentRef
                .get()
                .addOnSuccessListener {
                    val user = it.toObject(User::class.java)
                    nameAvailable = user == null
                }.addOnFailureListener {
                    nameAvailable = false
                }.await()
        } catch (e: Exception) {
            Log.i(TAG, "${e.message}")
        }

        emit(ResultData.Success(nameAvailable))
    }

    suspend fun saveImage(uri: Uri, intentImageCode: Int): Flow<ResultData<String>> = flow {
        val storageReference: StorageReference?
        var returnImageUrl = ""

        storageReference = if (intentImageCode == IntentImageCodes.PROFILE_IMAGE_CODE) {
            firebaseSource.storage.reference.child(profileImagePath + "${Date().time}")
        } else {
            firebaseSource.storage.reference.child(backgroundImagePath + "${Date().time}")
        }

        val uploadImage = storageReference.putFile(uri)
        uploadImage.continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let {
                    throw it
                }
            }
            storageReference.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                returnImageUrl = task.result.toString()
            }
        }.await()

        if (returnImageUrl.isEmpty()) {
            emit(ResultData.Error(Exception("There was an error, sorry"), null))
        } else {
            emit(ResultData.Success(returnImageUrl))
        }
    }
}