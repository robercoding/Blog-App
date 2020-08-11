package com.rober.blogapp.ui.main.profile.profiledetail

import android.util.Log
import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import com.rober.blogapp.data.ResultData
import com.rober.blogapp.data.network.repository.FirebaseRepository
import com.rober.blogapp.entity.Post
import com.rober.blogapp.entity.User
import com.rober.blogapp.util.state.DataState
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch


class ProfileDetailViewModel
 @ViewModelInject constructor(
     private val firebaseRepository: FirebaseRepository,
     @Assisted savedStateHandle: SavedStateHandle

 ) : ViewModel(){

    private var _profileUserState : MutableLiveData<DataState<User>> = MutableLiveData()

    val profileUserState: LiveData<DataState<User>>
        get() = _profileUserState

    private var _profileUserListState: MutableLiveData<DataState<List<Post>>> = MutableLiveData()

    val profileUserListState : LiveData<DataState<List<Post>>>
        get() = _profileUserListState



    fun setIntention(event: ProfileFragmentEvent){
        when(event){
            is ProfileFragmentEvent.loadUserDetails -> {
                if(event.name.isNullOrBlank()){
                    getCurrentUser()
                    getCurrentUserPosts()
                }else{
                    viewModelScope.launch {
                        firebaseRepository.getUserProfile(event.name)
                            .collect {resultData ->
                                when(resultData){
                                    is ResultData.Success ->{
                                        _profileUserState.value = DataState.Success(resultData.data!!)

                                    }
                                }
                            }
                    }

                }
            }
        }
    }

    private fun getCurrentUser() {
        viewModelScope.launch {
            firebaseRepository.getCurrentUser()
                .collect {resultData->
                    when(resultData){
                        is ResultData.Success -> {
                            if(resultData.data != null)
                                _profileUserState.value = DataState.Success(resultData.data)
                        }
                    }
                }
        }
    }

    private fun getCurrentUserPosts(){
        viewModelScope.launch {
            firebaseRepository.retrieveProfileUserPosts(false)
                .collect {resultData->
                    when(resultData){
                        is ResultData.Success -> {
                            if(resultData.data != null)
                                _profileUserListState.value = DataState.Success(resultData.data)
                            Log.i("ProfileViewModel", "${resultData.data}")
                        }
                    }
                }
        }
    }
}