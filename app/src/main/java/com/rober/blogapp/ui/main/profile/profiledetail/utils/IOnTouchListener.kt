package com.rober.blogapp.ui.main.profile.profiledetail.utils

import android.view.View
import com.rober.blogapp.ui.main.profile.profiledetail.ProfileDetailFragmentEvent

interface IOnTouchListener {
    fun setRippleEffectIfTouch(view: View, touchCoordinateX: Float, touchCoordinateY: Float)
    fun isTouchActionUpOnViewPlace(view: View, touchCoordinateX: Float, touchCoordinateY: Float) : Boolean
    fun setTouchIntention(profileDetailFragmentEvent: ProfileDetailFragmentEvent)
}