package com.rober.blogapp.ui.main.post.postadd

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rober.blogapp.data.ResultData
import com.rober.blogapp.data.network.repository.FirebaseRepository
import com.rober.blogapp.entity.Post
import com.rober.blogapp.entity.User
import com.rober.blogapp.ui.base.BaseViewModel
import com.rober.blogapp.util.MessageUtil
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class PostAddViewModel @ViewModelInject constructor(
    private val firebaseRepository: FirebaseRepository
): BaseViewModel<PostAddState, PostAddEvent>() {
    
    var user : User? = null
    var post: Post? = null

    var isPostToUpdate = false

    override fun setIntention(event: PostAddEvent){
        when(event) {
            is PostAddEvent.LoadUserDetails -> getCurrentUser()

            is PostAddEvent.GetPostToEdit -> {
                viewState = PostAddState.GetPostToEdit
            }

            is PostAddEvent.RenderPostToEditInView -> {
                post = event.post
                isPostToUpdate = true
                viewState = PostAddState.RenderPostToEditInView(event.post)
            }

            is PostAddEvent.ReadyToWrite -> {
                viewState = PostAddState.ReadyToWrite
            }

            is PostAddEvent.ReadyToSaveOrUpdatePost -> {
                viewState = PostAddState.SaveOrUpdatePost(isPostToUpdate)
            }

            is PostAddEvent.UpdatePost ->{
                post?.also {tempPost->
                    tempPost.title = event.title
                    tempPost.text = event.text

                    viewState = PostAddState.GoToPostDetailAndUpdatePost(tempPost)
                }?: kotlin.run {
                    viewState = PostAddState.Idle
                }
            }

            is PostAddEvent.SavePost -> {
                savePost(event.post)
            }

            is PostAddEvent.NotifyErrorFieldValidation -> {
                viewState = PostAddState.NotifyErrorFieldValidation
            }

            PostAddEvent.Idle -> {
                viewState = PostAddState.Idle
            }
        }
    }

    private fun getCurrentUser() {
        user = firebaseRepository.getCurrentUser()

        user?.run {
            viewState = PostAddState.SetUserDetail(this)
        }
    }

    private fun savePost(post: Post){
        viewModelScope.launch {
            firebaseRepository.savePost(post)
                .collect {resultData ->
                    when(resultData){
                        is ResultData.Success ->{
                            viewState = PostAddState.PostHasBeenSaved(MessageUtil("Post has been succesfully saved!"))
                        }
                        is ResultData.Error -> {
                            viewState = PostAddState.Error(resultData.exception)
                        }
                        is ResultData.Loading -> {
                            viewState = PostAddState.Idle
                        }
                    }
                }
        }
    }





}