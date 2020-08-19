package com.rober.blogapp.ui.main.feed

import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.rober.blogapp.R
import com.rober.blogapp.entity.Post
import com.rober.blogapp.ui.MainActivity
import com.rober.blogapp.ui.main.feed.adapter.PostAdapter
import com.rober.blogapp.util.RecyclerViewActionInterface
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_feed.*
import org.joda.time.DateTime

@AndroidEntryPoint
class FeedFragment : Fragment(), RecyclerViewActionInterface {


    private val TAG: String = "FeedFragment"

    private var mHasPullRefresh = false
    private var resource: Int = R.layout.adapter_feed_viewholder_posts

    private val viewModel: FeedViewModel by viewModels()
    lateinit var postAdapter: PostAdapter

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
        viewModel.setIntention(FeedFragmentEvent.RetrieveInitPosts)
        postAdapter = PostAdapter(requireView(), resource, this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

    }

    private fun subscribeObservers() {
        viewModel.feedState.observe(viewLifecycleOwner, Observer { feedState ->
            render(feedState)
        })
    }

    private fun render(feedState: FeedState) {
        Log.i("States", "State = ${feedState}")
        when (feedState) {
            is FeedState.SetListPosts -> {
                displayProgressBarInitialPosts(false)
                stopSwipeRefresh()

                postAdapter.setPosts(feedState.listFeedPosts.toMutableList())

                val linearLayoutManager = LinearLayoutManager(requireContext())

                setAdapterToRecyclerFeed(linearLayoutManager)
                viewModel.setIntention(FeedFragmentEvent.Idle)
            }

            is FeedState.LoadNewPosts -> {
                stopSwipeRefresh()

                postAdapter.setPosts(feedState.listFeedPosts.toMutableList())

                recycler_feed.apply {
                    adapter = postAdapter
                    scrollToPosition(feedState.scrollToPosition)
                }
                mHasPullRefresh = false
                viewModel.setIntention(FeedFragmentEvent.Idle)
            }

            is FeedState.StopRequestNewPosts -> {
                mHasPullRefresh = false

                Toast.makeText(requireContext(), "${feedState.messageUtil.message}", Toast.LENGTH_SHORT).show()
                stopSwipeRefresh()
                viewModel.setIntention(FeedFragmentEvent.Idle)
            }

            is FeedState.LoadOldPosts -> {
                displayProgressBarMorePosts(false)
                loadOldPosts(
                    feedState.listFeedPosts,
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

            is FeedState.GoToPostDetails -> {
                goToPostDetails(feedState.post)
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

            is FeedState.Error -> {
                Toast.makeText(requireContext(), feedState.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun displayProgressBarInitialPosts(isDisplayed: Boolean) {
        feed_progress_bar_init.visibility = if (isDisplayed) View.VISIBLE else View.GONE
    }

    private fun displayProgressBarMorePosts(isDisplayed: Boolean) {
        feed_progress_bar_more_posts.visibility = if (isDisplayed) View.VISIBLE else View.GONE
    }

    private fun stopSwipeRefresh() {
        swipe_refresh_layout.isRefreshing = false
    }

    private fun setAdapterToRecyclerFeed(linearLayoutManager: LinearLayoutManager) {
        onScrollListenerHelper = OnScrollListenerHelper(requireContext(), this)
//        onScrollListenerHelper?.hasUserReachedBottomAndDraggingBefore = false

        recycler_feed.apply {
            layoutManager = linearLayoutManager
            adapter = postAdapter
            addOnScrollListener(onScrollListenerHelper!!)
            setHasFixedSize(true)
        }
    }

    private fun loadOldPosts(listPosts: List<Post>, scrollToPosition: Int, endOfTimeline: Boolean) {
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

        swipe_refresh_layout.setOnRefreshListener {
            if (!mHasPullRefresh) {
                mHasPullRefresh = true
                viewModel.setIntention(FeedFragmentEvent.RetrieveNewFeedPosts)
            }
        }
    }

    private fun setupView(){
        swipe_refresh_layout.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(requireContext(), R.color.background))
        swipe_refresh_layout.setColorSchemeColors(ContextCompat.getColor(requireContext(), R.color.blueTwitter))

    }

    private fun goToPostAdd() {
        val navController = findNavController()
        navController.navigate(R.id.action_feedFragment_to_postAddFragment)
    }

    override fun clickListenerOnPost(positionAdapter: Int) {
        //val post = mutableListPosts[positionAdapter]
        viewModel.setIntention(FeedFragmentEvent.GoToPostDetails(positionAdapter))
    }

    private fun goToPostDetails(post: Post) {
        val bundle = bundleOf("post" to post)
        val navController = findNavController()
        navController.navigate(R.id.postDetailFragment, bundle)
        viewModel.setIntention(FeedFragmentEvent.Idle)
    }

    override fun clickListenerOnUser(positionAdapter: Int) {
        //val user_id = mutableListPosts[positionAdapter].user_creator_id

//        val navController = findNavController()
//        val bundle_user_id = bundleOf("user_id" to user_id)
//        navController.navigate(R.id.action_feedFragment_to_profileFragment, bundle_user_id)
    }

    override fun requestMorePosts(actualRecyclerViewPosition: Int) {
        viewModel.setIntention(FeedFragmentEvent.RetrieveOldFeedPosts(actualRecyclerViewPosition))
    }
}

sealed class FeedFragmentEvent {
    object RetrieveInitPosts : FeedFragmentEvent()
    object RetrieveNewFeedPosts : FeedFragmentEvent()
    data class RetrieveOldFeedPosts(val actualRecyclerViewPosition: Int) : FeedFragmentEvent()
    object RetrieveSavedLocalPosts : FeedFragmentEvent()

    object StopRequestOldPosts : FeedFragmentEvent()

    data class GoToPostDetails(val positionAdapter: Int) : FeedFragmentEvent()

    object Idle : FeedFragmentEvent()
}