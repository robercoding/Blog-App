package com.rober.blogapp.ui.main.profile.profileedit

import android.net.Uri
import android.util.Log
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.viewModelScope
import com.rober.blogapp.data.ResultData
import com.rober.blogapp.data.network.repository.FirebaseRepository
import com.rober.blogapp.entity.User
import com.rober.blogapp.ui.base.BaseViewModel
import com.rober.blogapp.ui.main.profile.profileedit.util.IntentImageCodes
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.threeten.bp.Instant

class ProfileEditViewModel @ViewModelInject constructor(
    private val firebaseRepository: FirebaseRepository
) : BaseViewModel<ProfileEditState, ProfileEditFragmentEvent>() {

    private lateinit var user: User
    private var profileImageUri: Uri? = null
    private var backgroundImageUri: Uri? = null

    private var profileImageDownloadUrl: String = ""
    private var backgroundImageDownloadUrl: String = ""

    private var successSaveUserDetails = false

    private var messageError = ""

    override fun setIntention(event: ProfileEditFragmentEvent) {
        when (event) {
            is ProfileEditFragmentEvent.NavigateToProfileDetail -> {
                viewState = ProfileEditState.NavigateToProfileDetail
            }

            is ProfileEditFragmentEvent.LoadingUser -> viewState = ProfileEditState.LoadingUser

            is ProfileEditFragmentEvent.LoadUser -> {
                user = event.user
                viewState = ProfileEditState.LoadUser(user)
            }

            is ProfileEditFragmentEvent.CheckIfUsernameAvailable -> checkIfUsernameIsAvailable(event.username)

            is ProfileEditFragmentEvent.GetImageFromGalleryForProfile -> viewState =
                ProfileEditState.GetImageFromGallery(IntentImageCodes.PROFILE_IMAGE_CODE)

            is ProfileEditFragmentEvent.GetImageFromGalleryForBackground -> viewState =
                ProfileEditState.GetImageFromGallery(IntentImageCodes.BACKGROUND_IMAGE_CODE)

            is ProfileEditFragmentEvent.SaveUriAndPreviewNewImage -> {
                Log.i(TAG, "Save Uri = ${event.uri} and IntentImageCode = ${event.IntentImageCode}")
                when (event.IntentImageCode) {
                    IntentImageCodes.PROFILE_IMAGE_CODE -> profileImageUri = event.uri
                    IntentImageCodes.BACKGROUND_IMAGE_CODE -> backgroundImageUri = event.uri
                }
                Log.i(TAG, "Background now = ${backgroundImageUri}")
                viewState = ProfileEditState.PreviewImage(event.uri)
            }

            is ProfileEditFragmentEvent.ValidateChanges -> {
                if (event.username == user.username && event.biography == user.biography && event.location == user.location && profileImageUri == null && backgroundImageUri == null) {
                    viewState = ProfileEditState.NavigateToProfileDetail
                } else
                    viewState = ProfileEditState.ValidateChanges
            }

            is ProfileEditFragmentEvent.NotifyErrorValidate -> {
                viewState = ProfileEditState.NotifyErrorValidate(
                    event.isUsernameAvailable,
                    event.isUsernameLengthOk,
                    event.isBiographyOk,
                    event.isLocationOk
                )
            }

            is ProfileEditFragmentEvent.SaveChanges -> {
                viewState = ProfileEditState.SavingChanges

                viewModelScope.launch {
                    val success = saveChanges(event.username, event.biography, event.location)
                    if (success)
                        viewState = ProfileEditState.SuccessSave
                    else
                        viewState = ProfileEditState.ErrorSave(messageError)
                }
            }
        }
    }

    private fun checkIfUsernameIsAvailable(username: String) {

        if (user.username != username) {
            job?.run {
                cancel()
            }

            job = viewModelScope.launch {
                firebaseRepository.checkIfUsernameAvailable(username)
                    .collect { resultData ->
                        when (resultData) {
                            is ResultData.Success -> {
                                viewState = ProfileEditState.NotifyUsernameAvailable(resultData.data!!)
                            }
                            is ResultData.Error -> viewState = ProfileEditState.Idle
                        }
                    }
            }
        } else {
            viewState = ProfileEditState.Idle
        }
    }

    private suspend fun saveChanges(username: String, biography: String, location: String): Boolean {

        if (!saveImages()) {
            return false
        }
        if (!saveUserDetailChanges(username, biography, location)) {
            return false
        }

        return true
    }

    private suspend fun saveUserDetailChanges(
        username: String,
        biography: String,
        location: String
    ): Boolean {

        if (profileImageDownloadUrl.isEmpty())
            profileImageDownloadUrl = user.profileImageUrl

        if (backgroundImageDownloadUrl.isEmpty())
            backgroundImageDownloadUrl = user.backgroundImageUrl

        val lastDateUsernameChange = Instant.now().epochSecond
        val newUser = user.copy().apply {
            this.username = username
            this.biography = biography
            this.location = location
            this.profileImageUrl = profileImageDownloadUrl
            this.backgroundImageUrl = backgroundImageDownloadUrl
            this.lastDateUsernameChange = lastDateUsernameChange
        }

        job = viewModelScope.launch {
            firebaseRepository.updateUser(user, newUser)
                .collect { resultData ->
                    when (resultData) {
                        is ResultData.Success -> {
                            successSaveUserDetails = true
                        }
                        is ResultData.Error -> {
                            resultData.exception.message?.also { message ->
                                messageError = message
                            }
                            successSaveUserDetails = false
                        }
                    }
                }
        }

        job?.join()
        return successSaveUserDetails
    }

    private suspend fun saveImages(): Boolean {
        profileImageUri?.let { profileImageUri ->
            val successProfileImage = saveImageProfile(profileImageUri)
            if (!successProfileImage)
                return false
        }

        backgroundImageUri?.let { backgroundImageUri ->
            val successBackgroundImage = saveImageBackground(backgroundImageUri)
            if (!successBackgroundImage)
                return false
        }

        return true
    }

    private suspend fun saveImageProfile(profileImageUri: Uri): Boolean {
        job = viewModelScope.launch {
            firebaseRepository.saveImage(profileImageUri, IntentImageCodes.PROFILE_IMAGE_CODE)
                .collect { resultData ->
                    when (resultData) {
                        is ResultData.Success -> {
                            profileImageDownloadUrl = resultData.data!!
                        }
                        is ResultData.Error -> {
                            profileImageDownloadUrl = ""
                        }
                    }
                }
        }
        job?.join()
        val success = profileImageDownloadUrl.isNotEmpty()
        return success
    }

    private suspend fun saveImageBackground(backgroundImageUri: Uri): Boolean {

        viewModelScope.launch {
            firebaseRepository.saveImage(backgroundImageUri, IntentImageCodes.BACKGROUND_IMAGE_CODE)
                .collect { resultData ->
                    when (resultData) {
                        is ResultData.Success -> {
                            backgroundImageDownloadUrl = resultData.data!!
                        }
                        is ResultData.Error -> {
                            backgroundImageDownloadUrl = ""
                        }
                    }
                }
        }
        job?.join()
        val success = backgroundImageDownloadUrl.isNotEmpty()
        return success
    }
}