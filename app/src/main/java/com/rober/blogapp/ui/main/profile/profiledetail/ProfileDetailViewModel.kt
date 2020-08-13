package com.rober.blogapp.ui.main.profile.profiledetail

import android.util.Log
import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import com.rober.blogapp.data.ResultData
import com.rober.blogapp.data.network.repository.FirebaseRepository
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch


class ProfileDetailViewModel
 @ViewModelInject constructor(
     private val firebaseRepository: FirebaseRepository,
     @Assisted savedStateHandle: SavedStateHandle

 ) : ViewModel(){

    private var _profileDetailState : MutableLiveData<ProfileDetailState> = MutableLiveData()

    val profileDetailState: LiveData<ProfileDetailState>
        get() = _profileDetailState

    fun setIntention(event: ProfileDetailFragmentEvent){
        when(event){
            is ProfileDetailFragmentEvent.loadUserDetails -> {
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

            is ProfileDetailFragmentEvent.loadUserPosts ->{
                getCurrentUserPosts()
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
                            _profileDetailState.value = ProfileDetailState.SetOtherUserProfile(resultData.data!!)
                        }
                    }
                }
        }
    }
}