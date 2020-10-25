package com.rober.blogapp.ui.main.post.postreply

interface PostReplyClickListener {
    fun onClickHighlightComment(positionAdapter: Int)
    fun onClickReplyComment(positionAdapter: Int)
}