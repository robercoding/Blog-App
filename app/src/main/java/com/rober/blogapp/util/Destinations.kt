package com.rober.blogapp.util

import com.rober.blogapp.R
import javax.inject.Inject

class Destinations @Inject constructor(){

    //WithoutBottomNavigation
    val LOGIN_FRAGMENT = R.id.loginFragment
    val REGISTER_FRAGMENT = R.id.registerFragment
    val POST_ADD_FRAGMENT = R.id.postAddFragment
    val POST_DETAIL_FRAGMENT = R.id.postDetailFragment
    val PROFILE_EDIT_FRAGMENT = R.id.profileEditFragment

    //With BottomNavigation
    val FEED_FRAGMENT = R.id.feedFragment
    val SEARCH_FRAGMENT = R.id.searchFragment
    val PROFILE_FRAGMENT = R.id.profileDetailFragment

    val fragmentsWithoutBottomNavigationList = listOf(
        LOGIN_FRAGMENT,
        REGISTER_FRAGMENT,
        POST_ADD_FRAGMENT,
        POST_DETAIL_FRAGMENT,
        PROFILE_EDIT_FRAGMENT
    )

    val fragmentsWithBottomNavigationList = listOf(
        FEED_FRAGMENT,
        SEARCH_FRAGMENT,
        PROFILE_FRAGMENT
    )

}