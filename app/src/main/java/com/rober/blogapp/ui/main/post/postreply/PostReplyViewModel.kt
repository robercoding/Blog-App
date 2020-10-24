package com.rober.blogapp.ui.main.post.postreply

import android.util.Log
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.viewModelScope
import com.rober.blogapp.data.ResultData
import com.rober.blogapp.data.network.repository.FirebaseRepository
import com.rober.blogapp.entity.Comment
import com.rober.blogapp.entity.User
import com.rober.blogapp.ui.base.BaseViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.threeten.bp.Instant

class PostReplyViewModel @ViewModelInject constructor(
    private val firebaseRepository: FirebaseRepository
) : BaseViewModel<PostReplyState, PostReplyEvent>() {

    lateinit var highlightComment: Comment

    var mutableListHighlightsComments = mutableListOf<Comment>()
    var mutableListHighlightsUsers = mutableListOf<User>()

    var mutableListComments = mutableListOf<Comment>()
    var mutableListUsers = mutableListOf<User>()

    var user: User = firebaseRepository.getCurrentUser()

    override fun setIntention(event: PostReplyEvent) {
        when (event) {
            is PostReplyEvent.SetDetails -> {
                mutableListHighlightsComments = event.listComments.toMutableList()
                mutableListHighlightsUsers = event.listUsers.toMutableList()
                highlightComment = event.listComments[event.listComments.lastIndex]

                viewState = PostReplyState.SetSelectedCommentView(
                    event.post,
                    event.postUser,
                    event.listComments,
                    event.listUsers
                )
            }

            is PostReplyEvent.GetCommentReplies -> {
                viewModelScope.launch {
                    getCommentReplies()
                }
            }

            is PostReplyEvent.ReplyComment -> {
                addReplyToComment(event.text)
            }

            is PostReplyEvent.SelectReplyComment -> {

            }
        }
    }

    private suspend fun getCommentReplies() {
        job = viewModelScope.launch {
            firebaseRepository.getCommentRepliesById(highlightComment.commentId)
                .collect { resultData ->
                    when (resultData) {
                        is ResultData.Success -> {
                            Log.i("SeeGetReplies", "Success")
                            mutableListComments = resultData.data!!.toMutableList()
                        }

                        is ResultData.Error -> {
                            Log.i("SeeGetReplies", "Error")
                        }
                    }
                }
        }
        job?.join()

        if (mutableListComments.isEmpty()) {
            Log.i("SeeGetReplies", "Empty")
            viewState = PostReplyState.CommentRepliesEmpty
            return
        }

        job = viewModelScope.launch {
            firebaseRepository.getUsersComments(mutableListComments)
                .collect { resultData ->
                    when (resultData) {
                        is ResultData.Success -> {
                            Log.i("SeeGetReplies", "Success users")
                            mutableListUsers = resultData.data!!.toMutableList()
                        }
                        is ResultData.Error -> {
                            Log.i("SeeGetReplies", "Error users")
                        }
                    }

                }
        }
        job?.join()

        if (mutableListUsers.isEmpty()) {
            viewState = PostReplyState.CommentRepliesEmpty
            return
        }

        viewState = PostReplyState.SetCommentReplies(mutableListComments.toList(), mutableListUsers.toList())
    }

    private fun addReplyToComment(text: String) {
        val comment = Comment(text, "", user.userId, highlightComment.commentId, Instant.now().epochSecond)
        viewModelScope.launch {
            firebaseRepository.addReplyToComment(comment)
                .collect { resultData ->
                    when (resultData) {
                        is ResultData.Success -> {
                            mutableListComments.add(comment)

                            val isUserInList =
                                mutableListUsers.any { user -> user.userId == comment.commentUserId }
                            if (!isUserInList) {
                                mutableListUsers.add(user)
                            }
                            viewState = PostReplyState.ReplySuccess(
                                mutableListComments.toList(),
                                mutableListUsers.toList()
                            )
                        }

                        is ResultData.Error -> {
                            val message = resultData.exception.message
                            message?.run {
                                viewState = PostReplyState.Error(this)
                            }
                        }
                    }

                }
        }
    }

}