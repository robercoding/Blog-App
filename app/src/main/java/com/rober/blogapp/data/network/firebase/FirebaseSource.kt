package com.rober.blogapp.data.network.firebase

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.rober.blogapp.data.network.util.FirebasePath
import com.rober.blogapp.entity.*
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.Exception

class FirebaseSource @Inject constructor(private val firebasePath: FirebasePath) {
    private val TAG = "FirebaseSource"

    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val storage = FirebaseStorage.getInstance()
    val functions = FirebaseFunctions.getInstance()

    var userAuth: FirebaseUser? = null
    var user: User? = null
    var username = ""
    var userDocumentUID: UserDocumentUID? = null
    var followingList: MutableList<Following>? = null
    var followerList: MutableList<Follower>? = null

    var userChangedUsername = false
    var usernameBeforeChange = ""

    var feedCheckedUserChangedUsername = false
    var profileDetailCheckedUserChangedUsername = false

    //When user goes back to FeedFragment check if there's a new activity on following to retrieve or not the their feeds
    val listNewFollowingsUsername = HashSet<String>()
    val listNewUnfollowingsUsername = HashSet<String>()

    val listNewFollowersUsername = HashSet<String>()
    val listNewUnfollowersUsername = HashSet<String>()

    suspend fun setCurrentUser() {
        Log.i("User:", "First time user -> $user and $username")
        var tempUsername = Username()
        var tempUser = User()
        Log.i(TAG, "${userAuth.toString()}")
        if (userAuth != null) {
            try {
                Log.i("User:", "FirebaseSource: uid ${userAuth!!.uid}")
                db.collection("usernames").document(userAuth!!.uid)
                    .get()
                    .addOnSuccessListener {
                        Log.i("User:", "Success")
                        if (it.exists())
                            tempUsername = it.toObject(Username::class.java)!!
                    }.addOnFailureListener {
                        Log.i("User:", "Failure")
                        username = "noname"
                    }
                    .await()

                if (!tempUsername.isEmpty()) {
                    tempUser =
                        db.collection(firebasePath.users_col).document(tempUsername.username).get().await().toObject(User::class.java)!!
                }

            } catch (e: Exception) {
                Log.i(TAG, e.message.toString())
            }

            if (!tempUser.isEmpty())
                user = tempUser

            if (!tempUsername.isEmpty())
                username = tempUsername.username

            Log.i(TAG, "User: $user")
        }
    }

    suspend fun setCurrentUserDocumentsUID() {
        if(user == null)
            return

        try {
            val userDocumentsRef = db.collection(firebasePath.user_documents_uid).whereEqualTo("username", user!!.username)

            userDocumentsRef
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        val listUserDocumentsUID = querySnapshot.toObjects(UserDocumentUID::class.java)
                        when (listUserDocumentsUID.size) {
                            1 -> userDocumentUID = listUserDocumentsUID[0]
                            else -> throw Exception("Something went wrong when getting the documents UID")
                        }
                    }
                }.await()
        } catch (e: Exception) {

        }

        Log.i("UserDocumentsUID", "$userDocumentUID")
    }

    suspend fun setCurrentFollowing() {
        if (userDocumentUID == null)
            return

        try {
            val followingRef = db.collection(firebasePath.following_col).document(userDocumentUID!!.followerDocumentUid)
                .collection(firebasePath.user_following)

            followingList = followingRef
                .get()
                .await()
                .toObjects(Following::class.java)

        } catch (e: Exception) {
            Log.i(TAG, "$e")
        }
    }

    suspend fun setCurrentFollower() {
        if (userDocumentUID == null)
            return

        try {
            val followerRef = db.collection(firebasePath.follower_col).document(userDocumentUID!!.followerDocumentUid)
                .collection(firebasePath.user_followers)

            followerList = followerRef
                .get()
                .await()
                .toObjects(Follower::class.java)

        } catch (e: Exception) {
            Log.i(TAG, "$e")
        }
    }

    fun getCurrentUser(): User {
        return if (user != null)
            user!!
        else
            User()
    }
}