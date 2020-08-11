package com.rober.blogapp.ui.main.post.postdetail

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.rober.blogapp.R
import com.rober.blogapp.entity.Post
import com.rober.blogapp.util.state.DataState
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
        viewModel.post.observe(viewLifecycleOwner, Observer {postDetailState ->
            when(postDetailState){
                is PostDetailState.SuccessPost<*> -> {
                    setPostDetails(postDetailState.data as Post)
                }
                is PostDetailState.BackToPreviousFragment -> {
                    moveToFeedFragment()
                }
            }
        })
    }

    private fun setupListeners(){
        post_detail_toolbar.setNavigationOnClickListener {
            viewModel.setIntention(PostDetailFragmentEvent.GoBackToPreviousFragment)
        }
    }

    private fun setPostDetails(post: Post){
        //post_detail_image_profile.background = post.
        post_detail_heart.text = "${post.likes.toString()} Likes"
        post_detail_text.text = post.text
        post_detail_title.text = post.title
        post_detail_username.text = post.user_creator_id
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