package com.rober.blogapp.ui.main.post.postdetail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel


class PostDetailViewModel: ViewModel() {

    private val _postDetailState : MutableLiveData<PostDetailState> = MutableLiveData()

    val postDetailState: LiveData<PostDetailState>
        get() = _postDetailState

    fun setIntention(event: PostDetailFragmentEvent){
        when(event){
            is PostDetailFragmentEvent.SetPost -> {
                _postDetailState.value = PostDetailState.SetPostDetails(event.post)
            }
            is PostDetailFragmentEvent.AddLike -> {

            }

            is PostDetailFragmentEvent.GoBackToPreviousFragment -> {
                _postDetailState.value = PostDetailState.BackToPreviousFragment
            }
        }
    }
}