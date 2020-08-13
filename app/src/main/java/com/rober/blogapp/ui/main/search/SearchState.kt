package com.rober.blogapp.ui.main.search

import com.rober.blogapp.entity.User
import java.lang.Exception

sealed class SearchState {
    data class ShowResultSearch(val listUsers: List<User>): SearchState()
    data class EmptyResultsSearch(val searchUsername: String): SearchState()

    object ReadySearchUser: SearchState()
    object StopSearchUser: SearchState()

    data class Error(val exception: Exception): SearchState()
    object Loading: SearchState()
    object Idle: SearchState()
}