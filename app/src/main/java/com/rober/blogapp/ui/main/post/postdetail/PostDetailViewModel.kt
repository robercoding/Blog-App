package com.rober.blogapp.ui.main.post.postdetail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.rober.blogapp.entity.Post
import com.rober.blogapp.util.state.DataState

class PostDetailViewModel: ViewModel() {

    private val _post : MutableLiveData<PostDetailState> = MutableLiveData()

    val post: LiveData<PostDetailState>
        get() = _post

    fun setIntention(event: PostDetailFragmentEvent){
        when(event){
            is PostDetailFragmentEvent.SetPost -> {
                _post.value = PostDetailState.SuccessPost(event.post)
            }
            is PostDetailFragmentEvent.AddLike -> {

            }

            is PostDetailFragmentEvent.GoBackToPreviousFragment -> {
                _post.value = PostDetailState.BackToPreviousFragment
            }
        }
    }
}