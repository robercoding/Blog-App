package com.rober.blogapp.util

import android.view.View

interface RecyclerViewClickInterface{
    fun clickListenerOnPost(positionAdapter: Int)
    fun clickListenerOnUser(positionAdapter: Int)
}