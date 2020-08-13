package com.rober.blogapp.ui.main.profile.profiledetail

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
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
            profileDetailViewModel.setIntention(ProfileDetailFragmentEvent.loadUserDetails(null))
        }else{
            profileDetailViewModel.setIntention(ProfileDetailFragmentEvent.loadUserDetails(userName))
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
                profileDetailViewModel.setIntention(ProfileDetailFragmentEvent.loadUserPosts(null))
            }

            is ProfileDetailState.SetUserPosts -> {
                displayProgressBar(false)

                val listUserPosts = profileDetailState.listUserPosts
                setUserPosts(listUserPosts.toMutableList())
            }

            is ProfileDetailState.SetOtherUserProfile -> {
                displayBottomNavigation(false)
                showViewMotionLayout(true)
                setViewForOtherUser()
                val visibilityFollow = profile_detail_motion_layout.getConstraintSet(R.id.start).getConstraint(R.id.profile_detail_button_follow).propertySet.visibility
                val visibilityEdit = profile_detail_motion_layout.getConstraintSet(R.id.start).getConstraint(R.id.profile_detail_button_edit).propertySet.visibility
                Log.i(TAG, "Visibility after setting Follow: $visibilityFollow")
                Log.i(TAG, "Visibility after setting Edit: $visibilityEdit")
                setOtherUserProfile(profileDetailState.user)
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

    private fun displayProgressBar(isDisplayed: Boolean){
        progress_bar_profile_posts.visibility = if(isDisplayed) View.VISIBLE else View.GONE
    }

    private fun displayBottomNavigation(display: Boolean){
        val navController = activity?.bottom_navigation ?: return
        if(display) navController.visibility = View.VISIBLE else navController.visibility = View.GONE
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
    data class loadUserDetails(val name: String? = null): ProfileDetailFragmentEvent()
    data class loadUserPosts(val name: String? = null): ProfileDetailFragmentEvent()
}