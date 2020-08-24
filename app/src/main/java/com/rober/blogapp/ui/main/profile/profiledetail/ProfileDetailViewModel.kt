package com.rober.blogapp.ui.main.profile.profiledetail

import android.util.Log
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import com.rober.blogapp.data.ResultData
import com.rober.blogapp.data.network.repository.FirebaseRepository
import com.rober.blogapp.entity.User
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch


class ProfileDetailViewModel
@ViewModelInject constructor(
    private val firebaseRepository: FirebaseRepository
) : ViewModel() {

    private val TAG = "ProfileDetailViewModel"
    private var _profileDetailState: MutableLiveData<ProfileDetailState> = MutableLiveData()

    val profileDetailState: LiveData<ProfileDetailState>
        get() = _profileDetailState

    var user: User? = null

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

            is ProfileDetailFragmentEvent.Idle -> {
                _profileDetailState.value = ProfileDetailState.Idle
            }
        }
    }

    private fun getCurrentUser() {
        _profileDetailState.value = ProfileDetailState.LoadingPosts
        _profileDetailState.value = ProfileDetailState.LoadingUser
        viewModelScope.launch {
            firebaseRepository.getCurrentUser()
                .collect { resultData ->
                    when (resultData) {
                        is ResultData.Success -> {
                            resultData.data?.let { resultDataUser ->
                                user = resultDataUser
                                _profileDetailState.value =
                                    ProfileDetailState.SetCurrentUserProfile(resultData.data)

                            } ?: kotlin.run {
                                _profileDetailState.value =
                                    ProfileDetailState.Error(Exception("We couldn't provide the user"))
                            }
                        }
                    }
                }
        }
    }

    private fun checkIfUserIsCurrentUser(username: String): Boolean {
        var isUserCurrentUser = false

        viewModelScope.launch {
            firebaseRepository.getCurrentUser()
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

        viewModelScope.launch {
            firebaseRepository.getUserProfile(username)
                .collect { resultData ->
                    when (resultData) {
                        is ResultData.Success -> {
                            resultData.data?.let { resultDataUser ->
                                user = resultDataUser
                                val currentUserFollowsOtherUser = currentUserFollowsOtherUser(username)
                                _profileDetailState.value = ProfileDetailState.SetOtherUserProfile( user!!, currentUserFollowsOtherUser)
                            }
                        }

                        is ResultData.Error -> {
                            _profileDetailState.value =
                                ProfileDetailState.Error(resultData.exception)
                        }
                    }
                }
        }
    }

    private fun getUserPosts() {
        viewModelScope.launch {
            firebaseRepository.retrieveProfileUsersPosts(user!!.username)
                .collect { resultData ->
                    when (resultData) {
                        is ResultData.Success -> {
                            Log.i("RequestPosts", " Success ${resultData.data}")
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

    private suspend fun currentUserFollowsOtherUser(otherUsername: String): Boolean {
        var currentUserFollowsOtherUser = false

        firebaseRepository.currentUserFollowsOtherUser(otherUsername)
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
                                Log.i("UserFollower", "Before Plus ${user?.follower}")
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