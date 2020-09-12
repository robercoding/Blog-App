package com.rober.blogapp.ui.main.profile.profileedit

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rober.blogapp.data.ResultData
import com.rober.blogapp.data.network.repository.FirebaseRepository
import com.rober.blogapp.entity.User
import com.rober.blogapp.ui.main.profile.profileedit.util.IntentImageCodes
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.threeten.bp.Instant

class ProfileEditViewModel @ViewModelInject constructor(
    private val firebaseRepository: FirebaseRepository
) : ViewModel() {
    private val TAG = "ProfileEditViewModel"

    private var _profileEditState: MutableLiveData<ProfileEditState> = MutableLiveData()

    val profileEditState: LiveData<ProfileEditState>
        get() = _profileEditState

    private lateinit var user: User
    private var profileImageUri: Uri? = null
    private var backgroundImageUri: Uri? = null

    private var profileImageDownloadUrl: String = ""
    private var backgroundImageDownloadUrl: String = ""

    private var successSaveUserDetails = false
    private var job: Job? = null

    private var messageError = ""

    fun setIntention(event: ProfileEditFragmentEvent) {
        when (event) {
            is ProfileEditFragmentEvent.NavigateToProfileDetail -> {
                _profileEditState.value = ProfileEditState.NavigateToProfileDetail
            }

            is ProfileEditFragmentEvent.LoadingUser -> _profileEditState.value = ProfileEditState.LoadingUser

            is ProfileEditFragmentEvent.LoadUser -> {
                user = event.user
                _profileEditState.value = ProfileEditState.LoadUser(user)
            }

            is ProfileEditFragmentEvent.CheckIfUsernameAvailable -> checkIfUsernameIsAvailable(event.username)

            is ProfileEditFragmentEvent.GetImageFromGalleryForProfile -> _profileEditState.value =
                ProfileEditState.GetImageFromGallery(IntentImageCodes.PROFILE_IMAGE_CODE)

            is ProfileEditFragmentEvent.GetImageFromGalleryForBackground -> _profileEditState.value =
                ProfileEditState.GetImageFromGallery(IntentImageCodes.BACKGROUND_IMAGE_CODE)

            is ProfileEditFragmentEvent.SaveUriAndPreviewNewImage -> {
                Log.i(TAG, "Save Uri = ${event.uri} and IntentImageCode = ${event.IntentImageCode}")
                when (event.IntentImageCode) {
                    IntentImageCodes.PROFILE_IMAGE_CODE -> profileImageUri = event.uri
                    IntentImageCodes.BACKGROUND_IMAGE_CODE -> backgroundImageUri = event.uri
                }
                Log.i(TAG, "Background now = ${backgroundImageUri}")
                _profileEditState.value = ProfileEditState.PreviewImage(event.uri)
            }

            is ProfileEditFragmentEvent.ValidateChanges -> {
                if (event.username == user.username && event.biography == user.biography && event.location == user.location && profileImageUri == null && backgroundImageUri == null) {
                    _profileEditState.value = ProfileEditState.NavigateToProfileDetail
                } else
                    _profileEditState.value = ProfileEditState.ValidateChanges
            }

            is ProfileEditFragmentEvent.NotifyErrorValidate -> {
                _profileEditState.value = ProfileEditState.NotifyErrorValidate(
                    event.isUsernameAvailable,
                    event.isUsernameLengthOk,
                    event.isBiographyOk,
                    event.isLocationOk
                )
            }

            is ProfileEditFragmentEvent.SaveChanges -> {
                _profileEditState.value = ProfileEditState.SavingChanges
                Log.i("NextTimeChangeUsername", "wtf lets save")

                viewModelScope.launch {
                    val success = saveChanges(event.username, event.biography, event.location)
                    if (success)
                        _profileEditState.value = ProfileEditState.SuccessSave
                    else
                        _profileEditState.value = ProfileEditState.ErrorSave(messageError)
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
                                _profileEditState.value = ProfileEditState.NotifyUsernameAvailable(resultData.data!!)
                                Log.i("NotifyUsername", "$username available = ${resultData.data}")
                            }
                            is ResultData.Error -> _profileEditState.value = ProfileEditState.Idle
                        }
                    }
            }
        } else {
            _profileEditState.value = ProfileEditState.Idle
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

        val job = viewModelScope.launch {
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

        job.join()
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
        val job = viewModelScope.launch {
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
        job.join()
        val success = profileImageDownloadUrl.isNotEmpty()
        return success
    }

    private suspend fun saveImageBackground(backgroundImageUri: Uri): Boolean {

        val job = viewModelScope.launch {
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
        job.join()
        val success = backgroundImageDownloadUrl.isNotEmpty()
        return success
    }
}