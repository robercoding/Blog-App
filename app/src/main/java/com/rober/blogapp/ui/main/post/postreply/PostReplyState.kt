package com.rober.blogapp.ui.main.post.postreply

import com.rober.blogapp.entity.Comment
import com.rober.blogapp.entity.Post
import com.rober.blogapp.entity.User

sealed class PostReplyState {

    data class SetSelectedCommentView(
        val post: Post,
        val postUser: User,
        val listComments: List<Comment>,
        val listUsers: List<User>
    ) : PostReplyState()
}