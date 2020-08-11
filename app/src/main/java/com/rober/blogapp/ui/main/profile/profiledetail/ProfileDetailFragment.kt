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
import com.rober.blogapp.ui.main.feed.adapter.PostAdapter
import com.rober.blogapp.util.RecyclerViewClickInterface
import com.rober.blogapp.util.state.DataState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_profile_detail.*

@AndroidEntryPoint
class ProfileFragment : Fragment(), RecyclerViewClickInterface{
    private val TAG ="ProfileFragment"

    private val detailViewModel: ProfileDetailViewModel by viewModels()
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

        Log.i(TAG, "ACTIVITY CREATED")
        var userName = arguments?.getString("user_id")
        if(userName.isNullOrBlank()){
            Log.i("ProfileFragment", "Let's load our user")
            detailViewModel.setIntention(
                ProfileFragmentEvent.loadUserDetails(
                    null
                )
            )
        }else{
            detailViewModel.setIntention(
                ProfileFragmentEvent.loadUserDetails(
                    userName
                )
            )
        }
    }

    private fun subscribeObservers(){
        detailViewModel.profileUserState.observe(viewLifecycleOwner, Observer { dataState->
            when(dataState){
                is DataState.Success -> {
                    val user = dataState.data
                    Log.i(TAG, "biography: ${user.biography}, name: ${user.username}, id: ${user.user_id} location: ${user.location}")
                    uid_name.text = "@${user.username}"
                    uid_biography.text = user.biography
                    uid_followers.text = "20 following"
                    uid_following.text = "30 followers"
                }

                is DataState.Loading -> {
                    displayProgressBar(true)
                }
            }

        })

        detailViewModel.profileUserListState.observe(viewLifecycleOwner, Observer { dataState ->
            when(dataState){
                is DataState.Success -> {
                    postAdapter.setPosts(dataState.data.toMutableList())
                    recycler_user_posts.apply {
                        displayProgressBar(false)
                        layoutManager = LinearLayoutManager(requireContext())
                        adapter = postAdapter
                    }
                }
                is DataState.Loading -> {
                    displayProgressBar(true)
                }
            }
        })
    }

    private fun displayProgressBar(isDisplayed: Boolean){
        progress_bar_profile_posts.visibility = if(isDisplayed) View.VISIBLE else View.GONE
    }

    override fun clickListenerOnPost(positionAdapter: Int) {
        //TODO
    }

    override fun clickListenerOnUser(positionAdapter: Int) {
        //TODO
    }
}


sealed class ProfileFragmentEvent{
    data class loadUserDetails(val name: String? = null): ProfileFragmentEvent()
}