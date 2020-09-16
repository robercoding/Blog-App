package com.rober.blogapp.ui.main.post.postdetail

import android.util.Log
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rober.blogapp.data.ResultData
import com.rober.blogapp.data.network.repository.FirebaseRepository
import com.rober.blogapp.entity.Post
import com.rober.blogapp.entity.User
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch


class PostDetailViewModel @ViewModelInject constructor(val firebaseRepository: FirebaseRepository) : ViewModel() {

    private val _postDetailState: MutableLiveData<PostDetailState> = MutableLiveData()

    val postDetailState: LiveData<PostDetailState>
        get() = _postDetailState

    fun setIntention(event: PostDetailFragmentEvent) {
        when (event) {
            is PostDetailFragmentEvent.SetPost -> {
                setPost(event.post, event.post.userCreatorId)
            }
            is PostDetailFragmentEvent.AddLike -> {
            }

            is PostDetailFragmentEvent.GoBackToPreviousFragment -> {
                _postDetailState.value = PostDetailState.BackToPreviousFragment
            }
        }
    }

    private fun setPost(post: Post, userUID: String) {
        viewModelScope.launch {
            val user = getUserProfile(userUID)

            user?.also { tempUser ->
                _postDetailState.value = PostDetailState.SetPostDetails(post, tempUser)
            } ?: kotlin.run {
                _postDetailState.value = PostDetailState.BackToPreviousFragment
            }
        }
    }

    private suspend fun getUserProfile(userUID: String): User? {
        var tempUser: User? = null
        val job = viewModelScope.launch {
            firebaseRepository.getUserProfile(userUID)
                .collect { resultData ->
                    when (resultData) {
                        is ResultData.Success -> tempUser = resultData.data!!

                    }
                }
        }
        job.join()
        return tempUser

    }
}