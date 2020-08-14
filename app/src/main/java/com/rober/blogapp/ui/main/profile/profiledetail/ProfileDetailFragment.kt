package com.rober.blogapp.ui.main.profile.profiledetail

import android.graphics.Typeface
import android.graphics.drawable.ShapeDrawable
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.rober.blogapp.R
import com.rober.blogapp.entity.Post
import com.rober.blogapp.entity.User
import com.rober.blogapp.ui.main.feed.adapter.PostAdapter
import com.rober.blogapp.util.RecyclerViewActionInterface
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_profile_detail.*

@AndroidEntryPoint
class ProfileFragment : Fragment(), RecyclerViewActionInterface{
    private val TAG ="ProfileDetailFragment"

    private val profileDetailViewModel: ProfileDetailViewModel by viewModels()
    lateinit var postAdapter: PostAdapter
    private val viewHolder = R.layout.adapter_feed_viewholder_posts

    private var user: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.i(TAG, "ON CREATE VIEW")
        return inflater.inflate(R.layout.fragment_profile_detail, container, false)
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        postAdapter = PostAdapter(requireView(), viewHolder, this)

        setupListeners()
        subscribeObservers()
        getUserArgumentAndSetIntention()
    }

    private fun subscribeObservers(){
        profileDetailViewModel.profileDetailState.observe(viewLifecycleOwner, Observer { profileDetailState ->
            render(profileDetailState)
        })
    }

    private fun getUserArgumentAndSetIntention(){
        val userName = arguments?.getString("user_id")

        if(userName.isNullOrBlank()){
            profileDetailViewModel.setIntention(ProfileDetailFragmentEvent.LoadUserDetails(null))
        }else{
            profileDetailViewModel.setIntention(ProfileDetailFragmentEvent.LoadUserDetails(userName))
        }
    }

    private fun render(profileDetailState: ProfileDetailState){
        Log.i(TAG, "State: ${profileDetailState}")
        when(profileDetailState){
            is ProfileDetailState.SetUserProfile -> {
                showViewMotionLayout(true)
                setViewForCurrentUser()
//                setViewForOtherUser()
//                val visibilityFollow = profile_detail_motion_layout.getConstraintSet(R.id.start).getConstraint(R.id.profile_detail_button_follow).propertySet.visibility
//                val visibilityEdit = profile_detail_motion_layout.getConstraintSet(R.id.start).getConstraint(R.id.profile_detail_button_edit).propertySet.visibility
//                Log.i(TAG, "Visibility after setting Follow: $visibilityFollow")
//                Log.i(TAG, "Visibility after setting Edit: $visibilityEdit")

                val user = profileDetailState.user
                setUserProfile(user)
                profileDetailViewModel.setIntention(ProfileDetailFragmentEvent.LoadUserPosts(null))
            }

            is ProfileDetailState.SetUserPosts -> {
                displayProgressBar(false)

                val listUserPosts = profileDetailState.listUserPosts
                setUserPosts(listUserPosts.toMutableList())
            }

            is ProfileDetailState.SetOtherUserProfile -> {
                user = profileDetailState.user

                displayBottomNavigation(false)
                showViewMotionLayout(true)
                setButtonViewForOtherUser(profileDetailState.currentUserFollowsOtherUser)
                setViewForOtherUser()

                setOtherUserProfile(user!!)
            }

            is ProfileDetailState.SetOtherUserPosts -> {
                displayProgressBar(false)
                setOtherUserPosts(profileDetailState.listOtherUserPosts.toMutableList())
            }

            is ProfileDetailState.LoadingPosts -> {
                //setViewForCurrentUser()
                displayProgressBar(true)
            }

            is ProfileDetailState.LoadingUser -> {
                setViewForCurrentUser()
                showViewMotionLayout(false)
            }

            is ProfileDetailState.Followed -> {
                Toast.makeText(requireContext(), "Now you're following ${user?.username}", Toast.LENGTH_SHORT).show()
                setButtonViewForOtherUser(true)
            }

            is ProfileDetailState.Unfollowed -> {
                Toast.makeText(requireContext(), "You stopped following ${user?.username}", Toast.LENGTH_SHORT).show()
                setButtonViewForOtherUser(false)
            }

            is ProfileDetailState.Error -> {
                Toast.makeText(requireContext(), "${profileDetailState.exception.message}", Toast.LENGTH_SHORT).show()
                backToPreviousFragment()
            }
        }
    }

    private fun showViewMotionLayout(showMotionLayout: Boolean){
        if(showMotionLayout){
            profile_detail_motion_layout.visibility = View.VISIBLE
            profile_detail_background_progress_bar.visibility = View.VISIBLE
        } else{
            profile_detail_background_progress_bar.visibility = View.GONE
            profile_detail_motion_layout.visibility = View.GONE

        }
    }

    private fun setUserProfile(user: User){

        uid_name.text = "@${user.username}"
        uid_biography.text = user.biography
        uid_followers.text = "${user.follower} Follower"
        uid_following.text = "${user.following} Following"

        if(user.follower > 1)
            uid_followers.append("s")
    }

    private fun setViewForCurrentUser(){

        profile_detail_motion_layout.getConstraintSet(R.id.start)?.let {
            it.getConstraint(R.id.profile_detail_button_edit).propertySet.visibility = View.VISIBLE
            it.getConstraint(R.id.profile_detail_button_follow).propertySet.visibility = View.GONE
        }
        profile_detail_motion_layout.getConstraintSet(R.id.end)?.let {
            it.getConstraint(R.id.profile_detail_button_edit).propertySet.visibility = View.VISIBLE
            it.getConstraint(R.id.profile_detail_button_follow).propertySet.visibility = View.GONE
        }
    }

    private fun setOtherUserProfile(user: User){

        uid_name.text = "@${user.username}"
        uid_biography.text = user.biography
        uid_followers.text = "${user.follower} Follower"
        uid_following.text = "${user.following} Following"

        if(user.follower > 1)
            uid_followers.append("s")
    }

    private fun setViewForOtherUser(){

        profile_detail_motion_layout.getConstraintSet(R.id.start)?.let {
            it.getConstraint(R.id.profile_detail_button_edit).propertySet.visibility = View.GONE
            it.getConstraint(R.id.profile_detail_button_follow).propertySet.visibility = View.VISIBLE
        }
        profile_detail_motion_layout.getConstraintSet(R.id.end)?.let {
            it.getConstraint(R.id.profile_detail_button_edit).propertySet.visibility = View.GONE
            it.getConstraint(R.id.profile_detail_button_follow).propertySet.visibility = View.VISIBLE
        }
    }

    private fun setButtonViewForOtherUser(currentUserFollowsOtherUser: Boolean){
        Log.i(TAG, "This should be what currentis $currentUserFollowsOtherUser")
        Log.i(TAG, "This should be whatever ${profile_detail_button_follow.isSelected}")

        if(currentUserFollowsOtherUser){
            profile_detail_button_follow.apply {
                isSelected = true

                setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.blueGray))
                text = "Following"
                setTypeface(null, Typeface.BOLD)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                text= "Following"
            }
        }else{
            profile_detail_button_follow.apply {
                isSelected = false
                text = "Follow"
                setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.background))
                val shapeDrawable = ShapeDrawable()
                shapeDrawable.paint.strokeWidth = 1.0F
                shapeDrawable.paint.color = ContextCompat.getColor(requireContext(), R.color.blueGray)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.blueGray))
            }
        }
    }

    private fun setUserPosts(listUserPosts: MutableList<Post>){
        postAdapter.setPosts(listUserPosts)

        recycler_profile_detail_posts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = postAdapter
        }
    }

    private fun setOtherUserPosts(listOtherUserPosts: MutableList<Post>){
        postAdapter.setPosts(listOtherUserPosts)

        recycler_profile_detail_posts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = postAdapter
        }
    }

    private fun setupListeners(){
        profile_detail_button_follow.setOnClickListener {
            Log.i(TAG, "Enabled: ${it.isEnabled}")

            if(it.isSelected){
                Log.i(TAG, "WE ARE GOING TO UNFOLLOW")
                it.isSelected = false
                user?.let {user ->
                    profileDetailViewModel.setIntention(ProfileDetailFragmentEvent.Unfollow(user))
                }
            }else{
                it.isSelected = true
                user?.let { user ->
                    profileDetailViewModel.setIntention(ProfileDetailFragmentEvent.Follow(user))
                }
            }
        }
    }

    private fun displayProgressBar(isDisplayed: Boolean){
        progress_bar_profile_posts.visibility = if(isDisplayed) View.VISIBLE else View.GONE
    }

    private fun displayBottomNavigation(display: Boolean){
        val navController = activity?.bottom_navigation ?: return
        if(display) navController.visibility = View.VISIBLE else navController.visibility = View.GONE
    }

    private fun backToPreviousFragment(){
        val navController = activity?.bottom_navigation ?: return
        navController.findNavController().popBackStack()
    }


    override fun clickListenerOnPost(positionAdapter: Int) {
        //TODO
    }

    override fun clickListenerOnUser(positionAdapter: Int) {
        //TODO
    }

    override fun loadOldFeedPosts() {
        TODO("Not yet implemented")
    }
}

sealed class ProfileDetailFragmentEvent{
    data class LoadUserDetails(val name: String? = null): ProfileDetailFragmentEvent()
    data class LoadUserPosts(val name: String? = null): ProfileDetailFragmentEvent()

    data class Unfollow(val user: User): ProfileDetailFragmentEvent()
    data class Follow(val user: User): ProfileDetailFragmentEvent()

    object Idle : ProfileDetailFragmentEvent()
}