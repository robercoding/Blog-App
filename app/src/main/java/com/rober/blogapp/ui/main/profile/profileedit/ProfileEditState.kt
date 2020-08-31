package com.rober.blogapp.ui.main.profile.profileedit

import android.net.Uri
import com.rober.blogapp.entity.User

sealed class ProfileEditState {
    object LoadingUser: ProfileEditState()

    data class LoadUser(val user: User) : ProfileEditState()

    data class GetImageFromGallery(val INTENT_IMAGE_CODE: Int) : ProfileEditState()
    data class PreviewImage(val uri: Uri): ProfileEditState()

    object ValidateChanges : ProfileEditState()
    object SavingChanges: ProfileEditState()

    object NavigateToProfileDetail : ProfileEditState()

    object SuccessSave : ProfileEditState()
    object ErrorSave : ProfileEditState()
}