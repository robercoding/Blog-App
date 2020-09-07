package com.rober.blogapp.ui.main.post.postadd

import com.rober.blogapp.entity.User
import com.rober.blogapp.util.MessageUtil

sealed class PostAddState {

    data class SetUserDetail(val user: User) : PostAddState()
    object ReadyToWrite: PostAddState()

    data class PostHasBeenSaved(val messageUtil: MessageUtil): PostAddState()

    data class Error(val exception: Exception): PostAddState()
    object Idle: PostAddState()

}