package com.rober.blogapp.data

sealed class ResultData <out R> {
    data class Success<out T>(val data: T? = null): ResultData<T>()
    data class Error<out T>(val exception: Exception, val data: T? = null): ResultData<T>()
    object Loading : ResultData<Nothing>()
}