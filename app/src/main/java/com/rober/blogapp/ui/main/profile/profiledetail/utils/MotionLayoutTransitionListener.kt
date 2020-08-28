package com.rober.blogapp.ui.main.profile.profiledetail.utils

import android.content.Context
import android.util.Log
import android.view.View
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.rober.blogapp.R
import kotlinx.android.synthetic.main.fragment_feed.view.*
import kotlinx.android.synthetic.main.fragment_profile_detail.*
import kotlinx.android.synthetic.main.fragment_profile_detail.view.*

class MotionLayoutTransitionListener constructor(
    private val view: View,
    private val imageFromUrl: String,
    private val dominantColorFromImageUrl: Int
) : MotionLayout.TransitionListener {

    override fun onTransitionTrigger(p0: MotionLayout?, p1: Int, p2: Boolean, p3: Float) {

    }

    override fun onTransitionStarted(p0: MotionLayout?, p1: Int, p2: Int) {

    }

    override fun onTransitionChange(p0: MotionLayout?, p1: Int, p2: Int, p3: Float) {
        if (p3 < 0.65f) {
            Glide.with(view)
                .load("https://firebasestorage.googleapis.com/v0/b/blog-app-d5912.appspot.com/o/users_profile_picture%2Fmew_small_1024_x_1024.jpg?alt=media&token=21dfa28c-2416-49c3-81e1-2475aaf25150")
                .into(view.profile_detail_image_background)
            view.profile_detail_swipe_refresh_layout.isEnabled = true
        } else {
            Glide.with(view)
                .clear(view.profile_detail_image_background)
            view.profile_detail_swipe_refresh_layout.isEnabled = false
            Log.i("ColorDiff", "White = ${R.color.white} and new ${dominantColorFromImageUrl}")
//            view.profile_detail_image_background.setBackgroundColor(ContextCompat.getColor(context, R.color.white))
            view.profile_detail_image_background.setBackgroundColor(dominantColorFromImageUrl)

        }
    }

    override fun onTransitionCompleted(p0: MotionLayout?, p1: Int) {

    }
}