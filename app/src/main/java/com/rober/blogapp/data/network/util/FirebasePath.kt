package com.rober.blogapp.data.network.util

import javax.inject.Inject

class FirebasePath @Inject constructor() {
    //Collection go with an underscore, example: user_posts
    //Documents go together, example: countPosts
    val users_col = "users"
    val user_documents_uid = "user_documents_uid"

    val posts_col = "posts"
    val user_posts = "user_posts"


    val user_count_posts = "user_count_posts"
    val countPosts = "countPosts"

    //Following
    val following_col = "following"
    val user_following = "user_following"

    //Followers
    val follower_col = "follower"
    val user_followers = "user_followers"

    //Post
    val reports_col = "reports"
    val posts_reports = "posts_reports"

    //Disabled
    val disabled_col = "disabled"
}