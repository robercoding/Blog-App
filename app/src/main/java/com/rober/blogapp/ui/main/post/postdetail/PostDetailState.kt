package com.rober.blogapp.ui.main.post.postdetail

import com.rober.blogapp.entity.Post
import com.rober.blogapp.entity.User
import java.lang.Exception

sealed class PostDetailState {
    data class SetPostDetails(val post: Post, val user: User) : PostDetailState()

    object BackToPreviousFragment: PostDetailState()

    data class Error(val exception: Exception): PostDetailState()
    object Loading: PostDetailState()
}