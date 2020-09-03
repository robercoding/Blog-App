package com.rober.blogapp.data.network.firebase

import android.net.Uri
import android.util.Log
import com.google.firebase.firestore.Query
import com.google.firebase.storage.StorageReference
import com.rober.blogapp.data.ResultData
import com.rober.blogapp.entity.CountsPosts
import com.rober.blogapp.entity.Post
import com.rober.blogapp.entity.User
import com.rober.blogapp.ui.main.profile.profileedit.util.IntentImageCodes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.*
import javax.inject.Inject
import kotlin.Exception

class FirebaseProfileEditManager @Inject constructor(
    private val firebaseSource: FirebaseSource
) {
    private val TAG = "FirebaseProfileEditMana"

    val profile_image_path = "profile_image/"
    val background_image_path = "background_image/"

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
                val successUpdateUsername: Boolean

                if (usernameDocumentID != null) {
                    successUpdateUsername = updateDocumentByField(usernameDocumentID, "username", newUser.username)
                } else {
                    return@flow
                }

                //Set new user once username is changed
                val successSetNewUser: Boolean
                if (successUpdateUsername) {
                    successSetNewUser = setNewUserDocument(newUser.username, newUser)
                } else {
                    return@flow
                }


                //Get posts before they delete
                var listPostsFromPreviousUser = listOf<Post>()
                var countPostsFromPreviousUser = CountsPosts(0)

                if (successSetNewUser) {
                    listPostsFromPreviousUser = getAllDocumentsFromCollection(previousUser)
                    countPostsFromPreviousUser = getDocumentCountPosts(previousUser)
                } else {
                    return@flow
                }
                //Set PostsCollection to the new user
                val successSetListPosts = setAllPostsToCollection(listPostsFromPreviousUser, newUser)
                val successSetCountPosts = setCountPosts(countPostsFromPreviousUser, newUser)

                //Delete the old posts and user once new data has been set
                val successDeleteUser: Boolean
                val successDeletePreviousUserPosts: Boolean
                if (successSetListPosts) {
                    successDeleteUser = deleteUserDocument(previousUser.username)
                    successDeletePreviousUserPosts = deletePostsPathRecursive(previousUser.username)
                } else {
                    return@flow
                }

                if (!successSetCountPosts) {
                    //Count all documents and set new counts
                }

                //Check everything went good
                if (successUpdateUsername && successSetNewUser && successDeleteUser && successDeletePreviousUserPosts)
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
                firebaseSource.userChangedUsername = true
                firebaseSource.usernameBeforeChange = previousUser.username

                firebaseSource.user = newUser
                firebaseSource.username = newUser.username
                emit(ResultData.Success(successUpdateUser))

            } else if (!differentUsernames && successUpdateUser) {
                firebaseSource.user = newUser
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

    private suspend fun getAllDocumentsFromCollection(previousUser: User): List<Post> {
        val previousUserCollectionPosts =
            firebaseSource.db.collection("posts").document(previousUser.username).collection("user_posts")
        var listPostsFromPreviousUser = listOf<Post>()

        previousUserCollectionPosts.get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    return@addOnSuccessListener
                }

                listPostsFromPreviousUser = querySnapshot.toObjects(Post::class.java)
            }.addOnFailureListener {
                throw it
            }.await()

        return listPostsFromPreviousUser
    }

    private suspend fun getDocumentCountPosts(previousUser: User): CountsPosts {
        val countPostsReference =
            firebaseSource.db.collection("posts").document(previousUser.username).collection("user_count_posts")
                .document("countPosts")

        val countPostsDocument = countPostsReference.get().await()


        val countPost = countPostsDocument.toObject(CountsPosts::class.java)

        countPost?.let {
            return countPost
        } ?: kotlin.run {
            return CountsPosts(0)
        }
    }

    private suspend fun setAllPostsToCollection(listPostsFromPreviousUser: List<Post>, newUser: User): Boolean {
//        val collectionNewUser = firebaseSource.db.collection("posts").document(newUser.username).collection("user_posts")

        val successCreatePostsDocumentForNewUser = createPostsDocumentForNewUser(newUser)
        if (!successCreatePostsDocumentForNewUser) {
            return false
        }
        return setPostsInCollection(listPostsFromPreviousUser, newUser)
    }

    private suspend fun createPostsDocumentForNewUser(newUser: User): Boolean {
        val postsDocumentUsername = firebaseSource.db.collection("posts").document(newUser.username)
        var success = false
        val documentContainsCollections = hashMapOf("documentContainsCollections" to false)
        postsDocumentUsername
            .set(documentContainsCollections)
            .addOnSuccessListener {
                success = true
            }.addOnFailureListener {
                success = false
            }.await()

        return success
    }

    private suspend fun setPostsInCollection(listPostsFromPreviousUser: List<Post>, newUser: User): Boolean {
        var success = false

        for (post in listPostsFromPreviousUser) {
            post.user_creator_id = newUser.username
            val userPostsDocument =
                firebaseSource.db.collection("posts").document(newUser.username).collection("user_posts").document()

            userPostsDocument.set(post)
                .addOnSuccessListener {
                    success = true
                }
                .addOnFailureListener {
                    success = false
                    Log.i(TAG, "Exception: $it")
                }.await()
        }
        return success
    }

    private suspend fun setCountPosts(countsPosts: CountsPosts, newUser: User): Boolean {
        val userCountPostsDocument =
            firebaseSource.db.collection("posts").document(newUser.username).collection("user_count_posts")
                .document("countPosts")
        var success = false
        userCountPostsDocument.set(countsPosts)
            .addOnSuccessListener {
                success = true
            }
            .addOnFailureListener {
                success = false
                Log.i(TAG, "Exception: $it")
                throw it
            }.await()

        return success
    }

    private suspend fun deleteAnyDocumentRecursive(path: String): Boolean {
        val data = hashMapOf(
            "path" to path
        )

        var success = false

        firebaseSource.functions
            .getHttpsCallable("deleteDocumentRecursive")
            .call(data)
            .continueWith { task ->
                success = task.isSuccessful
            }.await()


        Log.i("SuccessDeletePosts", "$success")
        return success
    }

    private suspend fun deletePostsPathRecursive(documentPreviousUsernameID: String): Boolean {
        val path = "/posts/$documentPreviousUsernameID"

        return deleteAnyDocumentRecursive(path)
    }

    private suspend fun setNewUserDocument(documentUsernameID: String, newUser: User): Boolean {
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

    private suspend fun deleteUserDocument(documentPreviousUsernameID: String): Boolean {
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

    private suspend fun getFollowingCollection(){

    }

    //TODO
    private suspend fun deleteFollowingPathRecursive(documentPreviousUsernameID: String): Boolean {
        val path = "/posts/$documentPreviousUsernameID"

        return deleteAnyDocumentRecursive(path)
    }

    suspend fun checkIfUsernameAvailable(username: String): Flow<ResultData<Boolean>> = flow {
        var nameAvailable = false

        try {
            val userDocumentRef = firebaseSource.db.collection("users").document(username)
            userDocumentRef
                .get()
                .addOnSuccessListener {
                    nameAvailable = false
                }.addOnSuccessListener {
                    nameAvailable = true
                }.await()

            emit(ResultData.Success(nameAvailable))

        } catch (e: Exception) {
            Log.i(TAG, "${e.message}")
        }
    }

    suspend fun saveImage(uri: Uri, intentImageCode: Int): Flow<ResultData<String>> = flow {
        var storageReference: StorageReference?
        var returnImageUrl = ""

        storageReference = if (intentImageCode == IntentImageCodes.PROFILE_IMAGE_CODE) {
            firebaseSource.storage.reference.child(profile_image_path + "${Date().time}")
        } else {
            firebaseSource.storage.reference.child(background_image_path + "${Date().time}")
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