package com.rober.blogapp.ui.main.profile

import android.util.Log
import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import com.rober.blogapp.data.ResultData
import com.rober.blogapp.data.network.repository.FirebaseRepository
import com.rober.blogapp.entity.User
import com.rober.blogapp.util.state.DataState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch


class ProfileViewModel
 @ViewModelInject constructor(
     private val firebaseRepository: FirebaseRepository,
     @Assisted savedStateHandle: SavedStateHandle

 ) : ViewModel(){

    private var _profileState : MutableLiveData<DataState<User>> = MutableLiveData()

    val profileState: LiveData<DataState<User>>
        get() = _profileState

    fun getCurrentUser() {
        viewModelScope.launch {
             firebaseRepository.getCurrentUser()
                 .collect {resultData->
                     when(resultData){
                         is ResultData.Success -> {
                             if(resultData.data != null)
                                _profileState.value = DataState.Success(resultData.data)
                         }
                     }
                 }
        }
    }

    fun setIntention(event: ProfileFragmentEvent){
        when(event){
            is ProfileFragmentEvent.loadUserDetails -> {
                if(event.name.isNullOrBlank()){
                    getCurrentUser()

                }

            }
        }
    }
}