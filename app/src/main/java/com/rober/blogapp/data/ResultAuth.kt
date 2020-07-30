package com.rober.blogapp.data

sealed class ResultAuth {
    object Success: ResultAuth()
    data class Error(val exception: Exception) : ResultAuth()
    object Loading: ResultAuth()
}