package com.rober.blogapp.ui.main.profile.profileedit

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.textfield.TextInputLayout
import com.rober.blogapp.R
import com.rober.blogapp.entity.User
import com.rober.blogapp.ui.main.profile.profileedit.util.IntentImageCodes
import com.rober.blogapp.util.ColorUtils
import com.theartofdev.edmodo.cropper.CropImage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_profile_edit.*


@AndroidEntryPoint
class ProfileEditFragment : Fragment() {
    private val TAG = "ProfileEditFragment"

    private val profileEditViewModel: ProfileEditViewModel by viewModels()

    private var INTENT_IMAGE_CODE = 0
    private var usernameAvailable = true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile_edit, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        observeViewModel()
        profileEditViewModel.setIntention(ProfileEditFragmentEvent.LoadingUser)

        getArgs()
    }

    private fun getArgs() {
        val userArg = arguments?.get("user") as User?

        userArg?.let { user ->
            profileEditViewModel.setIntention(ProfileEditFragmentEvent.LoadUser(user))
        } ?: kotlin.run {
            profileEditViewModel.setIntention(ProfileEditFragmentEvent.NavigateToProfileDetail)
        }
    }

    private fun observeViewModel() {
        profileEditViewModel.profileEditState.observe(viewLifecycleOwner, Observer { profileEditState ->
            render(profileEditState)
        })
    }

    private fun render(profileEditState: ProfileEditState) {
        when (profileEditState) {
            is ProfileEditState.LoadUser -> {
                setUserDetailsView(profileEditState.user)
                loadingUserView(false)
            }

            is ProfileEditState.NotifyUsernameAvailable -> isUsernameAvailable(profileEditState.isUsernameAvailable)

            is ProfileEditState.NavigateToProfileDetail -> navigateToProfileDetail()

            is ProfileEditState.LoadingUser -> loadingUserView(true)

            is ProfileEditState.GetImageFromGallery -> {
                INTENT_IMAGE_CODE = profileEditState.INTENT_IMAGE_CODE
                getImageFromGallery()
            }

            is ProfileEditState.PreviewImage -> setPreviewImage(profileEditState.uri)

            is ProfileEditState.ValidateChanges -> {
                val saveChanges = validateChanges()
                Log.i("SaveChanges", "SaveChanges = ${saveChanges}")
                if (saveChanges) {
                    saveActualChanges()
                }
            }

            is ProfileEditState.NotifyErrorValidate -> {
                setErrors(
                    profileEditState.isUsernameAvailable,
                    profileEditState.isUsernameLengthOk,
                    profileEditState.isBiographyOk,
                    profileEditState.isLocationOk
                )
            }

            is ProfileEditState.SavingChanges -> {
                displayProgressBarSaveChanges(true)
            }

            is ProfileEditState.ErrorSave -> {
                displayProgressBarSaveChanges(false)
                profileEditState.messageError?.also { messageError ->
                    Toast.makeText(requireContext(), messageError, Toast.LENGTH_SHORT).show()
                } ?: kotlin.run {
                    Toast.makeText(
                        requireContext(),
                        "Sorry there was an error when trying to save the new information.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            is ProfileEditState.SuccessSave -> {
                Toast.makeText(requireContext(), "Successfully updated!", Toast.LENGTH_SHORT).show()
                navigateToProfileDetail()
            }

            is ProfileEditState.Idle -> profile_text_layout_username.isEndIconVisible = false
        }
    }

    private fun setUserDetailsView(user: User) {
        Log.i(TAG, "Load")
        profile_text_edit_username.setText(user.username)

        profile_text_edit_biography.setText(user.biography)
        profile_text_edit_location.setText(user.location)

        val profileImage: Any = if (user.profileImageUrl.isEmpty())
            R.drawable.user_profile_png
        else
            user.profileImageUrl

        Glide.with(requireView())
            .load(profileImage)
            .into(profile_edit_image_profile)

        val backgroundImage: Any = if (user.backgroundImageUrl.isEmpty())
            R.drawable.blue_screen
        else
            user.backgroundImageUrl

        Glide.with(requireView())
            .load(backgroundImage)
            .fitCenter()
            .into(profile_edit_image_background)
    }

    private fun loadingUserView(display: Boolean) {
        if (display) {
            displayProgressBar(display)
            profile_edit_layout_user_details.visibility = View.GONE
        } else {
            displayProgressBar(display)
            profile_edit_layout_user_details.visibility = View.VISIBLE
        }
    }

    private fun isUsernameAvailable(isUsernameAvailable: Boolean) {
        if (isUsernameAvailable) {
            profile_text_layout_username.isEndIconVisible = true
            profile_text_layout_username.isErrorEnabled = false
            profile_text_layout_username.isHelperTextEnabled = false

            val drawable = resources.getDrawable(R.drawable.ic_tick_mark_circle, null)
            context?.run {
                profile_text_layout_username.setEndIconTintList(ColorUtils(this).profileEditColorStateListGreen)
            }

            profile_text_layout_username.endIconDrawable = drawable
            profile_text_layout_username.boxStrokeColor = ContextCompat.getColor(requireContext(), R.color.blueTwitter)

            usernameAvailable = true
        } else {
            setErrorInEditText(profile_text_layout_username, "Sorry, username is not available")
            usernameAvailable = false
        }
    }

    private fun setErrorInEditText(textInputLayout: TextInputLayout, message: String = "") {

        if (!textInputLayout.isErrorEnabled) {
            textInputLayout.isEndIconVisible = true
            textInputLayout.isErrorEnabled = true
            textInputLayout.isHelperTextEnabled = false

            val drawable = resources.getDrawable(R.drawable.ic_error_outline_24px, null)

            context?.run {
                textInputLayout.setHelperTextColor(ColorUtils(this).profileEditColorStateListRed)
                textInputLayout.setEndIconTintList(ColorUtils(this).profileEditColorStateListRed)
            }

            textInputLayout.helperText = message
            textInputLayout.endIconDrawable = drawable
            textInputLayout.boxStrokeColor = ContextCompat.getColor(requireContext(), R.color.red)
        }
    }

    private fun displayProgressBar(display: Boolean) {
        if (display) profile_edit_progress_bar_loading_user_details.visibility =
            View.VISIBLE else profile_edit_progress_bar_loading_user_details.visibility =
            View.GONE
    }

    private fun navigateToProfileDetail() {
        val navController = findNavController()
        navController.navigate(R.id.action_profileEditFragment_to_profileDetailFragment)
    }

    private fun getImageFromGallery() {
        context?.let {
            when (INTENT_IMAGE_CODE) {
                IntentImageCodes.PROFILE_IMAGE_CODE -> {
                    CropImage.activity()
                        .setFixAspectRatio(true)
                        .setAspectRatio(1, 1)
                        .start(it, this)
                }
                IntentImageCodes.BACKGROUND_IMAGE_CODE -> {
                    CropImage.activity()
                        .setFixAspectRatio(true)
                        .setAspectRatio(3, 1)
                        .start(it, this)
                }
            }
        }

    }

    private fun saveActualChanges() {
        displayProgressBarSaveChanges(true)
        val username = profile_text_edit_username.text.toString().replace("\\s".toRegex(), "") //remove whitespaces
        val biography = profile_text_edit_biography.text.toString()
        val location = profile_text_edit_location.text.toString()

        profileEditViewModel.setIntention(ProfileEditFragmentEvent.SaveChanges(username, biography, location))
    }

    private fun validateChanges(): Boolean {
        val usernameText = profile_text_edit_username.text.toString()
        val biographyText = profile_text_edit_biography.text.toString()
        val locationText = profile_text_edit_location.text.toString()

        val notifyErrorEvent = ProfileEditFragmentEvent.NotifyErrorValidate()
        if (biographyText.length <= 150) {
            notifyErrorEvent.isBiographyOk = true
        }
        if (locationText.length <= 25) {
            notifyErrorEvent.isLocationOk = true
        }
        if (usernameAvailable) {
            notifyErrorEvent.isUsernameAvailable = usernameAvailable
        }
        if (usernameText.isNotEmpty() && usernameText.length < 15) {
            notifyErrorEvent.isUsernameLengthOk = true
        }

        return if (!notifyErrorEvent.isBiographyOk || !notifyErrorEvent.isLocationOk || !notifyErrorEvent.isUsernameAvailable || !notifyErrorEvent.isUsernameLengthOk) {
            profileEditViewModel.setIntention(notifyErrorEvent)
            false
        } else {
            true
        }
    }

    private fun setErrors(
        isUsernameAvailable: Boolean,
        isUsernameLengthOk: Boolean,
        isBiographyOk: Boolean,
        isLocationOk: Boolean
    ) {
        if (isUsernameLengthOk) {
            if (!isUsernameAvailable) {
                setErrorInEditText(profile_text_layout_username, "Sorry, username is not available")
            }
        } else {
            setErrorInEditText(
                profile_text_layout_username,
                "Your username must be 15 characters or less and contain only letter, numbers, underscores and no spaces"
            )
        }

        if (!isBiographyOk) {
            setErrorInEditText(profile_text_layout_biography, "Biography can't be more than 150 characters ")
        }
        if (!isLocationOk) {
            setErrorInEditText(profile_text_layout_location, "Location can't be more than 25 characters ")
        }
    }

    private fun saveUri(uri: Uri) {
        profileEditViewModel.setIntention(ProfileEditFragmentEvent.SaveUriAndPreviewNewImage(uri, INTENT_IMAGE_CODE))
    }

    private fun setPreviewImage(uri: Uri) {
        when (INTENT_IMAGE_CODE) {
            IntentImageCodes.PROFILE_IMAGE_CODE -> profile_edit_image_profile.setImageURI(uri)

            IntentImageCodes.BACKGROUND_IMAGE_CODE -> Glide.with(requireView()).load(uri).into(profile_edit_image_background)
        }
    }

    private fun displayProgressBarSaveChanges(display: Boolean) {
        if (display) profile_edit_progress_bar_save_changes.visibility =
            View.VISIBLE else profile_edit_progress_bar_save_changes.visibility = View.GONE
    }

    private fun setupListeners() {
        profile_text_edit_username.addTextChangedListener {
            val usernameText = profile_text_edit_username.text.toString()
            if (usernameText.length < 5 || usernameText.length > 15) {
                profileEditViewModel.setIntention(ProfileEditFragmentEvent.NotifyErrorValidate(false, false, true, true))
            } else {
                profileEditViewModel.setIntention(
                    ProfileEditFragmentEvent.CheckIfUsernameAvailable(
                        profile_text_edit_username.text.toString()
                    )
                )
            }
        }

        profile_edit_material_toolbar.setNavigationOnClickListener {
            profileEditViewModel.setIntention(ProfileEditFragmentEvent.NavigateToProfileDetail)
        }

        profile_edit_image_profile_add.setOnClickListener {
            profileEditViewModel.setIntention(ProfileEditFragmentEvent.GetImageFromGalleryForProfile)
        }

        profile_edit_image_background_add.setOnClickListener {
            profileEditViewModel.setIntention(ProfileEditFragmentEvent.GetImageFromGalleryForBackground)
        }

        profile_edit_material_toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.toolbar_profile_edit_save -> {
                    profileEditViewModel.setIntention(
                        ProfileEditFragmentEvent.ValidateChanges(
                            profile_text_edit_username.text.toString(),
                            profile_text_edit_biography.text.toString(),
                            profile_text_edit_location.text.toString()
                        )
                    )
                    true
                }
                else -> true
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            val result = CropImage.getActivityResult(data)
            if (resultCode == RESULT_OK) {
                saveUri(result.uri)
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Log.i(TAG, "${result.error.message}")
            }
        }
    }
}

sealed class ProfileEditFragmentEvent {

    data class LoadUser(val user: User) : ProfileEditFragmentEvent()
    data class CheckIfUsernameAvailable(val username: String) : ProfileEditFragmentEvent()
    data class NotifyErrorValidate(
        var isUsernameAvailable: Boolean = false,
        var isUsernameLengthOk: Boolean = false,
        var isBiographyOk: Boolean = false,
        var isLocationOk: Boolean = false
    ) : ProfileEditFragmentEvent()

    object NavigateToProfileDetail : ProfileEditFragmentEvent()

    object GetImageFromGalleryForProfile : ProfileEditFragmentEvent()
    object GetImageFromGalleryForBackground : ProfileEditFragmentEvent()

    data class SaveUriAndPreviewNewImage(val uri: Uri, val IntentImageCode: Int) : ProfileEditFragmentEvent()

    data class ValidateChanges(val username: String, val biography: String, val location: String) :
        ProfileEditFragmentEvent()

    data class SaveChanges(val username: String, val biography: String, val location: String) : ProfileEditFragmentEvent()

    object LoadingUser : ProfileEditFragmentEvent()
}