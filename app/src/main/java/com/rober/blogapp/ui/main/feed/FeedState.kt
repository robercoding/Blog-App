package com.rober.blogapp.ui.main.feed

import com.rober.blogapp.entity.Post

sealed class FeedState {
    data class SetListPosts(val listFeedPosts: List<Post>): FeedState()
    data class LoadNewPosts(val listFeedPosts: List<Post>, val scrollToPosition: Int): FeedState()
    data class LoadOldPosts(val listFeedPosts: List<Post>, val scrollToPosition: Int, val endOfTimeline: Boolean): FeedState()
    object StopRequestOldPosts: FeedState()

    data class GoToPostDetails(val post: Post): FeedState()

    object Loading: FeedState()
    object LoadingMorePosts: FeedState()
    data class Error(val message: String?): FeedState()

    object Idle: FeedState()
}