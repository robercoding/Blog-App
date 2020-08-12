package com.rober.blogapp.ui.main.profile.profiledetail

import com.rober.blogapp.entity.Post
import com.rober.blogapp.entity.User
import java.lang.Exception

sealed class ProfileDetailState {

    data class SetUserProfile(val user: User): ProfileDetailState()
    data class SetOtherUserProfile(val user: User): ProfileDetailState()

    data class SetUserPosts(val listUserPosts: List<Post>): ProfileDetailState()
    data class SetOtherUserPosts(val listOtherUserPosts: List<Post>): ProfileDetailState()

    object LoadingPosts : ProfileDetailState()
    data class Error(val exception: Exception): ProfileDetailState()

}