package com.rober.blogapp.ui.main.post.postadd

import com.rober.blogapp.entity.Post
import com.rober.blogapp.entity.User
import com.rober.blogapp.util.MessageUtil

sealed class PostAddState {

    data class SetUserDetail(val user: User) : PostAddState()
    object GetPostToEdit: PostAddState()
    data class RenderPostToEditInView(val post: Post): PostAddState()
    object ReadyToWrite: PostAddState()

    data class SaveOrUpdatePost(val isPostToUpdate: Boolean): PostAddState()
    data class GoToPostDetailAndUpdatePost(val post: Post): PostAddState()
    data class PostHasBeenSaved(val messageUtil: MessageUtil): PostAddState()

    object NotifyErrorFieldValidation : PostAddState()
    data class Error(val exception: Exception): PostAddState()
    object Idle: PostAddState()

}