package com.rober.blogapp.util

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager

interface RecyclerViewActionInterface {
    fun clickListenerOnItem(positionAdapter: Int)

    fun clickListenerOnPost(positionAdapter: Int)
    fun clickListenerOnUser(positionAdapter: Int)

    fun requestMorePosts(actualRecyclerViewPosition: Int)
}