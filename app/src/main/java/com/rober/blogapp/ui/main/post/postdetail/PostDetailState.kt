package com.rober.blogapp.ui.main.post.postdetail

import com.rober.blogapp.entity.Post
import java.lang.Exception

sealed class PostDetailState {
    data class SetPostDetails(val data: Post) : PostDetailState()

    object BackToPreviousFragment: PostDetailState()

    data class Error(val exception: Exception): PostDetailState()
    object Loading: PostDetailState()
}