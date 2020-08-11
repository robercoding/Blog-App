package com.rober.blogapp.ui.main.post.postdetail

import java.lang.Exception

sealed class PostDetailState {
    data class SuccessPost<out T>(val data: T) : PostDetailState()
    data class Error<out T>(val exception: Exception): PostDetailState()
    object Loading: PostDetailState()

    object BackToPreviousFragment: PostDetailState()
}