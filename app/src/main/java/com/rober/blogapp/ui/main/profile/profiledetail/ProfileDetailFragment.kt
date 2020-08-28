package com.rober.blogapp.ui.main.profile.profiledetail

import android.graphics.Bitmap
import android.graphics.Typeface
import android.graphics.drawable.ShapeDrawable
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Toast
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.rober.blogapp.R
import com.rober.blogapp.entity.Post
import com.rober.blogapp.entity.User
import com.rober.blogapp.ui.main.feed.adapter.PostAdapter
import com.rober.blogapp.ui.main.profile.profiledetail.utils.MotionLayoutTransitionListener
import com.rober.blogapp.util.GetImageBitmapFromUrlAsyncTask
import com.rober.blogapp.util.RecyclerViewActionInterface
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_feed.*
import kotlinx.android.synthetic.main.fragment_profile_detail.*

@AndroidEntryPoint
class ProfileFragment : Fragment(), RecyclerViewActionInterface {
    private val TAG = "ProfileDetailFragment"

    private val profileDetailViewModel: ProfileDetailViewModel by viewModels()
    lateinit var postAdapter: PostAdapter
    private val viewHolder = R.layout.adapter_feed_viewholder_posts

    var dominantColorToolbarMotionLayoutEnd = 0
    var imageToolbarMotionLayoutStart = ""

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

    private fun subscribeObservers() {
        profileDetailViewModel.profileDetailState.observe(
            viewLifecycleOwner,
            Observer { profileDetailState ->
                render(profileDetailState)
            })
    }

    private fun getUserArgumentAndSetIntention() {
        val userName = arguments?.getString("user_id")

        if (userName.isNullOrBlank()) {
            profileDetailViewModel.setIntention(ProfileDetailFragmentEvent.LoadUserDetails(null))
        } else {
            profileDetailViewModel.setIntention(ProfileDetailFragmentEvent.LoadUserDetails(userName))
        }
    }

    private fun render(profileDetailState: ProfileDetailState) {
        Log.i(TAG, "State: ${profileDetailState}")
        when (profileDetailState) {
            is ProfileDetailState.SetCurrentUserProfile -> {
                Log.i(TAG, "SetCurrentUserProfile")
                showViewMotionLayout(true)

                val user = profileDetailState.user
                imageToolbarMotionLayoutStart = profileDetailState.imageBackground

                setViewForCurrentUser(profileDetailState.bitmap)
                setUserProfile(user)

                profileDetailViewModel.setIntention(ProfileDetailFragmentEvent.LoadUserPosts)
            }

            is ProfileDetailState.SetOtherUserProfile -> {
                val user = profileDetailState.user

                displayBottomNavigation(false)
                showViewMotionLayout(true)

                setFollowButtonViewForOtherUser(profileDetailState.currentUserFollowsOtherUser)

                setViewForOtherUser()
                setOtherUserProfile(user)
                profileDetailViewModel.setIntention(ProfileDetailFragmentEvent.LoadUserPosts)
            }

            is ProfileDetailState.SetUserPosts -> {
                val listUserPosts = profileDetailState.listUserPosts
                setUserPosts(listUserPosts.toMutableList())
                displayProgressBar(false)
                stopSwipeRefresh()
                profileDetailViewModel.setIntention(ProfileDetailFragmentEvent.Idle)
            }

            is ProfileDetailState.LoadingPosts -> {
                displayProgressBar(true)
            }

            is ProfileDetailState.LoadingUser -> {
                showViewMotionLayout(false)
            }

            is ProfileDetailState.Followed -> {
                val follower = profileDetailState.user.follower
                Toast.makeText(
                    requireContext(),
                    "Now you're following ${profileDetailState.user.username}",
                    Toast.LENGTH_SHORT
                ).show()
                setFollowButtonViewForOtherUser(true)
                setFollowerText(follower)
            }

            is ProfileDetailState.FollowError -> {
                Toast.makeText(
                    requireContext(),
                    "Sorry we couldn't follow the user, try again later",
                    Toast.LENGTH_SHORT
                ).show()
                setFollowButtonViewForOtherUser(false)
            }

            is ProfileDetailState.Unfollowed -> {
                val follower = profileDetailState.user.follower
                Log.i("UserFollower", "Get ${follower}")
                Toast.makeText(
                    requireContext(),
                    "You stopped following ${profileDetailState.user.username}",
                    Toast.LENGTH_SHORT
                ).show()
                setFollowButtonViewForOtherUser(false)
                setFollowerText(follower)
            }

            is ProfileDetailState.UnfollowError -> {
                Toast.makeText(
                    requireContext(),
                    "Sorry we couldn't unfollow the user, try again later",
                    Toast.LENGTH_SHORT
                ).show()
                setFollowButtonViewForOtherUser(true)
            }

            is ProfileDetailState.Error -> {
                Toast.makeText(
                    requireContext(),
                    "${profileDetailState.exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
                backToPreviousFragment()
            }

            is ProfileDetailState.Idle -> {
                //Nothing
            }
        }
    }

    private fun showViewMotionLayout(showMotionLayout: Boolean) {
        if (showMotionLayout) {
            profile_detail_motion_layout.visibility = View.VISIBLE
            profile_detail_background_progress_bar.visibility = View.VISIBLE
        } else {
            profile_detail_background_progress_bar.visibility = View.VISIBLE
            profile_detail_motion_layout.visibility = View.GONE
        }
    }

    private fun setUserProfile(user: User) {

        uid_name.text = "@${user.username}"
        uid_biography.text = user.biography
        uid_following.text = "${user.following} Following"

        Glide.with(requireView())
            .load("https://firebasestorage.googleapis.com/v0/b/blog-app-d5912.appspot.com/o/users_profile_picture%2Fmew_small_1024_x_1024.jpg?alt=media&token=21dfa28c-2416-49c3-81e1-2475aaf25150")
            .into(uid_image)

        Glide.with(requireView())
            .load("https://firebasestorage.googleapis.com/v0/b/blog-app-d5912.appspot.com/o/users_profile_picture%2Fmew_small_1024_x_1024.jpg?alt=media&token=21dfa28c-2416-49c3-81e1-2475aaf25150")
            .into(profile_detail_image_background)

        setFollowerText(user.follower)
    }

    private fun setViewForCurrentUser(bitmap: Bitmap) {
        Log.i("SetView", "Setting current User!")
        profile_detail_button_follow.visibility = View.GONE
        profile_detail_button_edit.visibility = View.VISIBLE

        Palette.Builder(bitmap).generate {palette ->
            palette?.let {
                val color = it.getDominantColor(ContextCompat.getColor(requireContext(), R.color.colorBlack))
                val motionLayoutTransitionListener = MotionLayoutTransitionListener(requireView(), imageToolbarMotionLayoutStart, color)
                profile_detail_motion_layout.apply {
                    setTransitionListener(motionLayoutTransitionListener)
                }

            }?: kotlin.run {
                Log.i("Palette", "Palette is not working")
            }
        }

        //When I apply this inside the palette the edit profile doesn't appear like follow button
        profile_detail_motion_layout.apply {
            Log.i("CurrentColor", "$dominantColorToolbarMotionLayoutEnd")

            getConstraintSet(R.id.start)?.let {
                it.getConstraint(R.id.profile_detail_button_edit).propertySet.visibility =
                    View.VISIBLE
                it.getConstraint(R.id.profile_detail_button_follow).propertySet.visibility =
                    View.GONE
                Glide.with(requireView())
                    .load("https://firebasestorage.googleapis.com/v0/b/blog-app-d5912.appspot.com/o/users_profile_picture%2Fmew_small_1024_x_1024.jpg?alt=media&token=21dfa28c-2416-49c3-81e1-2475aaf25150")
                    .into(profile_detail_image_background)
            }

            getConstraintSet(R.id.end)?.let {
                it.getConstraint(R.id.profile_detail_button_edit).propertySet.visibility =
                    View.VISIBLE
                it.getConstraint(R.id.profile_detail_button_follow).propertySet.visibility =
                    View.GONE
            }
        }


    }

    private fun setOtherUserProfile(user: User) {

        uid_name.text = "@${user.username}"
        uid_biography.text = user.biography
        uid_following.text = "${user.following} Following"
        Glide.with(requireView())
            .load("https://firebasestorage.googleapis.com/v0/b/blog-app-d5912.appspot.com/o/users_profile_picture%2Fmew_small_1024_x_1024.jpg?alt=media&token=21dfa28c-2416-49c3-81e1-2475aaf25150")
            .into(uid_image)

        Glide.with(requireView())
            .load("https://firebasestorage.googleapis.com/v0/b/blog-app-d5912.appspot.com/o/users_profile_picture%2Fmew_small_1024_x_1024.jpg?alt=media&token=21dfa28c-2416-49c3-81e1-2475aaf25150")
            .into(profile_detail_image_background)

        setFollowerText(user.follower)
    }

    private fun setFollowerText(follower: Int) {
        uid_followers.text = "${follower} Follower"

        if (follower > 1)
            uid_followers.append("s")
    }

    private fun setViewForOtherUser() {
        Log.i("SetView", "Setting other User!")
        profile_detail_button_follow.visibility = View.VISIBLE
        profile_detail_button_edit.visibility = View.GONE

        profile_detail_motion_layout.getConstraintSet(R.id.start)?.let {
            it.getConstraint(R.id.profile_detail_button_edit).propertySet.visibility = View.GONE
            it.getConstraint(R.id.profile_detail_button_follow).propertySet.visibility =
                View.VISIBLE
        }
        profile_detail_motion_layout.getConstraintSet(R.id.end)?.let {
            it.getConstraint(R.id.profile_detail_button_edit).propertySet.visibility = View.GONE
            it.getConstraint(R.id.profile_detail_button_follow).propertySet.visibility =
                View.VISIBLE
        }
    }

    private fun setUserPosts(listUserPosts: MutableList<Post>) {
        postAdapter.setPosts(listUserPosts)

        recycler_profile_detail_posts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = postAdapter
        }
    }

    private fun setFollowButtonViewForOtherUser(currentUserFollowsOtherUser: Boolean) {

        if (currentUserFollowsOtherUser) {
            profile_detail_button_follow.apply {
                isSelected = true
                setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.blueGray))
                text = "Following"
                setTypeface(null, Typeface.BOLD)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                text = "Following"
            }
        } else {
            profile_detail_button_follow.apply {
                isSelected = false
                text = "Follow"
                setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.background))
                val shapeDrawable = ShapeDrawable()
                shapeDrawable.paint.strokeWidth = 1.0F
                shapeDrawable.paint.color =
                    ContextCompat.getColor(requireContext(), R.color.blueGray)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.blueGray))
            }
        }
    }

    private fun setupListeners() {
        profile_detail_button_follow.apply {
            setOnClickListener {
                if (it.isSelected) {
                    setFollowButtonViewForOtherUser(false)
                    profileDetailViewModel.setIntention(ProfileDetailFragmentEvent.Unfollow)

                } else {
                    setFollowButtonViewForOtherUser(true)
                    profileDetailViewModel.setIntention(ProfileDetailFragmentEvent.Follow)
                }
            }
        }

        profile_detail_swipe_refresh_layout.setOnRefreshListener {
            profileDetailViewModel.setIntention(ProfileDetailFragmentEvent.LoadNewerPosts)
        }
    }

    private fun stopSwipeRefresh() {
        profile_detail_swipe_refresh_layout.isRefreshing = false
    }

    private fun displayProgressBar(isDisplayed: Boolean) {
        if (isDisplayed){
            progress_bar_profile_posts.visibility = View.VISIBLE
        }else{
            progress_bar_profile_posts.visibility = View.GONE
        }
    }

    private fun displayBottomNavigation(display: Boolean) {
        val navController = activity?.bottom_navigation ?: return
        if (display) navController.visibility = View.VISIBLE else navController.visibility =
            View.GONE
    }

    private fun backToPreviousFragment() {
        val navController = activity?.bottom_navigation ?: return
        navController.findNavController().popBackStack()
    }


    override fun clickListenerOnPost(positionAdapter: Int) {
        //TODO
    }

    override fun clickListenerOnUser(positionAdapter: Int) {
        //TODO
    }


    override fun requestMorePosts(actualRecyclerViewPosition: Int) {
        //
    }
}

sealed class ProfileDetailFragmentEvent {
    data class LoadUserDetails(val name: String? = null) : ProfileDetailFragmentEvent()
    object LoadUserPosts : ProfileDetailFragmentEvent()
    object LoadNewerPosts : ProfileDetailFragmentEvent()

    object Unfollow : ProfileDetailFragmentEvent()
    object Follow : ProfileDetailFragmentEvent()

    object Idle : ProfileDetailFragmentEvent()
}