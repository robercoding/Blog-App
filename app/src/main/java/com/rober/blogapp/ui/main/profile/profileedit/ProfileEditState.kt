package com.rober.blogapp.ui.main.profile.profileedit

import com.rober.blogapp.entity.User

sealed class ProfileEditState {
    object LoadingUser: ProfileEditState()

    data class LoadUser(val user: User) : ProfileEditState()

    object NavigateToProfileDetail : ProfileEditState()
}