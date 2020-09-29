package com.rober.blogapp.ui.main.post.postdetail

import com.rober.blogapp.entity.Option
import com.rober.blogapp.entity.Post
import com.rober.blogapp.entity.User
import java.lang.Exception

sealed class PostDetailState {
    object GetParcelableUpdatedPost : PostDetailState()
    object GetParcelablePost : PostDetailState()

    data class SetPostDetails(val post: Post, val user: User) : PostDetailState()

    data class GoToProfileFragment(val user: User): PostDetailState()
    object BackToPreviousFragment: PostDetailState()

    object HideOptions: PostDetailState()
    data class ShowPostOptions(val listOptions: List<Option>): PostDetailState()
    object PostDeleted : PostDetailState()
    object ErrorExecuteOption: PostDetailState()

    //Options
    data class RedirectToEditPost(val post: Post): PostDetailState()

    data class Error(val exception: Exception): PostDetailState()
    object Loading: PostDetailState()
    data class NotifyUser(val message: String): PostDetailState()
    object Idle: PostDetailState()
}