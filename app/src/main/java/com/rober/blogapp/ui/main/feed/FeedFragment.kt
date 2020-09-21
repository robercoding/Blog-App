package com.rober.blogapp.ui.main.feed

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.postOnAnimationDelayed
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.rober.blogapp.R
import com.rober.blogapp.entity.Post
import com.rober.blogapp.entity.User
import com.rober.blogapp.ui.main.feed.adapter.PostAdapter
import com.rober.blogapp.util.RecyclerViewActionInterface
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_feed.*

@AndroidEntryPoint
class FeedFragment : Fragment(), RecyclerViewActionInterface, OnMoveRecyclerListener {


    private val TAG: String = "FeedFragment"

    private var mHasPullRefresh = false
    private var resource: Int = R.layout.adapter_feed_viewholder_posts

    private val viewModel: FeedViewModel by viewModels()
    lateinit var postAdapter: PostAdapter

    private lateinit var animation: Animation
    private var isBounceAnimationVisible = false

    private var onScrollListenerHelper: OnScrollListenerHelper? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_feed, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        setupView()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        subscribeObservers()
        animation = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_out)
        viewModel.setIntention(FeedFragmentEvent.GetUserPicture)

        postAdapter = PostAdapter(requireView(), resource, this)
    }

    private fun subscribeObservers() {
        viewModel.feedState.observe(viewLifecycleOwner, Observer { feedState ->
            render(feedState)
        })
    }

    private fun render(feedState: FeedState) {
        Log.i("States", "State = ${feedState}")
        when (feedState) {
            is FeedState.SetUserDetails -> {
                setUserDetails(feedState.user)
                viewModel.setIntention(FeedFragmentEvent.RetrieveInitPosts)
            }
            is FeedState.SetListPosts -> {
                displayTextWelcomeMessage(false)
                displayProgressBarInitialPosts(false)
                stopSwipeRefresh()

                postAdapter.setUsers(feedState.listFeedUsers.toMutableList())
                postAdapter.setPosts(feedState.listFeedPosts.toMutableList())

                val linearLayoutManager = LinearLayoutManager(requireContext())

                setAdapterToRecyclerFeed(linearLayoutManager)
                viewModel.setIntention(FeedFragmentEvent.Idle)
            }

            is FeedState.LoadNewPosts -> {
                stopSwipeRefresh()
                displayTextNotifyMorePosts(true)
                displayTextWelcomeMessage(false)

                postAdapter.setUsers(feedState.listFeedUsers.toMutableList())
                postAdapter.setPosts(feedState.listFeedPosts.toMutableList())

                var linearLayoutManager = LinearLayoutManager(requireContext())
                recycler_feed.layoutManager?.run {
                    linearLayoutManager = this as LinearLayoutManager
                }

                recycler_feed.adapter?.itemCount?.also { itemCount ->
                    val pos = linearLayoutManager.findLastCompletelyVisibleItemPosition()

                    if (pos >= itemCount - 1) {
                        animationNotifyPostsBounceAndFadeOut()
                    } else {
                        animationNotifyBounce()
                    }
                } ?: kotlin.run {
                    animationNotifyPostsBounceAndFadeOut()
                }
                setAdapterToRecyclerFeed(linearLayoutManager)

                mHasPullRefresh = false
                viewModel.setIntention(FeedFragmentEvent.Idle)
            }

            is FeedState.StopRequestNewPosts -> {
                mHasPullRefresh = false

                feedState.messageUtil?.run {
                    Toast.makeText(
                        requireContext(),
                        message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
                stopSwipeRefresh()
                viewModel.setIntention(FeedFragmentEvent.Idle)
            }

            is FeedState.LoadOldPosts -> {
                displayProgressBarMorePosts(false)
                loadOldPosts(
                    feedState.listFeedPosts,
                    feedState.listFeedUsers,
                    feedState.scrollToPosition,
                    feedState.endOfTimeline
                )
                if (feedState.endOfTimeline)
                    viewModel.setIntention(FeedFragmentEvent.StopRequestOldPosts)
            }

            is FeedState.StopRequestOldPosts -> {
                Toast.makeText(
                    requireContext(),
                    "Sorry, there aren't more posts from the people you follow",
                    Toast.LENGTH_SHORT
                ).show()
                onScrollListenerHelper?.hasUserReachedBottomAndDraggingBefore = true
                viewModel.setIntention(FeedFragmentEvent.Idle)
            }

            is FeedState.LoadMessageZeroPosts -> {
                displayProgressBarInitialPosts(false)
                displayTextWelcomeMessage(true)
                recycler_feed.adapter
            }

            is FeedState.GoToPostDetailsFragment -> {
                goToPostDetailsFragment(feedState.post)
            }

            is FeedState.GoToProfileDetailsFragment -> {
                goToProfileDetailsFragment(feedState.user_id)
            }

            is FeedState.Loading -> {
                displayProgressBarInitialPosts(true)
            }
            is FeedState.LoadingMorePosts -> {
                displayProgressBarMorePosts(true)
            }

            is FeedState.Idle -> {
                Log.i(TAG, "IDLE")
            }

            is FeedState.SignOut -> {
//                findNavController().popBackStack(R.id.feedFragment, true)
//                findNavController().navigate(R.id.loginFragment)
                findNavController().navigate(R.id.action_feedFragment_to_loginFragment)
            }

            is FeedState.Error -> {
                Toast.makeText(requireContext(), feedState.message, Toast.LENGTH_SHORT).show()
            }
        }
    }



    private fun setUserDetails(user: User) {
        Log.i(TAG, "Setting user")

        Glide.with(requireView())
            .load(user.profileImageUrl)
            .into(feed_toolbar_image_profile)
    }

    private fun displayProgressBarInitialPosts(isDisplayed: Boolean) {
        feed_progress_bar_init.visibility = if (isDisplayed) View.VISIBLE else View.GONE
    }

    private fun displayTextWelcomeMessage(isDisplayed: Boolean) {
        feed_box_welcome.visibility = if (isDisplayed) View.VISIBLE else View.GONE
    }

    private fun displayProgressBarMorePosts(isDisplayed: Boolean) {
        feed_progress_bar_more_posts.visibility = if (isDisplayed) View.VISIBLE else View.GONE
    }

    private fun displayTextNotifyMorePosts(isDisplayed: Boolean) {
        feed_text_notify_new_posts.visibility = if (isDisplayed) View.VISIBLE else View.GONE

    }

    private fun stopSwipeRefresh() {
        feed_swipe_refresh_layout.isRefreshing = false
    }

    private fun setAdapterToRecyclerFeed(linearLayoutManager: LinearLayoutManager) {
        onScrollListenerHelper = OnScrollListenerHelper(requireContext(), this, this)
//        onScrollListenerHelper?.hasUserReachedBottomAndDraggingBefore = false

        recycler_feed.apply {
            layoutManager = linearLayoutManager
            adapter = postAdapter
            addOnScrollListener(onScrollListenerHelper!!)
            setHasFixedSize(true)
        }
    }

    private fun loadOldPosts(listPosts: List<Post>, listUsers: List<User>, scrollToPosition: Int, endOfTimeline: Boolean) {
        postAdapter.setUsers(listUsers.toMutableList())
        postAdapter.setPosts(listPosts.toMutableList())
        onScrollListenerHelper?.hasUserReachedBottomAndDraggingBefore = endOfTimeline

        recycler_feed.apply {
            adapter = postAdapter
            scrollToPosition(scrollToPosition)
        }
    }

    private fun setupListeners() {
        fab_to_post_add.setOnClickListener {
            goToPostAdd()
        }

        feed_swipe_refresh_layout.setOnRefreshListener {
            if (!mHasPullRefresh) {
                mHasPullRefresh = true
                viewModel.setIntention(FeedFragmentEvent.RetrieveNewFeedPosts)
            }
        }

        feed_text_notify_new_posts.setOnClickListener {
            val animation = AnimationUtils.loadAnimation(context, R.anim.fade_out)
            feed_text_notify_new_posts.startAnimation(animation)
            recycler_feed.smoothScrollToPosition(0)
            displayTextNotifyMorePosts(false)
//            feed_text_notify_new_posts.clearAnimation()

        }

        feed_fragment_toolbar_exit_app.setOnClickListener {
            viewModel.setIntention(FeedFragmentEvent.SignOut)
        }
    }

    private fun setupView() {
        feed_swipe_refresh_layout.setProgressBackgroundColorSchemeColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.background
            )
        )
        feed_swipe_refresh_layout.setColorSchemeColors(
            ContextCompat.getColor(
                requireContext(),
                R.color.blueTwitter
            )
        )
    }

    private fun goToPostAdd() {
        val navController = findNavController()
        navController.navigate(R.id.action_feedFragment_to_postAddFragment)
    }

    private fun animationNotifyPostsBounceAndFadeOut(){
        displayTextNotifyMorePosts(true)
        animation = AnimationUtils.loadAnimation(context, R.anim.bounce_animation)
        feed_text_notify_new_posts.startAnimation(animation)
        feed_text_notify_new_posts.postOnAnimationDelayed(650) {
            animation = AnimationUtils.loadAnimation(context, R.anim.fade_out)
            feed_text_notify_new_posts.startAnimation(animation)
            displayTextNotifyMorePosts(false)
        }
    }

    private fun animationNotifyBounce(){
        isBounceAnimationVisible = true
        animation = AnimationUtils.loadAnimation(context, R.anim.bounce_animation)
        feed_text_notify_new_posts.startAnimation(animation)
    }

    override fun clickListenerOnPost(positionAdapter: Int) {
        viewModel.setIntention(FeedFragmentEvent.GoToPostDetailsFragment(positionAdapter))
    }

    private fun goToPostDetailsFragment(post: Post) {
        val bundle = bundleOf("post" to post)
        val navController = findNavController()
        navController.navigate(R.id.action_feedFragment_to_postDetailFragment, bundle)
        viewModel.setIntention(FeedFragmentEvent.Idle)
    }

    override fun clickListenerOnUser(positionAdapter: Int) {
        viewModel.setIntention(FeedFragmentEvent.GoToProfileDetailsFragment(positionAdapter))
    }

    private fun goToProfileDetailsFragment(user_id: String) {
        val navController = findNavController()
        val bundle_user_id = bundleOf("user_id" to user_id)
        navController.navigate(R.id.action_feedFragment_to_profileFragment, bundle_user_id)
    }

    override fun requestMorePosts(actualRecyclerViewPosition: Int) {
        viewModel.setIntention(FeedFragmentEvent.RetrieveOldFeedPosts(actualRecyclerViewPosition))
    }

    override fun onMove() {
        if (isBounceAnimationVisible) {
            isBounceAnimationVisible = false

            animation = AnimationUtils.loadAnimation(context, R.anim.fade_out)
            feed_text_notify_new_posts.startAnimation(animation)
            feed_text_notify_new_posts.postOnAnimationDelayed(600) {
                displayTextNotifyMorePosts(false)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.i("CheckFirebaseBug", "We on resume check everything")
    }
}

sealed class FeedFragmentEvent {
    object GetUserPicture : FeedFragmentEvent()

    object RetrieveInitPosts : FeedFragmentEvent()
    object RetrieveNewFeedPosts : FeedFragmentEvent()
    data class RetrieveOldFeedPosts(val actualRecyclerViewPosition: Int) : FeedFragmentEvent()
    object RetrieveSavedLocalPosts : FeedFragmentEvent()

    object StopRequestOldPosts : FeedFragmentEvent()

    data class GoToPostDetailsFragment(val positionAdapter: Int) : FeedFragmentEvent()
    data class GoToProfileDetailsFragment(val positionAdapter: Int) : FeedFragmentEvent()

    object Idle : FeedFragmentEvent()
    object SignOut: FeedFragmentEvent()
}