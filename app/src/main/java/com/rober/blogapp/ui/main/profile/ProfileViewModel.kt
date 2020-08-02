package com.rober.blogapp.ui.main.profile

import android.util.Log
import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import com.rober.blogapp.data.network.repository.FirebaseRepository
import com.rober.blogapp.entity.User
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class ProfileViewModel
 @ViewModelInject constructor(
     private val firebaseRepository: FirebaseRepository,
     @Assisted savedStateHandle: SavedStateHandle

 ) : ViewModel(){

    var _savedUserProfile : MutableLiveData<User> = MutableLiveData()

    val savedUserProfile : LiveData<User>
        get() = _savedUserProfile

    fun getCurrentUser() {
        viewModelScope.launch {
            //val user = firebaseRepository.getCurrentUser()

        }
    }

    fun login(){
        viewModelScope.launch {
            //firebaseRepository.login()
        }

    }

    fun saveUser(user: User){
        viewModelScope.launch {
            delay(3000)
            //firebaseRepository.saveUser(user)
        }
    }

    fun printSomething(){
        Log.i("ProfileViewModel", "Now works")
    }
}