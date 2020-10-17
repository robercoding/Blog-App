package com.rober.blogapp.entity

data class Comment constructor(val commentId: String, val userId: String, val replyToldId:String, val hasReply: Boolean) {

    constructor() : this("", "", "", false)
}