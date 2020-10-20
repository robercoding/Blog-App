package com.rober.blogapp.entity

data class Comment constructor(
    val message: String,
    var commentId: String,
    val commentUserId: String,
    val replyToldId: String,
    val repliedAt: Long
) {

    constructor() : this("", "", "", "", 0)
}