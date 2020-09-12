package com.rober.blogapp.ui.main.profile.profileedit

import android.net.Uri
import com.rober.blogapp.entity.User

sealed class ProfileEditState {
    object LoadingUser : ProfileEditState()

    data class LoadUser(val user: User) : ProfileEditState()
    data class NotifyUsernameAvailable(val isUsernameAvailable: Boolean) : ProfileEditState()
    data class NotifyErrorValidate(
        val isUsernameAvailable: Boolean,
        val isUsernameLengthOk: Boolean,
        val isBiographyOk: Boolean,
        val isLocationOk: Boolean
    ) : ProfileEditState()

    data class GetImageFromGallery(val INTENT_IMAGE_CODE: Int) : ProfileEditState()
    data class PreviewImage(val uri: Uri) : ProfileEditState()

    object ValidateChanges : ProfileEditState()
    object SavingChanges : ProfileEditState()

    object NavigateToProfileDetail : ProfileEditState()

    object SuccessSave : ProfileEditState()
    data class ErrorSave(var messageError: String? = null) : ProfileEditState()

    object Idle : ProfileEditState()
}