package com.rober.blogapp.ui.main.profile.profileedit

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.rober.blogapp.entity.User

class ProfileEditViewModel @ViewModelInject constructor(): ViewModel() {

    private var _profileEditState : MutableLiveData<ProfileEditState> = MutableLiveData()

    val profileEditState : LiveData<ProfileEditState>
        get() = _profileEditState

    private lateinit var user: User

    fun setIntention(event: ProfileEditFragmentEvent){
        when(event){
            is ProfileEditFragmentEvent.NavigateToProfileDetail -> _profileEditState.value = ProfileEditState.NavigateToProfileDetail

            is ProfileEditFragmentEvent.LoadingUser -> _profileEditState.value = ProfileEditState.LoadingUser

            is ProfileEditFragmentEvent.LoadUser -> {
                user = event.user
                _profileEditState.value = ProfileEditState.LoadUser(user)
            }
        }
    }

}