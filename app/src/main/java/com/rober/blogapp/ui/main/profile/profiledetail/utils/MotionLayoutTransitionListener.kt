package com.rober.blogapp.ui.main.profile.profiledetail.utils


import android.util.Log
import android.view.View
import androidx.constraintlayout.motion.widget.MotionLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.android.synthetic.main.fragment_profile_detail.view.*

class MotionLayoutTransitionListener constructor(
    private val view: View,
    private val imageFromUrl: String,
    private val dominantColorFromImageUrl: Int
) : MotionLayout.TransitionListener {

    var isImageBlurred = false

    override fun onTransitionTrigger(p0: MotionLayout?, p1: Int, p2: Boolean, p3: Float) {

    }

    override fun onTransitionStarted(p0: MotionLayout?, p1: Int, p2: Int) {

    }

    override fun onTransitionChange(p0: MotionLayout?, p1: Int, p2: Int, p3: Float) {
        Log.i("MotionListener", "$p3")

        if(p3 < 0.70f){
            Glide.with(view)
                .load(imageFromUrl)
                .into(view.profile_detail_image_background_clear)
            view.profile_detail_image_background_clear.visibility = View.VISIBLE
            view.profile_detail_image_background_blurred.visibility = View.INVISIBLE
            view.profile_detail_swipe_refresh_layout.isEnabled = true
            if(!isImageBlurred){
                applyBlurToImageBackground(10, 2)
                isImageBlurred = true
            }

        }else if(p3>0.70f && p3< 0.85f) {
            view.profile_detail_image_background_blurred.visibility = View.VISIBLE
            view.profile_detail_image_background_clear.visibility = View.INVISIBLE
            view.profile_detail_swipe_refresh_layout.isEnabled = false

        }else if(p3> 0.85f){
            view.profile_detail_image_background_clear.visibility = View.VISIBLE
            view.profile_detail_image_background_blurred.visibility = View.INVISIBLE

            view.profile_detail_image_background_clear.setBackgroundColor(dominantColorFromImageUrl)
            Glide.with(view)
                .clear(view.profile_detail_image_background_clear)

            view.profile_detail_swipe_refresh_layout.isEnabled = false
        }
    }

    override fun onTransitionCompleted(p0: MotionLayout?, p1: Int) {
    }

    private fun applyBlurToImageBackground(radius: Int, sampling: Int){
        Glide.with(view)
            .load(imageFromUrl)
            .apply(RequestOptions.bitmapTransform(BlurTransformation(radius,sampling)))
            .into(view.profile_detail_image_background_blurred)
    }
}