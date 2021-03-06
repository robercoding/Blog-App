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
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rober.blogapp.R
import com.rober.blogapp.entity.Post
import com.rober.blogapp.entity.User
import com.rober.blogapp.ui.base.BaseFragment
import com.rober.blogapp.ui.main.feed.adapter.PostAdapter
import com.rober.blogapp.util.EmojiUtils.ANGUISHED_FACE
import com.rober.blogapp.util.RecyclerViewActionInterface
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_feed.*

@AndroidEntryPoint
class FeedFragment : BaseFragment<FeedState, FeedFragmentEvent, FeedViewModel>(R.layout.fragment_feed),
    RecyclerViewActionInterface, OnMoveRecyclerListener {

    private var mHasPullRefresh = false
    private var resource: Int = R.layout.adapter_feed_viewholder_posts

    override val viewModel: FeedViewModel by viewModels()
    lateinit var postAdapter: PostAdapter

    private lateinit var animation: Animation
    private var isBounceAnimationVisible = false

    private var onScrollListenerHelper: OnScrollListenerHelper? = null


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViewDesign()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        animation = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_out)
        viewModel.setIntention(FeedFragmentEvent.GetUserPicture)

        postAdapter = PostAdapter(requireView(), resource, this)
    }

    override fun render(viewState: FeedState) {
        Log.i("States", "State = ${viewState}")
        when (viewState) {

            is FeedState.SetUserDetails -> {
                setUserDetails(viewState.user)
                viewModel.setIntention(FeedFragmentEvent.RetrieveInitPosts)
            }

            is FeedState.SetListPosts -> {
                displayTextWelcomeMessage(false)
                displayProgressBarInitialPosts(false)
                stopSwipeRefresh()

                postAdapter.setUsers(viewState.listFeedUsers.toMutableList())
                postAdapter.setPosts(viewState.listFeedPosts.toMutableList())

                val linearLayoutManager = LinearLayoutManager(requireContext())

                setAdapterToRecyclerFeed(linearLayoutManager)
                viewModel.setIntention(FeedFragmentEvent.Idle)
            }

            is FeedState.LoadNewPosts -> {
                stopSwipeRefresh()
                displayTextNotifyMorePosts(true)
                displayTextWelcomeMessage(false)

                postAdapter.setUsers(viewState.listFeedUsers.toMutableList())
                postAdapter.setPosts(viewState.listFeedPosts.toMutableList())

                val linearLayoutManager = getLinearLayoutManagerFromRecycler(recycler_feed)
                setAnimationNotifyWithLinearLayout(linearLayoutManager)
                setAdapterToRecyclerFeed(linearLayoutManager)

                mHasPullRefresh = false
                viewModel.setIntention(FeedFragmentEvent.Idle)
            }

            is FeedState.StopRequestNewPosts -> {
                mHasPullRefresh = false

                viewState.messageUtil?.run {
                    displayToast(message + getEmoji(ANGUISHED_FACE))
                }
                stopSwipeRefresh()
                viewModel.setIntention(FeedFragmentEvent.Idle)
            }

            is FeedState.LoadOldPosts -> {
                displayProgressBarMorePosts(false)
                loadOldPosts(
                    viewState.listFeedPosts,
                    viewState.listFeedUsers,
                    viewState.scrollToPosition,
                    viewState.endOfTimeline
                )
                if (viewState.endOfTimeline)
                    viewModel.setIntention(FeedFragmentEvent.StopRequestOldPosts)
            }

            is FeedState.StopRequestOldPosts -> {
                displayToast("Sorry, there aren't more posts from the people you follow")
                onScrollListenerHelper?.hasUserReachedBottomAndDraggingBefore = true
                viewModel.setIntention(FeedFragmentEvent.Idle)
            }

            is FeedState.LoadMessageZeroPosts -> {
                displayProgressBarInitialPosts(false)
                displayTextWelcomeMessage(true)
                recycler_feed.adapter
            }

            is FeedState.GoToPostDetailsFragment -> {
                goToPostDetailsFragment(viewState.post)
            }

            is FeedState.GoToProfileDetailsFragment -> {
                goToProfileDetailsFragment(viewState.userId)
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
                findNavController().navigate(R.id.action_feedFragment_to_loginFragment)
            }

            is FeedState.Error -> {
                viewState.message?.let { displayToast(it) }
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

    private fun loadOldPosts(
        listPosts: List<Post>,
        listUsers: List<User>,
        scrollToPosition: Int,
        endOfTimeline: Boolean
    ) {
        postAdapter.setUsers(listUsers.toMutableList())
        postAdapter.setPosts(listPosts.toMutableList())
        onScrollListenerHelper?.hasUserReachedBottomAndDraggingBefore = endOfTimeline

        recycler_feed.apply {
            adapter = postAdapter
            scrollToPosition(scrollToPosition)
        }
    }

    override fun setupListeners() {
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

    override fun setupViewDesign() {
        feed_swipe_refresh_layout.setProgressBackgroundColorSchemeColor(
            ContextCompat.getColor(
                requireContext(),
                R.color.primaryBackground
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

    private fun getLinearLayoutManagerFromRecycler(recyclerView: RecyclerView): LinearLayoutManager {
        var linearLayoutManager = LinearLayoutManager(requireContext())
        recyclerView.layoutManager?.run {
            linearLayoutManager = this as LinearLayoutManager
        }

        return linearLayoutManager
    }

    private fun setAnimationNotifyWithLinearLayout(linearLayoutManager: LinearLayoutManager) {
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
    }

    private fun animationNotifyPostsBounceAndFadeOut() {
        displayTextNotifyMorePosts(true)
        animation = AnimationUtils.loadAnimation(context, R.anim.bounce_animation)
        feed_text_notify_new_posts.startAnimation(animation)
        feed_text_notify_new_posts.postOnAnimationDelayed(650) {
            animation = AnimationUtils.loadAnimation(context, R.anim.fade_out)
            feed_text_notify_new_posts.startAnimation(animation)
            displayTextNotifyMorePosts(false)
        }
    }

    private fun animationNotifyBounce() {
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

    override fun clickListenerOnItem(positionAdapter: Int) {
        //
    }

    private fun goToProfileDetailsFragment(userId: String) {
        val navController = findNavController()
        val bundleUserId = bundleOf("userId" to userId)
        navController.navigate(R.id.action_feedFragment_to_profileFragment, bundleUserId)
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
    object SignOut : FeedFragmentEvent()
}