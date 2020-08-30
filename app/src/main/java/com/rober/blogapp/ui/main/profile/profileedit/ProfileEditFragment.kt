package com.rober.blogapp.ui.main.profile.profileedit

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.rober.blogapp.R
import com.rober.blogapp.entity.User
import com.rober.blogapp.ui.main.profile.profiledetail.ProfileDetailFragmentEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_profile_edit.*


@AndroidEntryPoint
class ProfileEditFragment : Fragment() {

    private val profileEditViewModel : ProfileEditViewModel by viewModels()

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

    private fun getArgs(){
        val userArg = arguments?.get("user") as User?

        userArg?.let {user ->
            profileEditViewModel.setIntention(ProfileEditFragmentEvent.LoadUser(user))
        }?: kotlin.run {
            profileEditViewModel.setIntention(ProfileEditFragmentEvent.NavigateToProfileDetail)
        }
    }

    //Create 2 fujnctions = Observe the changes and render
    private fun observeViewModel(){
        profileEditViewModel.profileEditState.observe(viewLifecycleOwner, Observer {profileEditState ->
            render(profileEditState)
        })
    }

    private fun render(profileEditState: ProfileEditState){
        when(profileEditState){
            is ProfileEditState.LoadUser -> {
                setUserDetailsView(profileEditState.user)
                loadingUserView(false)
            }

            is ProfileEditState.NavigateToProfileDetail -> {
                navigateToProfileDetail()
            }

            is ProfileEditState.LoadingUser -> {
                loadingUserView(true)
            }
        }
    }

    private fun setUserDetailsView(user: User){
        profile_edit_username.setText(user.username)
        profile_edit_biography.setText(user.biography)
        profile_edit_location.setText(user.location)
        Glide.with(requireView())
            .load("https://firebasestorage.googleapis.com/v0/b/blog-app-d5912.appspot.com/o/users_profile_picture%2Fmew_small_1024_x_1024.jpg?alt=media&token=21dfa28c-2416-49c3-81e1-2475aaf25150")
            .into(profile_edit_image_profile)
    }

    private fun loadingUserView(display: Boolean){
        if(display){
            Log.i("ProgressBar", "GONE")
            displayProgressBar(display)
            profile_edit_layout_user_details.visibility = View.GONE
        }else{
            Log.i("ProgressBar", "VISIBLE")
            displayProgressBar(display)
            profile_edit_layout_user_details.visibility = View.VISIBLE
        }
    }

    private fun displayProgressBar(display: Boolean){
        if(display) profile_edit_progress_bar.visibility = View.VISIBLE else profile_edit_progress_bar.visibility = View.GONE
    }

    private fun navigateToProfileDetail(){
        val navController = findNavController()
        navController.navigate(R.id.profileDetailFragment)
    }

    private fun setupListeners(){
        profile_edit_material_toolbar.setNavigationOnClickListener {
            navigateToProfileDetail()
        }
    }
}

sealed class ProfileEditFragmentEvent{

    data class LoadUser(val user: User) : ProfileEditFragmentEvent()
    data class CheckIfUsernameAvailable(val username: String) : ProfileEditFragmentEvent()

    object NavigateToProfileDetail : ProfileEditFragmentEvent()

    object LoadingUser : ProfileEditFragmentEvent()
}