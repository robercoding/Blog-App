package com.rober.blogapp.ui.main.post.postadd

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rober.blogapp.data.ResultData
import com.rober.blogapp.data.network.repository.FirebaseRepository
import com.rober.blogapp.entity.Post
import com.rober.blogapp.util.MessageUtil
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class PostAddViewModel @ViewModelInject constructor(
    private val firebaseRepository: FirebaseRepository
): ViewModel() {

    private val _statePost: MutableLiveData<PostAddState> = MutableLiveData()

    val statePost : LiveData<PostAddState>
        get() = _statePost

    init {
    }

    fun setIntention(event: PostAddEvent){
        when(event) {
            is PostAddEvent.SavePost -> {
                savePost(event.post)
            }

            is PostAddEvent.ReadyToWrite -> {
                _statePost.value = PostAddState.ReadyToWrite
            }

            PostAddEvent.Idle -> {
                _statePost.value = PostAddState.Idle
            }
        }
    }

    private fun savePost(post: Post){
        viewModelScope.launch {
            firebaseRepository.savePost(post)
                .collect {resultData ->
                    when(resultData){
                        is ResultData.Success ->{
                            _statePost.value = PostAddState.PostHasBeenSaved(MessageUtil("Post has been succesfully saved!"))
                        }
                        is ResultData.Error -> {
                            _statePost.value = PostAddState.Error(resultData.exception)
                        }
                        is ResultData.Loading -> {
                            _statePost.value = PostAddState.Idle
                        }
                    }
                }
        }
    }





}