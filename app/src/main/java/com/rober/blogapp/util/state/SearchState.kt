package com.rober.blogapp.util.state

import com.rober.blogapp.entity.User

sealed class SearchState {
    data class ShowResultSearch(val listUsers: List<User>): SearchState()
    object ReadySearchUser: SearchState()
    object StopSearchUser: SearchState()
}