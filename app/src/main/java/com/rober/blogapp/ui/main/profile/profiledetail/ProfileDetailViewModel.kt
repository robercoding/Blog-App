package com.rober.blogapp.ui.main.profile.profiledetail

import android.util.Log
import androidx.hilt.Assisted
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
 ) : ViewModel(){

    private val TAG = "ProfileDetailViewModel"
    private var _profileDetailState : MutableLiveData<ProfileDetailState> = MutableLiveData()

    val profileDetailState: LiveData<ProfileDetailState>
        get() = _profileDetailState

    fun setIntention(event: ProfileDetailFragmentEvent){
        when(event){
            is ProfileDetailFragmentEvent.LoadUserDetails -> {
                if(event.name.isNullOrBlank()){
                    getCurrentUser()
                    //getCurrentUserPosts()
                }else{
                    val isUserCurrentUser = checkIfUserIsCurrentUser(event.name)

                    if(isUserCurrentUser)
                        getCurrentUser()
                    else
                        getUserProfile(event.name)
                }
            }

            is ProfileDetailFragmentEvent.LoadUserPosts ->{
                getCurrentUserPosts()
            }

            is ProfileDetailFragmentEvent.Follow -> {
                followOtherUser(event.user)
            }

            is ProfileDetailFragmentEvent.Unfollow ->{
                unfollowOtherUser(event.user)
            }

            is ProfileDetailFragmentEvent.Idle -> {
                _profileDetailState.value = ProfileDetailState.Idle
            }
        }
    }

    private fun getCurrentUser() {
        _profileDetailState.value = ProfileDetailState.LoadingUser
        viewModelScope.launch {
            firebaseRepository.getCurrentUser()
                .collect {resultData->
                    when(resultData){
                        is ResultData.Success -> {
                            Log.i("ProfileDetailViewModel", "Setting user!!")
                            _profileDetailState.value = ProfileDetailState.SetUserProfile(resultData.data!!)
                        }
                    }
                }
        }
    }

    private fun getCurrentUserPosts(){
        _profileDetailState.value = ProfileDetailState.LoadingPosts

        viewModelScope.launch {
            firebaseRepository.retrieveProfileUserPosts(false)
                .collect {resultData->
                    when(resultData){
                        is ResultData.Success -> {
                            _profileDetailState.value = ProfileDetailState.SetUserPosts(resultData.data!!)
                        }
                    }
                }
        }
    }

    private fun checkIfUserIsCurrentUser(username: String): Boolean{
        var isUserCurrentUser = false

        viewModelScope.launch {
            firebaseRepository.getCurrentUser()
                .collect {resultData ->
                    when(resultData) {
                        is ResultData.Success -> {
                            if(username == resultData.data?.username)
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

    private fun getUserProfile(username: String){
        _profileDetailState.value = ProfileDetailState.LoadingUser

        viewModelScope.launch {
            firebaseRepository.getUserProfile(username)
                .collect {resultData ->
                    when(resultData){
                        is ResultData.Success ->{
                            val currentUserFollowsOtherUser = currentUserFollowsOtherUser(username)
                            _profileDetailState.value = ProfileDetailState.SetOtherUserProfile(resultData.data!!, currentUserFollowsOtherUser)
                        }

                        is ResultData.Error -> {
                            _profileDetailState.value = ProfileDetailState.Error(resultData.exception)
                        }
                    }
                }
        }
    }

    private suspend fun currentUserFollowsOtherUser(otherUsername: String): Boolean{
        var currentUserFollowsOtherUser = false

            firebaseRepository.currentUserFollowsOtherUser(otherUsername)
                .collect { resultData ->
                    when (resultData){
                        is ResultData.Success ->{
                            currentUserFollowsOtherUser = resultData.data!!
                        }

                        is ResultData.Error -> currentUserFollowsOtherUser = false
                    }
                }

        return currentUserFollowsOtherUser
    }

    private fun followOtherUser(user: User){
        viewModelScope.launch {
            firebaseRepository.followOtherUser(user)
                .collect {resultData ->
                    when(resultData){
                        is ResultData.Success -> {
                            if(resultData.data!!)
                                _profileDetailState.value = ProfileDetailState.Followed
                            else
                                _profileDetailState.value = ProfileDetailState.Error(Exception("Sorry, we couldn't follow the user, try again later"))
                        }
                        is ResultData.Error -> {
                            _profileDetailState.value = ProfileDetailState.Error(resultData.exception)
                        }
                    }
                }
        }
    }

    private fun unfollowOtherUser(user: User){
        viewModelScope.launch {
            firebaseRepository.unfollowOtherUser(user)
                .collect {resultData ->
                    when(resultData){
                        is ResultData.Success -> {
                            if(resultData.data!!)
                                _profileDetailState.value = ProfileDetailState.Unfollowed
                            else
                                _profileDetailState.value = ProfileDetailState.Error(Exception("Sorry we couldn't unfollow the user, try again later"))
                        }
                        is ResultData.Error -> {
                            _profileDetailState.value = ProfileDetailState.Error(resultData.exception)
                        }
                    }
                }
        }
    }
}