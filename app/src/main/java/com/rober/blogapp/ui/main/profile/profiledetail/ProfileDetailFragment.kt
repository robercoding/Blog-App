package com.rober.blogapp.ui.main.profile.profiledetail

import android.content.SharedPreferences
import android.graphics.*
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.rober.blogapp.R
import com.rober.blogapp.entity.Post
import com.rober.blogapp.entity.User
import com.rober.blogapp.ui.base.BaseFragment
import com.rober.blogapp.ui.main.feed.adapter.PostAdapter
import com.rober.blogapp.ui.main.profile.profiledetail.utils.IOnTouchListener
import com.rober.blogapp.ui.main.profile.profiledetail.utils.MotionLayoutTransitionListener
import com.rober.blogapp.ui.main.profile.profiledetail.utils.OnTouchListener
import com.rober.blogapp.ui.main.settings.preferences.utils.Keys
import com.rober.blogapp.util.RecyclerViewActionInterface
import com.stfalcon.imageviewer.StfalconImageViewer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_profile_detail.*
import javax.inject.Inject

@AndroidEntryPoint
class ProfileFragment :
    BaseFragment<ProfileDetailState, ProfileDetailFragmentEvent, ProfileDetailViewModel>(R.layout.fragment_profile_detail),
    RecyclerViewActionInterface, IOnTouchListener {

    override val viewModel: ProfileDetailViewModel by viewModels()

    private val profileDetailViewModel: ProfileDetailViewModel by viewModels()
    lateinit var postAdapter: PostAdapter
    private val viewHolder = R.layout.adapter_feed_viewholder_posts

    private var isProfileImageVisualizing = false
    private var isBackgroundImageVisualizing = false

    private var isUserFollowingInAction = false
    private var isUserUnfollowingInAction = false

    private lateinit var onTouchListener: OnTouchListener

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    @Inject
    lateinit var keys: Keys

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        getUserArgumentAndSetIntention()
    }

    private fun getUserArgumentAndSetIntention() {
        val user = arguments?.getParcelable<User>("userObject")

        user?.run {
            profileDetailViewModel.setIntention(ProfileDetailFragmentEvent.SetUserObjectDetails(user))
        } ?: kotlin.run {

            val userName = arguments?.getString("userId")

            if (userName.isNullOrBlank()) {
                profileDetailViewModel.setIntention(ProfileDetailFragmentEvent.LoadUserDetails(null))
            } else {
                profileDetailViewModel.setIntention(ProfileDetailFragmentEvent.LoadUserDetails(userName))
            }
        }
    }

    override fun render(viewState: ProfileDetailState) {
        when (viewState) {
            is ProfileDetailState.SetCurrentUserProfile -> {
                showProfileDetailView(true)

                val user = viewState.user

                setViewForCurrentUser(user, viewState.bitmap)
                setUserProfileData(user)

                profileDetailViewModel.setIntention(ProfileDetailFragmentEvent.LoadUserPosts)
            }

            is ProfileDetailState.SetOtherUserProfile -> {
                val user = viewState.user

                displayBottomNavigation(false)
                showProfileDetailView(true)

                setFollowButtonViewForOtherUser(viewState.currentUserFollowsOtherUser)

                val bitmap = viewState.bitmap

                setViewForOtherUser(bitmap, user)
                setOtherUserProfileData(user)
                profileDetailViewModel.setIntention(ProfileDetailFragmentEvent.LoadUserPosts)
            }

            is ProfileDetailState.SetUserPosts -> {
                val listUserPosts = viewState.listUserPosts
                setUserPosts(listUserPosts.toMutableList(), viewState.user)
                displayProgressBar(false)
                stopSwipeRefresh()
                profileDetailViewModel.setIntention(ProfileDetailFragmentEvent.Idle)
            }

            is ProfileDetailState.LoadingPosts -> {
                displayProgressBar(true)
            }

            is ProfileDetailState.LoadingUser -> {
                showProfileDetailView(false)
            }

            is ProfileDetailState.LoadBackgroundImage -> {
                val imageProfile = viewState.backgroundImageUrl

                val images = mutableListOf<Any>()
                if (imageProfile.isEmpty()) {
                    images.add(R.drawable.blue_screen)
                } else {
                    images.add(imageProfile)
                }

                StfalconImageViewer.Builder<Any>(requireContext(), images) { view, image ->
                    Glide.with(requireView())
                        .load(image)
                        .into(view)
                }.allowZooming(true)
                    .withHiddenStatusBar(false)
                    .withTransitionFrom(profile_detail_image_background_clear)
                    .withDismissListener { isBackgroundImageVisualizing = false }
                    .show()
            }

            is ProfileDetailState.LoadProfileImage -> {
                Toast.makeText(requireContext(), "Load image profile", Toast.LENGTH_SHORT).show()
                val imageProfile = viewState.profileImageUrl

                val images = mutableListOf<Any>()
                if (imageProfile.isEmpty()) {
                    images.add(R.drawable.user_profile_png)
                } else {
                    images.add(imageProfile)
                }

                StfalconImageViewer.Builder<Any>(requireContext(), images) { view, image ->
                    Glide.with(requireView())
                        .load(image)
                        .into(view)
                }.allowZooming(true)
                    .withDismissListener { isProfileImageVisualizing = false }
                    .withHiddenStatusBar(false)
                    .withTransitionFrom(uid_image)
                    .show()

//                isProfileImageVisualizing = false
            }

            is ProfileDetailState.Followed -> {
                val follower = viewState.user.follower
                Toast.makeText(
                    requireContext(),
                    "Now you're following ${viewState.user.username}",
                    Toast.LENGTH_SHORT
                ).show()
                setFollowButtonViewForOtherUser(true)
                setFollowerText(follower)
                isUserFollowingInAction = false
            }

            is ProfileDetailState.FollowError -> {
                isUserFollowingInAction = false
                Toast.makeText(
                    requireContext(),
                    "Sorry we couldn't follow the user, try again later",
                    Toast.LENGTH_SHORT
                ).show()
                setFollowButtonViewForOtherUser(false)
            }

            is ProfileDetailState.Unfollowed -> {
                val follower = viewState.user.follower
                Log.i("UserFollower", "Get $follower")
                Toast.makeText(
                    requireContext(),
                    "You stopped following ${viewState.user.username}",
                    Toast.LENGTH_SHORT
                ).show()
                setFollowButtonViewForOtherUser(false)
                setFollowerText(follower)
                isUserUnfollowingInAction = false
            }

            is ProfileDetailState.UnfollowError -> {
                isUserUnfollowingInAction = false
                Toast.makeText(
                    requireContext(),
                    "Sorry we couldn't unfollow the user, try again later",
                    Toast.LENGTH_SHORT
                ).show()
                setFollowButtonViewForOtherUser(true)
            }

            is ProfileDetailState.NavigateToPostDetail -> {
                navigateToPostDetail(viewState.post)
            }

            is ProfileDetailState.NavigateToProfileEdit -> {
                navigateToProfileEdit(viewState.user)
            }

            is ProfileDetailState.NavigateToSettings -> {
                navigateToSettings()
            }

            is ProfileDetailState.PopBackStack -> {
                backToPreviousFragment()
            }

            is ProfileDetailState.Error -> {
                Toast.makeText(
                    requireContext(),
                    "${viewState.exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
                backToPreviousFragment()
            }

            is ProfileDetailState.Idle -> {
                //Nothing
            }
        }
    }

    private fun showProfileDetailView(showMotionLayout: Boolean) {
        if (showMotionLayout) {
            profile_detail_motion_layout.visibility = View.VISIBLE
            profile_detail_background_progress_bar.visibility = View.VISIBLE
        } else {
            profile_detail_background_progress_bar.visibility = View.VISIBLE
            profile_detail_motion_layout.visibility = View.GONE
        }

        profile_detail_swipe_refresh_layout.setProgressBackgroundColorSchemeColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.primaryBackground
            )
        )
        profile_detail_swipe_refresh_layout.setColorSchemeColors(
            ContextCompat.getColor(
                requireContext(),
                R.color.blueTwitter
            )
        )
    }

    private fun setUserProfileData(user: User) {

        uid_name.text = "@${user.username}"
        uid_biography.text = user.biography
        profile_detail_user_following.text = "${user.following} Following"

        if (user.location.isNotEmpty()) {
            profile_detail_motion_layout.getConstraintSet(R.id.start)
                .getConstraint(R.id.profile_detail_location).propertySet.visibility = View.VISIBLE
            profile_detail_location.text = user.location
            val drawable = resources.getDrawable(R.drawable.ic_location, null)
            drawable.setBounds(
                0,
                0,
                (drawable.intrinsicWidth * 0.8).toInt(),
                (drawable.intrinsicHeight * 0.8).toInt()
            )
            profile_detail_location.setCompoundDrawables(drawable, null, null, null)
        } else {
            profile_detail_motion_layout.getConstraintSet(R.id.start)
                .getConstraint(R.id.profile_detail_location).propertySet.visibility = View.GONE
        }

        setFollowerText(user.follower)
    }

    private fun setViewForCurrentUser(user: User, bitmap: Bitmap) {
        Log.i("SetView", "Setting current User!")
        profile_detail_button_follow.visibility = View.GONE
        profile_detail_button_edit.visibility = View.VISIBLE

        val colorPalette = if (user.backgroundImageUrl.isEmpty())
            R.drawable.blue_screen
        else
            user.backgroundImageUrl

        setPaletteWithMotionLayoutListener(colorPalette, bitmap)

        profile_detail_motion_layout.apply {
            this.getConstraintSet(R.id.start)?.let {
                it.getConstraint(R.id.profile_detail_button_edit).propertySet.visibility =
                    View.VISIBLE
                it.getConstraint(R.id.profile_detail_button_follow).propertySet.visibility =
                    View.GONE
            }
            this.getConstraintSet(R.id.end)?.let {
                it.getConstraint(R.id.profile_detail_button_edit).propertySet.visibility =
                    View.VISIBLE
                it.getConstraint(R.id.profile_detail_button_follow).propertySet.visibility =
                    View.GONE
            }
        }
        setImages(user)
    }

    private fun setViewForOtherUser(bitmap: Bitmap, user: User) {
        Log.i("SetView", "Setting other User!")
        profile_detail_button_follow.visibility = View.VISIBLE
        profile_detail_button_edit.visibility = View.GONE

        val colorPalette = if (user.backgroundImageUrl.isEmpty())
            R.drawable.blue_screen
        else
            user.backgroundImageUrl

        setPaletteWithMotionLayoutListener(colorPalette, bitmap)

        profile_detail_motion_layout.apply {
            this.getConstraintSet(R.id.start)?.let { constraintSet ->
                constraintSet.getConstraint(R.id.profile_detail_button_edit).propertySet.visibility =
                    View.GONE
                constraintSet.getConstraint(R.id.profile_detail_button_follow).propertySet.visibility =
                    View.VISIBLE
            }
            this.getConstraintSet(R.id.end)?.let {
                it.getConstraint(R.id.profile_detail_button_edit).propertySet.visibility = View.GONE
                it.getConstraint(R.id.profile_detail_button_follow).propertySet.visibility = View.VISIBLE
            }
        }
        setImages(user)
    }

    private fun setImages(user: User) {
        val profileImageToLoad: Any = if (user.profileImageUrl.isEmpty())
            R.drawable.user_profile_png
        else
            user.profileImageUrl

        val backgroundImageToLoad = if (user.backgroundImageUrl.isEmpty())
            R.drawable.blue_screen
        else
            user.backgroundImageUrl

        Glide.with(requireView())
            .load(profileImageToLoad)
            .into(uid_image)

        Glide.with(requireView())
            .load(backgroundImageToLoad)
            .into(profile_detail_image_background_clear)
    }

    private fun setPaletteWithMotionLayoutListener(imageFromUrlToolbarStart: Any, bitmap: Bitmap) {
        Palette.Builder(bitmap).generate { palette ->
            palette?.let {
                val color = it.getDominantColor(ContextCompat.getColor(requireContext(), R.color.black))
                val motionLayoutTransitionListener =
                    MotionLayoutTransitionListener(requireView(), imageFromUrlToolbarStart, color)
                profile_detail_motion_layout.apply {
                    setTransitionListener(motionLayoutTransitionListener)
                }
            } ?: kotlin.run {
                Log.i("Palette", "Palette is not working")
            }
        }
    }

    private fun setOtherUserProfileData(user: User) {

        uid_name.text = "@${user.username}"
        uid_biography.text = user.biography
        profile_detail_user_following.text = "${user.following} Following"
        if (user.location.isNotEmpty()) {
            profile_detail_motion_layout.getConstraintSet(R.id.start)
                .getConstraint(R.id.profile_detail_location).propertySet.visibility = View.VISIBLE
            profile_detail_location.text = user.location
            val drawable = resources.getDrawable(R.drawable.ic_location, null)
            drawable.setBounds(
                0,
                0,
                (drawable.intrinsicWidth * 0.8).toInt(),
                (drawable.intrinsicHeight * 0.8).toInt()
            )
            profile_detail_location.setCompoundDrawables(drawable, null, null, null)
        } else {
            profile_detail_motion_layout.getConstraintSet(R.id.start)
                .getConstraint(R.id.profile_detail_location).propertySet.visibility = View.GONE
        }

        setFollowerText(user.follower)
    }

    private fun setFollowerText(follower: Int) {
        profile_detail_user_followers.text = "${follower} Follower"

        if (follower > 1)
            profile_detail_user_followers.append("s")
    }


    private fun setUserPosts(listUserPosts: MutableList<Post>, user: User) {
        postAdapter.setPosts(listUserPosts)
        postAdapter.setUsers(listOf(user).toMutableList())

        recycler_profile_detail_posts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = postAdapter
        }
    }

    private fun setFollowButtonViewForOtherUser(currentUserFollowsOtherUser: Boolean) {
        val textColor: Int
        val background: Int

        if (currentUserFollowsOtherUser) {
            background = getColor(R.color.blueGray)
            textColor = getColor(R.color.white)

            profile_detail_button_follow.apply {
                isSelected = true
                setBackgroundColor(getColor(R.color.blueGray))
                setTypeface(null, Typeface.BOLD)
                setTextColor(textColor)
                text = "Following"
            }
        } else {
            textColor = getColor(R.color.blueGray)
            background = getColor(R.color.primaryBackground)

            profile_detail_button_follow.apply {
                isSelected = false
                text = "Follow"

                setBackgroundColor(background)
                setTextColor(textColor)
            }
        }
    }

    override fun setupObjects() {
        super.setupObjects()

        postAdapter = PostAdapter(requireView(), viewHolder, this)
        onTouchListener = OnTouchListener(requireView(), this)

        profile_detail_swipe_refresh_layout.setOnTouchListener(onTouchListener)
    }

    override fun setupListeners() {
        profile_detail_button_follow.apply {
            setOnClickListener {
                if (!isUserFollowingInAction && !isUserUnfollowingInAction) {
                    if (it.isSelected) {
                        isUserUnfollowingInAction = true
                        setFollowButtonViewForOtherUser(false)
                        profileDetailViewModel.setIntention(ProfileDetailFragmentEvent.Unfollow)
                    } else {
                        isUserFollowingInAction = true
                        setFollowButtonViewForOtherUser(true)
                        profileDetailViewModel.setIntention(ProfileDetailFragmentEvent.Follow)
                    }
                }
            }
        }

        profile_detail_button_edit.apply {
            setOnClickListener {
                profileDetailViewModel.setIntention(ProfileDetailFragmentEvent.NavigateToProfileEdit)
            }
        }

        profile_detail_swipe_refresh_layout.setOnRefreshListener {
            profileDetailViewModel.setIntention(ProfileDetailFragmentEvent.LoadNewerPosts)
        }

        profile_detail_arrow_back.setOnClickListener {
            profileDetailViewModel.setIntention(ProfileDetailFragmentEvent.PopBackStack)
        }

        profile_detail_settings.setOnClickListener {
            profileDetailViewModel.setIntention(ProfileDetailFragmentEvent.NavigateToSettings)
        }

        profile_detail_image_background_clear.setOnClickListener {
        }
    }

    private fun stopSwipeRefresh() {
        profile_detail_swipe_refresh_layout.isRefreshing = false
    }

    private fun displayProgressBar(isDisplayed: Boolean) {
        if (isDisplayed) {
            progress_bar_profile_posts.visibility = View.VISIBLE
        } else {
            progress_bar_profile_posts.visibility = View.GONE
        }
    }

    private fun displayBottomNavigation(display: Boolean) {
        val navController = activity?.bottom_navigation ?: return
        if (display) navController.visibility = View.VISIBLE else navController.visibility =
            View.GONE
    }

    private fun backToPreviousFragment() {
//        val navController = activity?.bottom_navigation ?: return
        val navController = findNavController()
        navController.popBackStack()
    }

    private fun navigateToPostDetail(post: Post) {
        val navController = findNavController()
        val postBundle = bundleOf("post" to post)
        navController.navigate(R.id.action_profileDetailFragment_to_postDetailFragment, postBundle)
    }

    private fun navigateToProfileEdit(user: User) {
        val navController = findNavController()
        val userBundle = bundleOf("user" to user)
        navController.navigate(R.id.action_profileDetailFragment_to_profileEditFragment, userBundle)
    }

    private fun navigateToSettings() {
        findNavController().navigate(R.id.action_profileDetailFragment_to_settingsFragment)
    }

    override fun setRippleEffectIfTouch(view: View, touchCoordinateX: Float, touchCoordinateY: Float) {
        val viewCoordinatesX = intArrayOf(view.left, view.right)
        val viewCoordinatesY = intArrayOf(view.top, view.bottom)

        if (touchCoordinateX > viewCoordinatesX[0] && touchCoordinateX < viewCoordinatesX[1] && touchCoordinateY > viewCoordinatesY[0] && touchCoordinateY < viewCoordinatesY[1]) {
            view.isPressed = true
        }
    }

//    private fun setDarkerModeOnImage(view: ImageView, touchCoordinateX: Float, touchCoordinateY: Float) {
//        val viewCoordinatesX = intArrayOf(view.left, view.right)
//        val viewCoordinatesY = intArrayOf(view.top, view.bottom)
//
//        if (touchCoordinateX > viewCoordinatesX[0] && touchCoordinateX < viewCoordinatesX[1] && touchCoordinateY > viewCoordinatesY[0] && touchCoordinateY < viewCoordinatesY[1]) {
//        }
//    }

    override fun isTouchActionUpOnViewPlace(
        view: View,
        touchCoordinateX: Float,
        touchCoordinateY: Float
    ): Boolean {
        val viewCoordinatesX = intArrayOf(view.left, view.right)
        val viewCoordinatesY = intArrayOf(view.top, view.bottom)

        return touchCoordinateX > viewCoordinatesX[0] && touchCoordinateX < viewCoordinatesX[1] && touchCoordinateY > viewCoordinatesY[0] && touchCoordinateY < viewCoordinatesY[1]
    }

    override fun setTouchIntention(profileDetailFragmentEvent: ProfileDetailFragmentEvent) {
        when (profileDetailFragmentEvent) {
            is ProfileDetailFragmentEvent.PopBackStack -> {
                profileDetailViewModel.setIntention(profileDetailFragmentEvent)
            }

            is ProfileDetailFragmentEvent.NavigateToProfileEdit -> {
                profileDetailViewModel.setIntention(ProfileDetailFragmentEvent.NavigateToProfileEdit)
            }

            is ProfileDetailFragmentEvent.NavigateToSettings -> {
                profileDetailViewModel.setIntention(ProfileDetailFragmentEvent.NavigateToSettings)
            }

            is ProfileDetailFragmentEvent.LoadProfileImage -> {
                if (!isProfileImageVisualizing && !isProfileImageVisualizing) {
                    isProfileImageVisualizing = true
                    profileDetailViewModel.setIntention(profileDetailFragmentEvent)
                }
            }

            is ProfileDetailFragmentEvent.LoadBackgroundImage -> {
                if (!isBackgroundImageVisualizing && !isProfileImageVisualizing) {
                    isBackgroundImageVisualizing = true
                    profileDetailViewModel.setIntention(profileDetailFragmentEvent)
                }
            }

            is ProfileDetailFragmentEvent.Follow -> {
                setFollowButtonViewForOtherUser(true)
                if (!isUserUnfollowingInAction && !isUserFollowingInAction) {
                    isUserFollowingInAction = true
                    profileDetailViewModel.setIntention(ProfileDetailFragmentEvent.Follow)
                }
            }

            is ProfileDetailFragmentEvent.Unfollow -> {
                setFollowButtonViewForOtherUser(false)
                if (!isUserUnfollowingInAction && !isUserFollowingInAction) {
                    isUserUnfollowingInAction = true
                    profileDetailViewModel.setIntention(ProfileDetailFragmentEvent.Unfollow)

                }
            }
        }
    }

    override fun setupViewDesign() {
        super.setupViewDesign()
        profile_detail_arrow_back.colorFilter =
            PorterDuffColorFilter(
                ContextCompat.getColor(requireContext(), R.color.blueTwitter),
                PorterDuff.Mode.SRC_ATOP
            )
    }

    override fun clickListenerOnPost(positionAdapter: Int) {
        profileDetailViewModel.setIntention(ProfileDetailFragmentEvent.NavigateToPostDetail(positionAdapter))
    }

    override fun clickListenerOnUser(positionAdapter: Int) {
        //TODO
    }


    override fun requestMorePosts(actualRecyclerViewPosition: Int) {
        //
    }

    override fun clickListenerOnSettings(positionAdapter: Int) {
        //
    }
}

sealed class ProfileDetailFragmentEvent {
    data class SetUserObjectDetails(val user: User) : ProfileDetailFragmentEvent()
    data class LoadUserDetails(val userUID: String? = null) : ProfileDetailFragmentEvent()
    object LoadUserPosts : ProfileDetailFragmentEvent()
    object LoadNewerPosts : ProfileDetailFragmentEvent()

    object LoadBackgroundImage : ProfileDetailFragmentEvent()
    object LoadProfileImage : ProfileDetailFragmentEvent()

    object Unfollow : ProfileDetailFragmentEvent()
    object Follow : ProfileDetailFragmentEvent()

    data class NavigateToPostDetail(val positionAdapter: Int) : ProfileDetailFragmentEvent()
    object NavigateToProfileEdit : ProfileDetailFragmentEvent()
    object NavigateToSettings : ProfileDetailFragmentEvent()
    object PopBackStack : ProfileDetailFragmentEvent()

    object Idle : ProfileDetailFragmentEvent()
}