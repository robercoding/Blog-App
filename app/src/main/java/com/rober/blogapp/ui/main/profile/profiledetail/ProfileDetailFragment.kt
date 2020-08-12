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
import kotlinx.android.synthetic.main.fragment_profile_detail.*

@AndroidEntryPoint
class ProfileFragment : Fragment(), RecyclerViewActionInterface{
    private val TAG ="ProfileFragment"

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
        getUserAndSetIntention()
    }

    private fun subscribeObservers(){
        profileDetailViewModel.profileDetailState.observe(viewLifecycleOwner, Observer { profileDetailState ->
            render(profileDetailState)
        })
    }

    private fun getUserAndSetIntention(){
        val userName = arguments?.getString("user_id")

        if(userName.isNullOrBlank()){
            profileDetailViewModel.setIntention(ProfileDetailFragmentEvent.loadUserDetails(null))
        }else{
            profileDetailViewModel.setIntention(ProfileDetailFragmentEvent.loadUserDetails(userName))
        }
    }

    private fun render(profileDetailState: ProfileDetailState){
        Log.i("ProfileViewModel", "${profileDetailState}")
        when(profileDetailState){
            is ProfileDetailState.SetUserProfile -> {
                Log.i("ProfileDetailViewModel", "Receive1!!")
                val user = profileDetailState.user
                setUserProfile(user)
                profileDetailViewModel.setIntention(ProfileDetailFragmentEvent.loadUserPosts(null))
            }
            is ProfileDetailState.SetUserPosts -> {
                val listUserPosts = profileDetailState.listUserPosts
                setUserPosts(listUserPosts.toMutableList())

            }

            is ProfileDetailState.SetOtherUserProfile ->{
                setOtherUserProfile(profileDetailState.user)

            }
            is ProfileDetailState.LoadingPosts -> {
                displayProgressBar(true)
            }
        }

    }



    private fun setUserProfile(user: User){
        Log.i("ProfileDetailViewModel", "Setting here ${user.username}")

        uid_name.text = "@${user.username}"
        uid_biography.text = user.biography
        uid_followers.text = "${user.follower} Follower"
        uid_following.text = "${user.following} Following"

        if(user.follower > 1)
            uid_followers.append("s")
    }


    //TODO HERE change edit profile by follow/following button
    private fun setOtherUserProfile(user: User){
        uid_name.text = "@${user.username}"
        uid_biography.text = user.biography
        uid_followers.text = "${user.follower} Follower"
        uid_following.text = "${user.following} Following"

        if(user.follower > 1)
            uid_followers.append("s")
    }

    //TODO HERE CHANGE VIEWHOLDER LAYOUT TO BE ABLE TO DELETE POSTS
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