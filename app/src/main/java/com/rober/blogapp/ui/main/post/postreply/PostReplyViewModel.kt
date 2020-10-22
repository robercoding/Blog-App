package com.rober.blogapp.ui.main.post.postreply

import androidx.hilt.lifecycle.ViewModelInject
import com.rober.blogapp.ui.base.BaseViewModel

class PostReplyViewModel @ViewModelInject constructor() : BaseViewModel<PostReplyState, PostReplyEvent>() {
    override fun setIntention(event: PostReplyEvent) {
        when (event) {
            is PostReplyEvent.SetDetails -> {
                viewState = PostReplyState.SetSelectedCommentView(
                    event.post,
                    event.postUser,
                    event.listComments,
                    event.listUsers
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
    }
}