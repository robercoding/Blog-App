package com.rober.blogapp.util

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import androidx.core.content.ContextCompat
import com.rober.blogapp.R

class ColorUtils(val context: Context) {
    val profileEditColorStateListGreen = ColorStateList(
        arrayOf(
            intArrayOf(-android.R.attr.state_checked),
            intArrayOf(android.R.attr.state_checked)
        ),
        intArrayOf(
            ContextCompat.getColor(context, R.color.green),
            ContextCompat.getColor(context, R.color.green)
        )
    )

    val profileEditColorStateListRed = ColorStateList(
        arrayOf(
            intArrayOf(-android.R.attr.state_checked),
            intArrayOf(android.R.attr.state_checked)
        ),
        intArrayOf(
            Color.RED,
            Color.RED
        )
    )

    val postDetailReplyEditText = ColorStateList(
        arrayOf(
            intArrayOf(-android.R.attr.state_checked),
            intArrayOf(android.R.attr.state_checked)
        ),
        intArrayOf(
            ContextCompat.getColor(context, R.color.grayLight),
            ContextCompat.getColor(context, R.color.blueTwitter)
        )
    )
}