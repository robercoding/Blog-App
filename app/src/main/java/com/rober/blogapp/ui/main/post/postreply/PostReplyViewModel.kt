package com.rober.blogapp.ui.main.post.postreply

import android.util.Log
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.viewModelScope
import com.rober.blogapp.data.ResultData
import com.rober.blogapp.data.network.repository.FirebaseRepository
import com.rober.blogapp.entity.Comment
import com.rober.blogapp.entity.Post
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
    var mutableListComments = mutableListOf<Comment>()

    var mutableListUsers = mutableListOf<User>()

    var mutableListHistoryHighlightComment = mutableListOf<List<Comment>>()
    var mutableListHistoryComment = mutableListOf<List<Comment>>()

    var user: User = firebaseRepository.getCurrentUser()

    lateinit var post: Post
    lateinit var postUser: User

    override fun setIntention(event: PostReplyEvent) {
        when (event) {
            is PostReplyEvent.SetDetails -> {
                mutableListHighlightsComments = event.listComments.toMutableList()
                mutableListUsers = event.listUsers.toMutableList()
                highlightComment = event.listComments[event.listComments.lastIndex]

                post = event.post
                postUser = event.postUser


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
                Log.i("SeeRestore", "Before save ${mutableListHighlightsComments}")
                saveHistory()

                val nextCommentHighlight = mutableListComments[event.positionAdapter]
                mutableListHighlightsComments.add(nextCommentHighlight)

                val isUserInHighlightUsers =
                    mutableListUsers.any { user -> user.userId == nextCommentHighlight.commentUserId }
                if (!isUserInHighlightUsers) {
                    val user =
                        mutableListUsers.firstOrNull { user -> user.userId == nextCommentHighlight.commentUserId }

                    if (user == null) {
                        mutableListHighlightsComments.remove(nextCommentHighlight)
                        return
                    }
                    mutableListUsers.add(user)
                }

                highlightComment = nextCommentHighlight
                viewState = PostReplyState.SetSelectedCommentView(
                    post,
                    postUser,
                    mutableListHighlightsComments.toList(),
                    mutableListUsers.toList()
                )
            }
            is PostReplyEvent.PopBackStack -> {
                if (mutableListHistoryHighlightComment.toList().isEmpty()) {
                    Log.i("SeeListHistory", "Empty")
                    viewState = PostReplyState.PopBackStack
                    return
                }

                Log.i("SeeListHistory", "Before remove= ${mutableListHistoryHighlightComment}")

                //Get comment state
                mutableListHighlightsComments =
                    mutableListHistoryHighlightComment[mutableListHistoryHighlightComment.lastIndex].toMutableList()
                mutableListComments =
                    mutableListHistoryComment[mutableListHistoryComment.lastIndex].toMutableList()

                mutableListHistoryHighlightComment.removeAt(mutableListHistoryHighlightComment.lastIndex)
                Log.i("SeeListHistory", "After remove= ${mutableListHistoryHighlightComment}")
                mutableListHistoryComment.removeAt(mutableListHistoryComment.lastIndex)

                viewState = PostReplyState.RestoreCommentsAdapter(
                    mutableListHighlightsComments.toList(),
                    mutableListComments.toList(),
                    mutableListUsers.toList(),
                    postUser
                )
            }
        }
    }

    private suspend fun getCommentReplies() {
        mutableListComments.clear()
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
                            mutableListUsers.addAll(resultData.data!!)
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

    private fun saveHistory() {
        Log.i("SeeRestore", "Save comments! ${mutableListHighlightsComments}")
        mutableListHistoryHighlightComment.add(mutableListHighlightsComments.toList())
        mutableListHistoryComment.add(mutableListComments.toList())
    }

}