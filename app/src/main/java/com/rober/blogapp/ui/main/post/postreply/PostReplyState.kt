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

    data class SetCommentReplies(
        val listComments: List<Comment>,
        val listUsers: List<User>
    ) : PostReplyState()

    data class RestoreCommentsAdapter(
        val listHighlightComments: List<Comment>,
        val listComments: List<Comment>,
        val listUsers: List<User>,
        val postUser: User
    ) : PostReplyState()

    object CommentRepliesEmpty : PostReplyState()
    object PopBackStack : PostReplyState()

    //Reply
    data class ReplySuccess(val listComment: List<Comment>, val listUser: List<User>) : PostReplyState()
    data class Error(val message: String) : PostReplyState()
}