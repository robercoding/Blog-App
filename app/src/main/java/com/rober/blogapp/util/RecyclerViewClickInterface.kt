package com.rober.blogapp.util

import android.view.View

interface RecyclerViewActionInterface{
    fun clickListenerOnPost(positionAdapter: Int)
    fun clickListenerOnUser(positionAdapter: Int)

    fun loadOldFeedPosts()
}