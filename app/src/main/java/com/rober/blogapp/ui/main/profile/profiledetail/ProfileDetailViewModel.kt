package com.rober.blogapp.ui.main.profile.profiledetail

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
import com.rober.blogapp.R
import com.rober.blogapp.data.ResultData
import com.rober.blogapp.data.network.repository.FirebaseRepository
import com.rober.blogapp.entity.User
import com.rober.blogapp.ui.main.profile.profiledetail.utils.ProfileUserCodes
import com.rober.blogapp.util.AsyncResponse
import com.rober.blogapp.util.GetImageBitmapFromUrlAsyncTask
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.net.URL


class ProfileDetailViewModel
@ViewModelInject constructor(
    private val firebaseRepository: FirebaseRepository,
    private val application: Application
) : ViewModel(), AsyncResponse {


    private val TAG = "ProfileDetailViewModel"
    private var _profileDetailState: MutableLiveData<ProfileDetailState> = MutableLiveData()

    val profileDetailState: LiveData<ProfileDetailState>
        get() = _profileDetailState

    var user: User? = null
    var previousBackgroundImageUrl = ""
    var bitmap : Bitmap? = null

    private var currentUserFollowsOtherUser = false
    private var PROFILE_USER = ProfileUserCodes.EMPTY_USER_PROFILE

    private var colorUrl = 0

    fun setIntention(event: ProfileDetailFragmentEvent) {
        when (event) {
            is ProfileDetailFragmentEvent.LoadUserDetails -> {
                if (event.name.isNullOrBlank()) {
                    getCurrentUser()
                    //getCurrentUserPosts()
                } else {
                    val isUserCurrentUser = checkIfUserIsCurrentUser(event.name)

                    if (isUserCurrentUser)
                        getCurrentUser()
                    else
                        getUserProfile(event.name)
                }
            }

            is ProfileDetailFragmentEvent.LoadUserPosts -> {
                Log.i("RequestPosts", "RequestsPosts Before choice")
                user?.let {
                    getUserPosts()
                }
            }

            is ProfileDetailFragmentEvent.LoadNewerPosts -> {
                user?.let {
                    getNewerPosts()
                }
            }

            is ProfileDetailFragmentEvent.Follow -> {
                user?.let {
                    followOtherUser()
                } ?: kotlin.run {
                    _profileDetailState.value =
                        ProfileDetailState.Error(Exception("We couldn't find the user, sorry"))
                }
            }

            is ProfileDetailFragmentEvent.Unfollow -> {
                user?.let {
                    unfollowOtherUser()
                } ?: kotlin.run {
                    _profileDetailState.value =
                        ProfileDetailState.Error(Exception("We couldn't find the user, sorry"))
                }
            }

            is ProfileDetailFragmentEvent.NavigateToProfileEdit -> {
                user?.let { user ->
                    _profileDetailState.value = ProfileDetailState.NavigateToProfileEdit(user)
                } ?: kotlin.run {
                    _profileDetailState.value = ProfileDetailState.Idle
                }
            }

            is ProfileDetailFragmentEvent.Idle -> {
                _profileDetailState.value = ProfileDetailState.Idle
            }
        }
    }

    private fun getCurrentUser() {
        _profileDetailState.value = ProfileDetailState.LoadingUser
        PROFILE_USER = ProfileUserCodes.CURRENT_USER_PROFILE
        user = firebaseRepository.getCurrentUser()

        user?.let {
            getBitmapFromUrl(it.backgroundImageUrl)
        }?: kotlin.run {
            _profileDetailState.value =
                ProfileDetailState.Error(Exception("We couldn't provide the user"))
        }
    }

    private fun checkIfUserIsCurrentUser(username: String): Boolean {
        var isUserCurrentUser = false

        viewModelScope.launch {
            firebaseRepository.getCurrentUserProfileDetail()
                .collect { resultData ->
                    when (resultData) {
                        is ResultData.Success -> {
                            if (username == resultData.data?.username)
                                isUserCurrentUser = true
                        }
                        is ResultData.Error -> {
                            isUserCurrentUser = false
                        }
                    }
                }
        }
        return isUserCurrentUser
    }

    private fun getUserProfile(username: String) {
        _profileDetailState.value = ProfileDetailState.LoadingUser
        PROFILE_USER = ProfileUserCodes.OTHER_USER_PROFILE
        viewModelScope.launch {
            delay(200)
            firebaseRepository.getUserProfile(username)
                .collect { resultData ->
                    when (resultData) {
                        is ResultData.Success -> {
                            resultData.data?.let { resultDataUser ->
                                user = resultDataUser
                                currentUserFollowsOtherUser = checkIfCurrentUserFollowsOtherUser(username)
//                                val bitmap = getBitmapFromUrl(imageUrl)
//                                val bitmap = getBitmapLightWeight(imageUrl)
                            }
                        }

                        is ResultData.Error -> {
                            _profileDetailState.value =
                                ProfileDetailState.Error(resultData.exception)
                        }
                    }
                }

            user?.let {
                getBitmapFromUrl(it.backgroundImageUrl)
            }
        }
    }

    private fun getBitmapFromUrl(urlImage: String) {
        val imageBitmapFromUrlAsyncTask = GetImageBitmapFromUrlAsyncTask()
        imageBitmapFromUrlAsyncTask.delegate = this
        imageBitmapFromUrlAsyncTask.execute(urlImage)
    }

    override fun processFinish(processedBitmap : Bitmap) {
        bitmap = processedBitmap

        user?.run {
            if (PROFILE_USER == ProfileUserCodes.CURRENT_USER_PROFILE) {
                _profileDetailState.value = ProfileDetailState.SetCurrentUserProfile(this, processedBitmap)
            } else if (PROFILE_USER == ProfileUserCodes.OTHER_USER_PROFILE) {
                _profileDetailState.value =
                    ProfileDetailState.SetOtherUserProfile(this, currentUserFollowsOtherUser, processedBitmap)
            }
        }
    }

//    private fun getDominantColorFromBitmap(bitmap: Bitmap) {
//
//        Palette.Builder(bitmap).generate { palette ->
//            colorUrl = palette?.let {
//                it.getDominantColor(ContextCompat.getColor(application.applicationContext, R.color.colorBlack))
//            } ?: kotlin.run {
//                Log.i("Palette", "Not work")
//            }
//        }
//        Log.i("CurrentColor", "We are outside woah")
//    }

    private fun getUserPosts() {
//        _profileDetailState.value = ProfileDetailState.LoadingPosts
        viewModelScope.launch {
            firebaseRepository.retrieveProfileUsersPosts(user!!.username)
                .collect { resultData ->
                    when (resultData) {
                        is ResultData.Success -> {
                            _profileDetailState.value =
                                ProfileDetailState.SetUserPosts(resultData.data!!)
                        }
                        is ResultData.Error -> {
                            _profileDetailState.value =
                                ProfileDetailState.Error(Exception("Sorry we couldn't load posts"))
                        }
                        is ResultData.Loading -> {
                            _profileDetailState.value = ProfileDetailState.LoadingPosts
                        }
                    }
                }
        }
    }

    private fun getNewerPosts() {
        viewModelScope.launch {

            user = firebaseRepository.getCurrentUserRefreshed()

            user?.also {tempUser ->
                if(tempUser.backgroundImageUrl != previousBackgroundImageUrl){
                    getBitmapFromUrl(tempUser.backgroundImageUrl)
                }else{
                    bitmap?.let {tempBitmap ->
                        _profileDetailState.value = ProfileDetailState.SetCurrentUserProfile(tempUser, tempBitmap)
                    }
                }
            }

            firebaseRepository.retrieveNewerPostsUserProfile(user!!.username)
                .collect { resultData ->
                    when (resultData) {
                        is ResultData.Success -> {
                            _profileDetailState.value =
                                ProfileDetailState.SetUserPosts(resultData.data!!)
                        }
                    }
                }
        }
    }

    private suspend fun checkIfCurrentUserFollowsOtherUser(otherUsername: String): Boolean {
        var currentUserFollowsOtherUser = false

        firebaseRepository.checkIfCurrentUserFollowsOtherUser(otherUsername)
            .collect { resultData ->
                when (resultData) {
                    is ResultData.Success -> {
                        currentUserFollowsOtherUser = resultData.data!!
                    }

                    is ResultData.Error -> currentUserFollowsOtherUser = false
                }
            }

        return currentUserFollowsOtherUser
    }

    private fun followOtherUser() {
        viewModelScope.launch {
            firebaseRepository.followOtherUser(user!!)
                .collect { resultData ->
                    when (resultData) {
                        is ResultData.Success -> {
                            if (resultData.data!!) {
                                user?.let {
                                    user?.follower = it.follower.plus(1)
                                }
                                Log.i("UserFollower", "After Plus ${user?.follower}")
                                _profileDetailState.value = ProfileDetailState.Followed(user!!)
                            } else
                                _profileDetailState.value = ProfileDetailState.FollowError
                        }
                        is ResultData.Error -> {
                            _profileDetailState.value = ProfileDetailState.FollowError
                        }
                    }
                }
        }
    }

    private fun unfollowOtherUser() {
        viewModelScope.launch {
            firebaseRepository.unfollowOtherUser(user!!)
                .collect { resultData ->
                    when (resultData) {
                        is ResultData.Success -> {
                            if (resultData.data!!) {
                                Log.i("UserFollower", "Before resting wtf ${user?.follower}")
                                user?.let {
                                    user?.follower = it.follower.minus(1)
                                }
                                Log.i("UserFollower", "After resting wtf ${user?.follower}")

                                _profileDetailState.value = ProfileDetailState.Unfollowed(user!!)
                            } else {
                                _profileDetailState.value = ProfileDetailState.UnfollowError

                            }
                        }
                        is ResultData.Error -> {
                            _profileDetailState.value =
                                ProfileDetailState.UnfollowError
                        }
                    }
                }
        }
    }
}