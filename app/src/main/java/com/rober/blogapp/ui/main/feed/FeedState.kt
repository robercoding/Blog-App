package com.rober.blogapp.ui.main.feed

import com.rober.blogapp.entity.Post

sealed class FeedState {
    data class SetListPosts(val listFeedPosts: List<Post>): FeedState()

    object Loading: FeedState()
    data class Error(val message: String?): FeedState()

    object Idle: FeedState()
}