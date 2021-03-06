package com.rober.blogapp.ui.main.post.postdetail

import com.rober.blogapp.entity.Comment
import com.rober.blogapp.entity.Option
import com.rober.blogapp.entity.Post
import com.rober.blogapp.entity.User
import java.lang.Exception

sealed class PostDetailState {
    object GetParcelableUpdatedPost : PostDetailState()
    object GetParcelablePost : PostDetailState()
    object GetParcelableReportedPost : PostDetailState()

    data class SetPostDetails(val post: Post, val user: User) : PostDetailState()
    data class SetPostCommments(val listComment: List<Comment>, val listUser: List<User>) : PostDetailState()
    object PostCommentsEmpty : PostDetailState()
    data class SetReportPostDetails(val post: Post, val user: User) : PostDetailState()

    //Reply
    object LoadingReply : PostDetailState()
    data class ReplySuccess(val listComment: List<Comment>, val listUser: List<User>) : PostDetailState()

    //Comment
    data class SetSelectedCommentView(
        val listSelectedComment: List<Comment>,
        val listUsers: List<User>,
        val post: Post,
        val postUser: User
    ) : PostDetailState()

    data class GoToProfileFragment(val user: User) : PostDetailState()
    object BackToPreviousFragment : PostDetailState()

    object HideOptions : PostDetailState()
    data class ShowPostOptions(val listOptions: List<Option>) : PostDetailState()
    object OpenDialogReport : PostDetailState()
    object PostDeleted : PostDetailState()
    object ErrorExecuteOption : PostDetailState()

    //Options
    data class RedirectToEditPost(val post: Post) : PostDetailState()

    data class Error(val exception: Exception) : PostDetailState()
    data class ErrorLoadingPost(val message: String) : PostDetailState()
    object Loading : PostDetailState()
    data class NotifyUser(val message: String) : PostDetailState()
    object Idle : PostDetailState()
}