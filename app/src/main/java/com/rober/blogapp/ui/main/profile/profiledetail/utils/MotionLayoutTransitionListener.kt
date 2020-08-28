package com.rober.blogapp.ui.main.profile.profiledetail.utils

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.ViewTarget
import com.rober.blogapp.R
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.android.synthetic.main.fragment_feed.view.*
import kotlinx.android.synthetic.main.fragment_profile_detail.*
import kotlinx.android.synthetic.main.fragment_profile_detail.view.*

class MotionLayoutTransitionListener constructor(
    private val view: View,
    private val imageFromUrl: String,
    private val dominantColorFromImageUrl: Int
) : MotionLayout.TransitionListener {

    var blurredImageGlide : ViewTarget<ImageView, Drawable>? = null
    var isImageBlurred = false

    override fun onTransitionTrigger(p0: MotionLayout?, p1: Int, p2: Boolean, p3: Float) {

    }

    override fun onTransitionStarted(p0: MotionLayout?, p1: Int, p2: Int) {

    }

    override fun onTransitionChange(p0: MotionLayout?, p1: Int, p2: Int, p3: Float) {
        Log.i("PTres", "${p3}")
//        when(p3){
//            0f -> {
////                applyBlurrToImageBackground(0, 0)
//                Glide.with(view)
//                    .load(imageFromUrl)
//                    .into(view.profile_detail_image_background)
//                view.profile_detail_swipe_refresh_layout.isEnabled = true
//            }
//            0.35f -> {
//                applyBlurrToImageBackground(25, 1)
//            }
//
//            0.45f -> {
//                applyBlurrToImageBackground(25, 2)
//            }
//            0.55f -> {
//                applyBlurrToImageBackground(25, 3)
//            }
//            0.65f -> {
//                applyBlurrToImageBackground(25, 4)
//                view.profile_detail_swipe_refresh_layout.isEnabled = true
//            }
//            0.75f ->{
//                view.profile_detail_image_background.setBackgroundColor(dominantColorFromImageUrl)
//                view.profile_detail_swipe_refresh_layout.isEnabled = false
//            }
//        }
        //KEEP BLURRED IMAGE UNTIL IT'S FULLY COLOR DOMINANT
//        if (p3 > 0 && p3 < 0.35) {
//            Glide.with(view)
//                .load(imageFromUrl)
//                .into(view.profile_detail_image_background)
//            view.profile_detail_swipe_refresh_layout.isEnabled = true
//        }else if(p3 > 0.35f && p3 < 0.45){
//            applyBlurrToImageBackground(25, 1)
//            view.profile_detail_swipe_refresh_layout.isEnabled = true
//        }else if(p3 > 0.45 && p3 < 0.55){
//
//        }else if(p3 > 0.55 && p3 < 0.65){
//                applyBlurrToImageBackground(25, 1)
//                view.profile_detail_swipe_refresh_layout.isEnabled = true
//        }else if(p3 > 0.65 && p3 < 0.90){
//            view.profile_detail_swipe_refresh_layout.isEnabled = false
//        }else if(p3 > 0.90){
//            Glide.with(view)
//                .clear(view.profile_detail_image_background)
//            view.profile_detail_image_background.setBackgroundColor(dominantColorFromImageUrl)
//            view.profile_detail_swipe_refresh_layout.isEnabled = false
//        }
        if(p3 < 0.70f){
            Glide.with(view)
                .load(imageFromUrl)
                .into(view.profile_detail_image_background_clear)
            view.profile_detail_image_background_clear.visibility = View.VISIBLE
            view.profile_detail_image_background_blurred.visibility = View.INVISIBLE
            view.profile_detail_swipe_refresh_layout.isEnabled = true
            if(!isImageBlurred){
                applyBlurrToImageBackground(10, 2)
                isImageBlurred = true
            }

        }else if(p3>0.70f && p3< 0.85f){
            view.profile_detail_image_background_blurred.visibility = View.VISIBLE
            view.profile_detail_image_background_clear.visibility = View.INVISIBLE
            view.profile_detail_swipe_refresh_layout.isEnabled = false

        }else if(p3> 0.85f){
            view.profile_detail_image_background_clear.visibility = View.VISIBLE
            view.profile_detail_image_background_blurred.visibility = View.INVISIBLE

            view.profile_detail_image_background_clear.setBackgroundColor(dominantColorFromImageUrl)
            Glide.with(view)
                .clear(view.profile_detail_image_background_clear)
//            Glide.with(view)
//                .clear(view.profile_detail_image_background_blurred)

            view.profile_detail_swipe_refresh_layout.isEnabled = false
        }
//        if(p3 > 75f){
////            Glide.with(view)
////                .clear(view.profile_detail_image_background)
//            applyBlurrToImageBackground(25, 3)
////            view.profile_detail_image_background.setBackgroundColor(dominantColorFromImageUrl)
//            view.profile_detail_swipe_refresh_layout.isEnabled = false
//        }else{
//            Glide.with(view)
//                .load(imageFromUrl)
//                .into(view.profile_detail_image_background)
//            view.profile_detail_swipe_refresh_layout.isEnabled = true
//        }
    }

    override fun onTransitionCompleted(p0: MotionLayout?, p1: Int) {

    }

    private fun applyBlurrToImageBackground(radius: Int, sampling: Int){
        Glide.with(view)
            .load(imageFromUrl)
            .apply(RequestOptions.bitmapTransform(BlurTransformation(radius,sampling)))
            .into(view.profile_detail_image_background_blurred)
    }
}