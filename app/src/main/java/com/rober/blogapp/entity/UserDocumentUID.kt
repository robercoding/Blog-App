package com.rober.blogapp.entity

data class UserDocumentUID(
    val username: String,
    val postsDocumentUid: String,
    val followingDocumentUid: String,
    val followerDocumentUid: String,
    val userUid: String
) {
    constructor() : this("", "", "", "", "")
}