package com.rober.blogapp.ui.main.profile.profiledetail

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.viewModelScope
import com.rober.blogapp.R
import com.rober.blogapp.data.ResultData
import com.rober.blogapp.data.network.repository.FirebaseRepository
import com.rober.blogapp.entity.Post
import com.rober.blogapp.entity.User
import com.rober.blogapp.ui.base.BaseViewModel
import com.rober.blogapp.ui.main.profile.profiledetail.utils.ProfileUserCodes
import com.rober.blogapp.util.AsyncResponse
import com.rober.blogapp.util.GetImageBitmapFromUrlAsyncTask
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class ProfileDetailViewModel
@ViewModelInject constructor(
    private val firebaseRepository: FirebaseRepository,
    private val application: Application
) : BaseViewModel<ProfileDetailState, ProfileDetailFragmentEvent>(), AsyncResponse {

    private var user: User? = null
    private var userPosts = mutableListOf<Post>()
    private var previousBackgroundImageUrl = ""
    private var bitmap: Bitmap? = null

    private var currentUserFollowsOtherUser = false
    private var PROFILE_USER = ProfileUserCodes.EMPTY_USER_PROFILE

    private var isUserFollowingInAction = false
    private var isUserUnfollowingInAction = false

    override fun setIntention(event: ProfileDetailFragmentEvent) {
        when (event) {
            is ProfileDetailFragmentEvent.SetUserObjectDetails -> {
                user = event.user
                viewModelScope.launch {
                    user?.run {
                        setUserObjectDetails(this)
                    } ?: kotlin.run {
                        viewState = ProfileDetailState.PopBackStack
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
                    viewState = ProfileDetailState.LoadBackgroundImage(backgroundImageUrl)
                } ?: kotlin.run {
                    viewState = ProfileDetailState.Idle
                }
            }

            is ProfileDetailFragmentEvent.LoadProfileImage -> {
                user?.run {
                    viewState = ProfileDetailState.LoadProfileImage(profileImageUrl)
                } ?: kotlin.run {
                    viewState = ProfileDetailState.Idle
                }
            }

            is ProfileDetailFragmentEvent.Follow -> {
                if (!isUserFollowingInAction) {
                    isUserFollowingInAction = true
                    user?.let {
                        followOtherUser()
                    } ?: kotlin.run {
                        viewState =
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
                        viewState =
                            ProfileDetailState.Error(Exception("We couldn't find the user, sorry"))
                    }
                }
            }

            is ProfileDetailFragmentEvent.NavigateToPostDetail -> {
                val post = userPosts[event.positionAdapter]

                viewState = ProfileDetailState.NavigateToPostDetail(post)
            }

            is ProfileDetailFragmentEvent.NavigateToSettings -> {
                viewState = ProfileDetailState.NavigateToSettings
            }

            is ProfileDetailFragmentEvent.NavigateToProfileEdit -> {
                user?.let { user ->
                    viewState = ProfileDetailState.NavigateToProfileEdit(user)
                } ?: kotlin.run {
                    viewState = ProfileDetailState.Idle
                }
            }

            is ProfileDetailFragmentEvent.PopBackStack -> viewState =
                ProfileDetailState.PopBackStack

            is ProfileDetailFragmentEvent.Idle -> {
                viewState = ProfileDetailState.Idle
            }
        }
    }

    private suspend fun setUserObjectDetails(user: User) {
        viewState = ProfileDetailState.LoadingUser

        val isUserTheCurrentUser = checkIfUserIsCurrentUser(user.userId)


        if (isUserTheCurrentUser)
            PROFILE_USER = ProfileUserCodes.CURRENT_USER_PROFILE
        else {
            currentUserFollowsOtherUser = checkIfCurrentUserFollowsOtherUser(user.userId)
            PROFILE_USER = ProfileUserCodes.OTHER_USER_PROFILE
        }

        if (user.backgroundImageUrl.isEmpty()) {
            bitmap = createBitmapOrangeScreen()

            bitmap?.also { tempBitmap ->
                setUserDetails(user, tempBitmap)
            } ?: kotlin.run {
                viewState = ProfileDetailState.PopBackStack
            }

        } else {
            getBitmapFromUrl(user.backgroundImageUrl)
        }
    }

    private fun getCurrentUser() {
        viewState = ProfileDetailState.LoadingUser

        PROFILE_USER = ProfileUserCodes.CURRENT_USER_PROFILE
        user = firebaseRepository.getCurrentUser()

        user?.let { tempUser ->
            if (tempUser.backgroundImageUrl.isEmpty()) {
                val bitmapOrangeScreen = createBitmapOrangeScreen()
                viewState =
                    ProfileDetailState.SetCurrentUserProfile(tempUser, bitmapOrangeScreen)
            } else {
                getBitmapFromUrl(tempUser.backgroundImageUrl)
            }
        } ?: kotlin.run {
            viewState =
                ProfileDetailState.Error(Exception("We couldn't provide the user"))
        }
    }

    private suspend fun checkIfUserIsCurrentUser(userUID: String): Boolean {
        var isUserCurrentUser = false

        job = viewModelScope.launch {
            firebaseRepository.getCurrentUserProfileDetail()
                .collect { resultData ->
                    when (resultData) {
                        is ResultData.Success -> {
                            if (userUID == resultData.data?.userId)
                                isUserCurrentUser = true
                        }
                        is ResultData.Error -> {
                            isUserCurrentUser = false
                        }
                    }
                }
        }
        job?.join()
        return isUserCurrentUser
    }

    private fun getUserProfile(userUID: String) {
        viewState = ProfileDetailState.LoadingUser
        PROFILE_USER = ProfileUserCodes.OTHER_USER_PROFILE
        viewModelScope.launch {
            delay(200)
            firebaseRepository.getUserProfile(userUID)
                .collect { resultData ->
                    when (resultData) {
                        is ResultData.Success -> {
                            resultData.data?.let { resultDataUser ->
                                user = resultDataUser
                                currentUserFollowsOtherUser =
                                    checkIfCurrentUserFollowsOtherUser(resultData.data.userId)
//                                val bitmap = getBitmapFromUrl(imageUrl)
//                                val bitmap = getBitmapLightWeight(imageUrl)
                            }
                        }

                        is ResultData.Error -> {
                            viewState =
                                ProfileDetailState.Error(resultData.exception)
                        }
                    }
                }

            user?.let { tempUser ->
                if (tempUser.backgroundImageUrl.isEmpty()) {
                    val bitmapOrangeScreen = createBitmapOrangeScreen()
                    viewState =
                        ProfileDetailState.SetOtherUserProfile(
                            tempUser,
                            currentUserFollowsOtherUser,
                            bitmapOrangeScreen
                        )
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
//                viewState = ProfileDetailState.SetCurrentUserProfile(tempUser, bitmapOrangeScreen)
            }
        }
    }

    private fun createBitmapOrangeScreen(): Bitmap =
        BitmapFactory.decodeResource(application.applicationContext.resources, R.drawable.blue_screen)

    private fun setUserDetails(user: User, bitmap: Bitmap) {
        if (PROFILE_USER == ProfileUserCodes.CURRENT_USER_PROFILE) {
            viewState = ProfileDetailState.SetCurrentUserProfile(user, bitmap)
        } else if (PROFILE_USER == ProfileUserCodes.OTHER_USER_PROFILE) {
            viewState =
                ProfileDetailState.SetOtherUserProfile(user, currentUserFollowsOtherUser, bitmap)
        } else {
            viewState = ProfileDetailState.PopBackStack
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
//        viewState = ProfileDetailState.LoadingPosts
        user?.let { tempUser ->
            viewModelScope.launch {
                firebaseRepository.retrieveProfileUsersPosts(tempUser.userId)
                    .collect { resultData ->
                        when (resultData) {
                            is ResultData.Success -> {
                                resultData.data?.also { tempUserPosts ->
                                    userPosts = tempUserPosts.toMutableList()
                                }
                                viewState =
                                    ProfileDetailState.SetUserPosts(userPosts, tempUser)
                            }
                            is ResultData.Error -> {
                                viewState =
                                    ProfileDetailState.Error(Exception("Sorry we couldn't load posts"))
                            }
                            is ResultData.Loading -> {
                                viewState = ProfileDetailState.LoadingPosts
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
                        viewState =
                            ProfileDetailState.SetCurrentUserProfile(tempUser, tempBitmap)
                    }
                }
                firebaseRepository.retrieveNewerPostsUserProfile(tempUser.userId)
                    .collect { resultData ->
                        when (resultData) {
                            is ResultData.Success -> {
                                viewState =
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
                                viewState = ProfileDetailState.Followed(user!!)
                            } else
                                viewState = ProfileDetailState.FollowError
                        }
                        is ResultData.Error -> {
                            viewState = ProfileDetailState.FollowError
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

                                viewState = ProfileDetailState.Unfollowed(user!!)
                            } else {
                                viewState = ProfileDetailState.UnfollowError

                            }
                        }
                        is ResultData.Error -> {
                            viewState =
                                ProfileDetailState.UnfollowError
                        }
                    }
                }
        }
    }
}