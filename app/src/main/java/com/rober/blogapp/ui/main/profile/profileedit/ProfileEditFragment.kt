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
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.rober.blogapp.R
import com.rober.blogapp.entity.User
import com.rober.blogapp.ui.main.profile.profileedit.util.IntentImageCodes
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
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

    //Create 2 fujnctions = Observe the changes and render
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

            is ProfileEditState.NavigateToProfileDetail -> navigateToProfileDetail()

            is ProfileEditState.LoadingUser -> loadingUserView(true)

            is ProfileEditState.GetImageFromGallery -> {
                INTENT_IMAGE_CODE = profileEditState.INTENT_IMAGE_CODE
                getImageFromGallery()
            }

            is ProfileEditState.PreviewImage -> setPreviewImage(profileEditState.uri)

            is ProfileEditState.ValidateChanges -> {
                val saveChanges = validateChanges()
                if(saveChanges){
                    saveActualChanges()
                }
            }

            is ProfileEditState.SavingChanges -> {
                displayProgressBarSaveChanges(true)
            }

            is ProfileEditState.ErrorSave -> {
                displayProgressBarSaveChanges(false)
                Toast.makeText(requireContext(), "There were some errors, try again later", Toast.LENGTH_SHORT).show()
            }
            is ProfileEditState.SuccessSave -> {
                Toast.makeText(requireContext(), "Successfully updated!", Toast.LENGTH_SHORT).show()
                navigateToProfileDetail()
            }
        }
    }

    private fun setUserDetailsView(user: User) {
        profile_edit_username.setText(user.username)
        profile_edit_biography.setText(user.biography)
        profile_edit_location.setText(user.location)
        Glide.with(requireView())
            .load("https://firebasestorage.googleapis.com/v0/b/blog-app-d5912.appspot.com/o/users_profile_picture%2Fmew_small_1024_x_1024.jpg?alt=media&token=21dfa28c-2416-49c3-81e1-2475aaf25150")
            .into(profile_edit_image_profile)

        Glide.with(requireView())
            .load("https://firebasestorage.googleapis.com/v0/b/blog-app-d5912.appspot.com/o/users_profile_picture%2Fflakked.jpg?alt=media&token=41834b34-5d7c-4dad-bd54-0d1e7c7dad29")
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

    private fun displayProgressBar(display: Boolean) {
        if (display) profile_edit_progress_bar_loading_user_details.visibility = View.VISIBLE else profile_edit_progress_bar_loading_user_details.visibility =
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

    private fun setupListeners() {
        profile_edit_material_toolbar.setNavigationOnClickListener {
            profileEditViewModel.setIntention(ProfileEditFragmentEvent.NavigateToProfileDetail)
        }

        profile_edit_image_profile_add.setOnClickListener {
            Toast.makeText(requireContext(), "Clicked on add", Toast.LENGTH_SHORT).show()
            profileEditViewModel.setIntention(ProfileEditFragmentEvent.GetImageFromGalleryForProfile)
        }

        profile_edit_image_background_add.setOnClickListener {
            profileEditViewModel.setIntention(ProfileEditFragmentEvent.GetImageFromGalleryForBackground)
        }

        profile_edit_material_toolbar.setOnMenuItemClickListener {menuItem ->
            when(menuItem.itemId){
                R.id.toolbar_profile_edit_save ->{
                    profileEditViewModel.setIntention(ProfileEditFragmentEvent.ValidateChanges)
                    true
                }
                else -> true
            }
        }
    }

    private fun saveActualChanges(){
        displayProgressBarSaveChanges(true)
        val username = profile_edit_username.text.toString().replace("\\s".toRegex(), "") //remove whitespaces
        val biography = profile_edit_biography.text.toString()
        val location = profile_edit_location.text.toString()

        profileEditViewModel.setIntention(ProfileEditFragmentEvent.SaveChanges(username, biography, location))
    }

    private fun validateChanges(): Boolean{
        if(profile_edit_biography.lineCount >= 4){
            return false
        }
        if(profile_edit_location.text.toString().length > 20){
            return false
        }
        if(!usernameAvailable || profile_edit_username.text.toString().isEmpty()){
            return false
        }
        return true
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

    private fun saveUri(uri: Uri){
        profileEditViewModel.setIntention(ProfileEditFragmentEvent.SaveUriAndPreviewNewImage(uri, INTENT_IMAGE_CODE))
    }

    private fun setPreviewImage(uri: Uri) {
        when (INTENT_IMAGE_CODE) {
            IntentImageCodes.PROFILE_IMAGE_CODE -> profile_edit_image_profile.setImageURI(uri)

            IntentImageCodes.BACKGROUND_IMAGE_CODE -> Glide.with(requireView()).load(uri).into(profile_edit_image_background)
        }
    }

    private fun displayProgressBarSaveChanges(display: Boolean){
        if(display) profile_edit_progress_bar_save_changes.visibility = View.VISIBLE else profile_edit_progress_bar_save_changes.visibility = View.GONE
    }
}

sealed class ProfileEditFragmentEvent {

    data class LoadUser(val user: User) : ProfileEditFragmentEvent()
    data class CheckIfUsernameAvailable(val username: String) : ProfileEditFragmentEvent()

    object NavigateToProfileDetail : ProfileEditFragmentEvent()

    object GetImageFromGalleryForProfile : ProfileEditFragmentEvent()
    object GetImageFromGalleryForBackground : ProfileEditFragmentEvent()

    data class SaveUriAndPreviewNewImage(val uri:Uri, val IntentImageCode: Int): ProfileEditFragmentEvent()

    object ValidateChanges : ProfileEditFragmentEvent()
    data class SaveChanges(val username: String, val biography: String, val location: String): ProfileEditFragmentEvent()

    object LoadingUser : ProfileEditFragmentEvent()
}