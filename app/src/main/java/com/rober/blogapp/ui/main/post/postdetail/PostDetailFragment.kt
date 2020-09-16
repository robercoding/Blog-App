package com.rober.blogapp.ui.main.post.postdetail

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.rober.blogapp.R
import com.rober.blogapp.entity.Post
import com.rober.blogapp.entity.User
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_post_detail.*

@AndroidEntryPoint
class PostDetailFragment : Fragment() {

    private val viewModel: PostDetailViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_post_detail, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        setupListeners()
        setupObservers()

        val post = arguments?.getParcelable<Post>("post")

        if(post == null){
            viewModel.setIntention(PostDetailFragmentEvent.GoBackToPreviousFragment)
        }else{
            viewModel.setIntention(PostDetailFragmentEvent.SetPost(post))
        }
    }

    private fun setupObservers(){
        viewModel.postDetailState.observe(viewLifecycleOwner, Observer {postDetailState ->
            render(postDetailState)
        })
    }

    private fun render(postDetailState: PostDetailState){
        when(postDetailState){
            is PostDetailState.SetPostDetails -> {
                setPostDetails(postDetailState.post)
                setUserDetails(postDetailState.user)
            }
            is PostDetailState.BackToPreviousFragment -> {
                moveToFeedFragment()
            }
        }
    }

    private fun setupListeners(){
        post_detail_toolbar.setNavigationOnClickListener {
            viewModel.setIntention(PostDetailFragmentEvent.GoBackToPreviousFragment)
        }
        post_detail_text.movementMethod = ScrollingMovementMethod()
    }

    private fun setPostDetails(post: Post){
        //post_detail_image_profile.background = post.
        post_detail_heart_number.text = "${post.likes}"
        post_detail_text.text = post.text
        post_detail_title.text = post.title
//        post_detail_username.text = post.userCreatorId
    }

    private fun setUserDetails(user: User){
        post_detail_username.text = "@${user.username}"

        val imageProfile: Any = if(user.profileImageUrl.isEmpty())
            R.drawable.outline_account_circle_black_24dp
        else
            user.profileImageUrl

        Glide.with(requireView())
            .load(imageProfile)
            .into(post_detail_image_profile)
    }

    private fun moveToFeedFragment(){
        val navController = findNavController()
        navController.popBackStack()
    }
}

sealed class PostDetailFragmentEvent {
    data class SetPost(val post: Post) : PostDetailFragmentEvent()
    object AddLike : PostDetailFragmentEvent()
    object AddRepost : PostDetailFragmentEvent()

    object GoBackToPreviousFragment : PostDetailFragmentEvent()
}