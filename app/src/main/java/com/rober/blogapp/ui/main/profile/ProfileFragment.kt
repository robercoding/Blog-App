package com.rober.blogapp.ui.main.profile

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
import com.rober.blogapp.util.state.DataState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_feed.*
import kotlinx.android.synthetic.main.fragment_profile.*

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private val viewModel: ProfileViewModel by viewModels()
    lateinit var postAdapter: PostAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        postAdapter = PostAdapter(requireView())
        subscribeObservers()

        var userName = arguments?.getString("userName")
        if(userName.isNullOrBlank()){
            Log.i("ProfileFragment", "Let's load our user")
            viewModel.setIntention(ProfileFragmentEvent.loadUserDetails(null))


        }else{
            Log.i("ProfileFragment", "Let's load other user")
        }
    }

    private fun subscribeObservers(){
        viewModel.profileUserState.observe(viewLifecycleOwner, Observer {dataState->
            when(dataState){
                is DataState.Success -> {
                    val user = dataState.data
                    uid_name.text = user.username
                    uid_biography.text = user.biography
                    uid_followers.text = "20 following"
                    uid_following.text = "30 followers"
                }

                is DataState.Loading -> {
                    displayProgressBar(true)
                }
            }

        })

        viewModel.profileUserListState.observe(viewLifecycleOwner, Observer {dataState ->
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
}


sealed class ProfileFragmentEvent{
    data class loadUserDetails(val name: String? = null): ProfileFragmentEvent()
}