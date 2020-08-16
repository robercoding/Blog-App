package com.rober.blogapp.ui.main.feed

import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rober.blogapp.R
import com.rober.blogapp.entity.Post
import com.rober.blogapp.ui.main.feed.adapter.PostAdapter
import com.rober.blogapp.util.RecyclerViewActionInterface
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_feed.*

@AndroidEntryPoint
class FeedFragment : Fragment(), RecyclerViewActionInterface {


    private val TAG: String = "FeedFragment"

    private var mHasPullRefresh = false
    private var resource: Int = R.layout.adapter_feed_viewholder_posts

    private val viewModel: FeedViewModel by viewModels()
    lateinit var postAdapter: PostAdapter



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_feed, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        postAdapter = PostAdapter(requireView(), resource, this)

        subscribeObservers()
        viewModel.setIntention(FeedFragmentEvent.RetrieveInitPosts)
    }

    private fun subscribeObservers(){
        viewModel.feedState.observe(viewLifecycleOwner, Observer {feedState ->
            render(feedState)
        })
    }

    private fun render(feedState: FeedState){
        when(feedState){
            is FeedState.SetListPosts -> {
                displayProgressBar(false)
                stopSwipeRefresh()

                postAdapter.setPosts(feedState.listFeedPosts.toMutableList())

                val linearLayoutManager = LinearLayoutManager(requireContext())

                setAdapterToRecyclerFeed(linearLayoutManager)
                viewModel.setIntention(FeedFragmentEvent.Idle)
            }

            is FeedState.LoadNewPosts -> {
                loadNewPosts(feedState.listFeedPosts, feedState.scrollToPosition)
            }

            is FeedState.GoToPostDetails -> {
                goToPostDetails(feedState.post)
            }

            is FeedState.Loading -> {
                displayProgressBar(true)
            }

            is FeedState.Idle -> {}

            is FeedState.Error -> {
                Toast.makeText(requireContext(), feedState.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun displayProgressBar(isDisplayed: Boolean){
        progressbar.visibility = if(isDisplayed) View.VISIBLE else View.GONE
    }

    private fun stopSwipeRefresh(){
        swipe_refresh_layout.isRefreshing = false
    }

    private fun setAdapterToRecyclerFeed(linearLayoutManager: LinearLayoutManager){
        val onScrollListenerHelper = OnScrollListenerHelper(requireContext(), this)

        recycler_feed.apply {
            layoutManager = linearLayoutManager
            adapter = postAdapter
            addOnScrollListener(onScrollListenerHelper)
            setHasFixedSize(true)
        }
    }

    private fun loadNewPosts(listPosts: List<Post>, scrollToPosition: Int){
        postAdapter.setPosts(listPosts.toMutableList())

        recycler_feed.apply {
            adapter = postAdapter
            scrollToPosition(scrollToPosition)
        }

    }

    private fun setupListeners(){
        fab_to_post_add.setOnClickListener {
            goToPostAdd()
        }

        swipe_refresh_layout.setOnRefreshListener {
            mHasPullRefresh = true
            //viewModel.setIntention(FeedFragmentEvent.RetrieveNewFeedPosts)
        }
    }

    private fun goToPostAdd(){
        val navController = findNavController()
        navController.navigate(R.id.action_feedFragment_to_postAddFragment)
    }

    override fun clickListenerOnPost(positionAdapter: Int) {
        //val post = mutableListPosts[positionAdapter]
        viewModel.setIntention(FeedFragmentEvent.GoToPostDetails(positionAdapter))
    }

    private fun goToPostDetails(post: Post){
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

        viewModel.setIntention(FeedFragmentEvent.RetrieveNewFeedPosts(actualRecyclerViewPosition))
    }
}
sealed class FeedFragmentEvent{
    object RetrieveInitPosts: FeedFragmentEvent()
    data class RetrieveNewFeedPosts(val actualRecyclerViewPosition: Int): FeedFragmentEvent()
    object RetrieveOldFeedPosts : FeedFragmentEvent()

    data class GoToPostDetails(val positionAdapter: Int): FeedFragmentEvent()

    object Idle: FeedFragmentEvent()
}