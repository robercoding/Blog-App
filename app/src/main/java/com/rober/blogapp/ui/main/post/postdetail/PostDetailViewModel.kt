package com.rober.blogapp.ui.main.post.postdetail

import android.app.Application
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rober.blogapp.R
import com.rober.blogapp.data.ResultData
import com.rober.blogapp.data.network.repository.FirebaseRepository
import com.rober.blogapp.entity.Post
import com.rober.blogapp.entity.User
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch


class PostDetailViewModel @ViewModelInject constructor(
    val firebaseRepository: FirebaseRepository,
    val application: Application
) : ViewModel() {

    private val _postDetailState: MutableLiveData<PostDetailState> = MutableLiveData()

    val postDetailState: LiveData<PostDetailState>
        get() = _postDetailState

    var user: User? = null

    fun setIntention(event: PostDetailFragmentEvent) {
        when (event) {
            is PostDetailFragmentEvent.SetPost -> {
                setPost(event.post, event.post.userCreatorId)
            }
            is PostDetailFragmentEvent.AddLike -> {
            }

            is PostDetailFragmentEvent.GoToProfileFragment -> {
                user?.run {
                    _postDetailState.value = PostDetailState.GoToProfileFragment(this)
                } ?: kotlin.run { _postDetailState.value = PostDetailState.Idle }
            }

            is PostDetailFragmentEvent.GoBackToPreviousFragment -> {
                _postDetailState.value = PostDetailState.BackToPreviousFragment
            }

            is PostDetailFragmentEvent.ShowPostOptions -> {
                _postDetailState.value = PostDetailState.ShowPostOptions(
                    application.applicationContext.resources.getStringArray(
                        R.array.list_post_detail_options
                    ).toList()
                )
            }
        }
    }

    private fun setPost(post: Post, userUID: String) {
        viewModelScope.launch {
            val userProfile = getUserProfile(userUID)

            userProfile?.also { tempUserProfile ->
                user = tempUserProfile
                _postDetailState.value = PostDetailState.SetPostDetails(post, tempUserProfile)
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