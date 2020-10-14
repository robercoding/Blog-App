package com.rober.blogapp.ui.main.feed

import com.rober.blogapp.entity.Post
import com.rober.blogapp.entity.User
import com.rober.blogapp.util.MessageUtil

sealed class FeedState {
    data class SetUserDetails(val user: User): FeedState()
    data class SetListPosts(val listFeedPosts: List<Post>, val listFeedUsers: List<User>): FeedState()
    data class LoadNewPosts(val listFeedPosts: List<Post>, val listFeedUsers: List<User>, val scrollToPosition: Int): FeedState()
    data class LoadOldPosts(val listFeedPosts: List<Post>,  val listFeedUsers: List<User>, val scrollToPosition: Int, val endOfTimeline: Boolean): FeedState()
    object StopRequestOldPosts: FeedState()
    data class StopRequestNewPosts(var messageUtil: MessageUtil? = null): FeedState()
    object LoadMessageZeroPosts: FeedState()

    data class GoToPostDetailsFragment(val post: Post): FeedState()
    data class GoToProfileDetailsFragment(val userId: String): FeedState()

    object Loading: FeedState()
    object LoadingMorePosts: FeedState()
    data class Error(val message: String?): FeedState()

    object Idle: FeedState()
    object  SignOut: FeedState()
}