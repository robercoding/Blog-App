package com.rober.blogapp.util.state

sealed class PostAddState {

    data class Success<T>(val data:T?): PostAddState()
    data class Error<T>(val exception: T, val data:T?): PostAddState()
    object Idle: PostAddState()

}