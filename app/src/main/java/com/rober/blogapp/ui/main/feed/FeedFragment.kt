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
import com.rober.blogapp.util.RecyclerViewClickInterface
import com.rober.blogapp.util.state.FeedState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_feed.*

@AndroidEntryPoint
class FeedFragment : Fragment(), RecyclerViewClickInterface{


    private val TAG: String = "FeedFragment"
    private var mHasReachedBottomonce = false
    private var mHasPullRefresh = false

    private var resource: Int = R.layout.adapter_feed_viewholder_posts

    private var recyclerViewState: Parcelable? = null

    private val viewModel: FeedViewModel by viewModels()
    lateinit var postAdapter: PostAdapter

    private var mutableListPosts = mutableListOf<Post>()

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
        postAdapter = PostAdapter(requireView(), R.layout.adapter_feed_viewholder_posts, this)


        subscribeObservers()
    }

    private fun subscribeObservers(){
        viewModel.feedState.observe(viewLifecycleOwner, Observer {dataState ->
            Log.i(TAG, "$dataState")

            when(dataState){
                is FeedState.SuccessListPostState -> {
                    postAdapter.setPosts(dataState.data.toMutableList())
                    mutableListPosts = dataState.data.toMutableList()

                    val linearLayoutManager = LinearLayoutManager(requireContext())

                    Toast.makeText(requireContext(), "Stop refreshing", Toast.LENGTH_SHORT).show()
                    displayProgressBar(false)
                    swipe_refresh_layout.isRefreshing = false

                    recycler_feed.apply {
                        if(recyclerViewState != null && !mHasPullRefresh){
                            layoutManager!!.onRestoreInstanceState(recyclerViewState)
                            mHasPullRefresh = false
                        }else
                            layoutManager = linearLayoutManager

                        adapter = postAdapter
                        setHasFixedSize(true)
                        addOnScrollListener(object: RecyclerView.OnScrollListener() {
                            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                                super.onScrolled(recyclerView, dx, dy)

                                if((linearLayoutManager.findLastVisibleItemPosition() == linearLayoutManager.itemCount -1) && !mHasReachedBottomonce){
                                    recyclerViewState = linearLayoutManager.onSaveInstanceState()
                                    viewModel.setIntention(FeedFragmentEvent.RetrieveFeedPosts(true))
                                    mHasReachedBottomonce = true
                                }

                            }
                            override fun onScrollStateChanged(
                                recyclerView: RecyclerView,
                                newState: Int
                            ) {
                                super.onScrollStateChanged(recyclerView, newState)
                            }
                        })
                    }
                }
                is FeedState.Error -> {
                    Toast.makeText(requireContext(), dataState.message, Toast.LENGTH_SHORT).show()
                }


                is FeedState.GettingPostState -> {
                    displayProgressBar(true)

                }
            }
        })
    }

    private fun displayProgressBar(isDisplayed: Boolean){
        progressbar.visibility = if(isDisplayed) View.VISIBLE else View.GONE
    }

    private fun setupListeners(){
        fab_to_post_add.setOnClickListener {
            goToPostAdd()
        }

        swipe_refresh_layout.setOnRefreshListener {
            mHasPullRefresh = true
            viewModel.setIntention(FeedFragmentEvent.RetrieveFeedPosts(true))
            Toast.makeText(requireContext(), "Request more posts", Toast.LENGTH_SHORT).show()
        }
    }

    private fun goToPostAdd(){
        val navController = findNavController()
        navController.navigate(R.id.postAddFragment)
    }

    override fun clickListenerOnPost(positionAdapter: Int) {
        val post = mutableListPosts[positionAdapter]

        val bundle = bundleOf("post" to post)
        val navController = findNavController()
        navController.navigate(R.id.postDetailFragment, bundle)
    }

    override fun clickListenerOnUser(positionAdapter: Int) {
        val user_id = mutableListPosts[positionAdapter].user_creator_id

        val navController = findNavController()
        val bundle_user_id = bundleOf("user_id" to user_id)
        navController.navigate(R.id.profileDetailFragment, bundle_user_id)
    }

}
sealed class FeedFragmentEvent{
    data class RetrieveFeedPosts(val morePosts: Boolean) : FeedFragmentEvent()
}