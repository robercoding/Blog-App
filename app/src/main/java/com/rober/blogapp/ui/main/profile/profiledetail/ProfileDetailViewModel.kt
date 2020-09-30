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
import com.rober.blogapp.entity.Post
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

    private var user: User? = null
    private var userPosts = mutableListOf<Post>()
    private var previousBackgroundImageUrl = ""
    private var bitmap: Bitmap? = null

    private var currentUserFollowsOtherUser = false
    private var PROFILE_USER = ProfileUserCodes.EMPTY_USER_PROFILE

    private var colorUrl = 0
    private var isUserFollowingInAction = false
    private var isUserUnfollowingInAction = false

    fun setIntention(event: ProfileDetailFragmentEvent) {
        when (event) {
            is ProfileDetailFragmentEvent.SetUserObjectDetails -> {
                user = event.user
                viewModelScope.launch {
                    user?.run {
                        setUserObjectDetails(this)
                    } ?: kotlin.run {
                        _profileDetailState.value = ProfileDetailState.PopBackStack
                    }
                }
            }

            is ProfileDetailFragmentEvent.LoadUserDetails -> {
                if (event.userUID.isNullOrBlank()) {
                    getCurrentUser()
                    //getCurrentUserPosts()
                } else {
                    viewModelScope.launch {
                        val isUserCurrentUser = checkIfUserIsCurrentUser(event.userUID)
                        if (isUserCurrentUser)
                            getCurrentUser()
                        else
                            getUserProfile(event.userUID)
                    }
                }
            }

            is ProfileDetailFragmentEvent.LoadUserPosts -> {
                user?.let {
                    getUserPosts()
                }
            }

            is ProfileDetailFragmentEvent.LoadNewerPosts -> {
                user?.let {
                    getNewerPosts()
                }
            }

            is ProfileDetailFragmentEvent.LoadBackgroundImage -> {
                user?.run {
                    _profileDetailState.value = ProfileDetailState.LoadBackgroundImage(backgroundImageUrl)
                } ?: kotlin.run {
                    _profileDetailState.value = ProfileDetailState.Idle
                }
            }

            is ProfileDetailFragmentEvent.LoadProfileImage -> {
                user?.run {
                    _profileDetailState.value = ProfileDetailState.LoadProfileImage(profileImageUrl)
                } ?: kotlin.run {
                    _profileDetailState.value = ProfileDetailState.Idle
                }
            }

            is ProfileDetailFragmentEvent.Follow -> {
                if (!isUserFollowingInAction) {
                    isUserFollowingInAction = true
                    user?.let {
                        followOtherUser()
                    } ?: kotlin.run {
                        _profileDetailState.value =
                            ProfileDetailState.Error(Exception("We couldn't find the user, sorry"))
                    }
                }
            }

            is ProfileDetailFragmentEvent.Unfollow -> {
                if (!isUserUnfollowingInAction) {
                    isUserUnfollowingInAction = true
                    user?.let {
                        unfollowOtherUser()
                    } ?: kotlin.run {
                        _profileDetailState.value =
                            ProfileDetailState.Error(Exception("We couldn't find the user, sorry"))
                    }
                }
            }

            is ProfileDetailFragmentEvent.NavigateToPostDetail -> {
                val post = userPosts[event.positionAdapter]

                _profileDetailState.value = ProfileDetailState.NavigateToPostDetail(post)
            }

            is ProfileDetailFragmentEvent.NavigateToProfileEdit -> {
                user?.let { user ->
                    _profileDetailState.value = ProfileDetailState.NavigateToProfileEdit(user)
                } ?: kotlin.run {
                    _profileDetailState.value = ProfileDetailState.Idle
                }
            }

            is ProfileDetailFragmentEvent.PopBackStack -> _profileDetailState.value = ProfileDetailState.PopBackStack

            is ProfileDetailFragmentEvent.Idle -> {
                _profileDetailState.value = ProfileDetailState.Idle
            }
        }
    }

    private suspend fun setUserObjectDetails(user: User) {
        _profileDetailState.value = ProfileDetailState.LoadingUser

        val isUserTheCurrentUser = checkIfUserIsCurrentUser(user.user_id)


        if (isUserTheCurrentUser)
            PROFILE_USER = ProfileUserCodes.CURRENT_USER_PROFILE
        else{
            currentUserFollowsOtherUser = checkIfCurrentUserFollowsOtherUser(user.user_id)
            PROFILE_USER = ProfileUserCodes.OTHER_USER_PROFILE
        }

        if (user.backgroundImageUrl.isEmpty()) {
            bitmap = createBitmapOrangeScreen()

            bitmap?.also { tempBitmap ->
                setUserDetails(user, tempBitmap)
            } ?: kotlin.run {
                _profileDetailState.value = ProfileDetailState.PopBackStack
            }

        } else {
            getBitmapFromUrl(user.backgroundImageUrl)
        }
    }

    private fun getCurrentUser() {
        _profileDetailState.value = ProfileDetailState.LoadingUser

        PROFILE_USER = ProfileUserCodes.CURRENT_USER_PROFILE
        user = firebaseRepository.getCurrentUser()

        user?.let { tempUser ->
            if (tempUser.backgroundImageUrl.isEmpty()) {
                val bitmapOrangeScreen = createBitmapOrangeScreen()
                _profileDetailState.value = ProfileDetailState.SetCurrentUserProfile(tempUser, bitmapOrangeScreen)
            } else {
                getBitmapFromUrl(tempUser.backgroundImageUrl)
            }
        } ?: kotlin.run {
            _profileDetailState.value =
                ProfileDetailState.Error(Exception("We couldn't provide the user"))
        }
    }

    private suspend fun checkIfUserIsCurrentUser(userUID: String): Boolean {
        var isUserCurrentUser = false

        val job = viewModelScope.launch {
            firebaseRepository.getCurrentUserProfileDetail()
                .collect { resultData ->
                    when (resultData) {
                        is ResultData.Success -> {
                            if (userUID == resultData.data?.user_id)
                                isUserCurrentUser = true
                        }
                        is ResultData.Error -> {
                            isUserCurrentUser = false
                        }
                    }
                }
        }
        job.join()
        return isUserCurrentUser
    }

    private fun getUserProfile(userUID: String) {
        _profileDetailState.value = ProfileDetailState.LoadingUser
        PROFILE_USER = ProfileUserCodes.OTHER_USER_PROFILE
        viewModelScope.launch {
            delay(200)
            firebaseRepository.getUserProfile(userUID)
                .collect { resultData ->
                    when (resultData) {
                        is ResultData.Success -> {
                            resultData.data?.let { resultDataUser ->
                                user = resultDataUser
                                currentUserFollowsOtherUser = checkIfCurrentUserFollowsOtherUser(resultData.data.user_id)
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

            user?.let { tempUser ->
                if (tempUser.backgroundImageUrl.isEmpty()) {
                    val bitmapOrangeScreen = createBitmapOrangeScreen()
                    _profileDetailState.value =
                        ProfileDetailState.SetOtherUserProfile(tempUser, currentUserFollowsOtherUser, bitmapOrangeScreen)
                } else {
                    getBitmapFromUrl(tempUser.backgroundImageUrl)
                }

            }
        }
    }

    private fun getBitmapFromUrl(urlImage: String) {
        val imageBitmapFromUrlAsyncTask = GetImageBitmapFromUrlAsyncTask()
        imageBitmapFromUrlAsyncTask.delegate = this
        imageBitmapFromUrlAsyncTask.execute(urlImage)
    }

    override fun processFinish(processedBitmap: Bitmap?) {

        user?.also { tempUser ->
            processedBitmap?.also { tempProcessedBitmap ->
                bitmap = tempProcessedBitmap
                setUserDetails(tempUser, tempProcessedBitmap)

            } ?: kotlin.run {
                val bitmapOrangeScreen = createBitmapOrangeScreen()
                bitmap = bitmapOrangeScreen
                setUserDetails(tempUser, bitmapOrangeScreen)
//                _profileDetailState.value = ProfileDetailState.SetCurrentUserProfile(tempUser, bitmapOrangeScreen)
            }
        }
    }

    private fun createBitmapOrangeScreen(): Bitmap =
        BitmapFactory.decodeResource(application.applicationContext.resources, R.drawable.blue_screen)

    private fun setUserDetails(user: User, bitmap: Bitmap) {
        if (PROFILE_USER == ProfileUserCodes.CURRENT_USER_PROFILE) {
            _profileDetailState.value = ProfileDetailState.SetCurrentUserProfile(user, bitmap)
        } else if (PROFILE_USER == ProfileUserCodes.OTHER_USER_PROFILE) {
            _profileDetailState.value =
                ProfileDetailState.SetOtherUserProfile(user, currentUserFollowsOtherUser, bitmap)
        } else {
            _profileDetailState.value = ProfileDetailState.PopBackStack
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
        user?.let { tempUser ->
            viewModelScope.launch {
                firebaseRepository.retrieveProfileUsersPosts(tempUser.user_id)
                    .collect { resultData ->
                        when (resultData) {
                            is ResultData.Success -> {
                                resultData.data?.also { tempUserPosts ->
                                    userPosts = tempUserPosts.toMutableList()
                                }
                                _profileDetailState.value =
                                    ProfileDetailState.SetUserPosts(userPosts, tempUser)
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
    }

    private fun getNewerPosts() {
        viewModelScope.launch {
            user?.also { tempUser ->
                previousBackgroundImageUrl = tempUser.backgroundImageUrl
            }
            user = firebaseRepository.getCurrentUserRefreshed()

            user?.also { tempUser ->
                if (tempUser.backgroundImageUrl != previousBackgroundImageUrl) {
                    getBitmapFromUrl(tempUser.backgroundImageUrl)
                } else {
                    bitmap?.let { tempBitmap ->
                        _profileDetailState.value = ProfileDetailState.SetCurrentUserProfile(tempUser, tempBitmap)
                    }
                }
                firebaseRepository.retrieveNewerPostsUserProfile(tempUser.user_id)
                    .collect { resultData ->
                        when (resultData) {
                            is ResultData.Success -> {
                                _profileDetailState.value =
                                    ProfileDetailState.SetUserPosts(resultData.data!!, tempUser)
                            }
                        }
                    }
            }
        }
    }

    private suspend fun checkIfCurrentUserFollowsOtherUser(userID: String): Boolean {
        var currentUserFollowsOtherUser = false

        firebaseRepository.checkIfCurrentUserFollowsOtherUser(userID)
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
                                    isUserFollowingInAction = false
                                }
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
                                user?.let {
                                    user?.follower = it.follower.minus(1)
                                    isUserUnfollowingInAction = false
                                }

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