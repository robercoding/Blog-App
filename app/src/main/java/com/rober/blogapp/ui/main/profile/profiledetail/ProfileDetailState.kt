package com.rober.blogapp.ui.main.profile.profiledetail

import android.graphics.Bitmap
import com.rober.blogapp.entity.Post
import com.rober.blogapp.entity.User
import java.lang.Exception

sealed class ProfileDetailState {

    data class SetCurrentUserProfile(val user: User, val imageBackground: String, val bitmap: Bitmap) : ProfileDetailState()
    data class SetOtherUserProfile(val user: User, val currentUserFollowsOtherUser: Boolean) : ProfileDetailState()

    data class SetUserPosts(val listUserPosts: List<Post>) : ProfileDetailState()

    data class Unfollowed(val user: User) : ProfileDetailState()
    data class Followed(val user: User) : ProfileDetailState()

    object UnfollowError: ProfileDetailState()
    object FollowError: ProfileDetailState()

    object Idle : ProfileDetailState()

    object LoadingUser : ProfileDetailState()
    object LoadingPosts : ProfileDetailState()
    data class Error(val exception: Exception) : ProfileDetailState()

}