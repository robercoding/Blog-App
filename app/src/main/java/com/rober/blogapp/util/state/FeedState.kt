package com.rober.blogapp.util.state

sealed class FeedState<out R> {

    object Idle: FeedState<Nothing>()
    object GettingPostState : FeedState<Nothing>()
    data class SuccessListPostState<out T>(val data: T): FeedState<T>()
    data class Error<out T>(val message: String?): FeedState<T>()
}