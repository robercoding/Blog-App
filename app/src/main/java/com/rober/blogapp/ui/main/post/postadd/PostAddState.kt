package com.rober.blogapp.ui.main.post.postadd

import com.rober.blogapp.util.MessageUtil

sealed class PostAddState {

    data class PostHasBeenSaved(val messageUtil: MessageUtil): PostAddState()

    data class Error(val exception: Exception): PostAddState()
    object Idle: PostAddState()

}