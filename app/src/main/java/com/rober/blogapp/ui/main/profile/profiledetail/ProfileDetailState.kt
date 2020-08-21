package com.rober.blogapp.ui.main.profile.profiledetail

import com.rober.blogapp.entity.Post
import com.rober.blogapp.entity.User
import java.lang.Exception

sealed class ProfileDetailState {

    data class SetUserProfile(val user: User) : ProfileDetailState()
    data class SetOtherUserProfile(val user: User, val currentUserFollowsOtherUser: Boolean) : ProfileDetailState()

    data class SetUserPosts(val listUserPosts: List<Post>) : ProfileDetailState()
    data class SetOtherUserPosts(val listOtherUserPosts: List<Post>) : ProfileDetailState()

    data class Unfollowed(val user: User) : ProfileDetailState()
    data class Followed(val user: User) : ProfileDetailState()

    object Idle : ProfileDetailState()

    object LoadingUser : ProfileDetailState()
    object LoadingPosts : ProfileDetailState()
    data class Error(val exception: Exception) : ProfileDetailState()

}