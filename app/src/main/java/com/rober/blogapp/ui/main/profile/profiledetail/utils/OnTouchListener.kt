package com.rober.blogapp.ui.main.profile.profiledetail.utils

import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import com.rober.blogapp.ui.main.profile.profiledetail.ProfileDetailFragmentEvent
import kotlinx.android.synthetic.main.fragment_profile_detail.view.*

class OnTouchListener constructor(val rootView: View, val iOnTouchListenerDelegate: IOnTouchListener) :
    View.OnTouchListener {

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {

        if (event?.action == MotionEvent.ACTION_DOWN) {
            val touchCoordinateX = event.x
            val touchCoordinateY = event.y

//                    val rectProfileImage = Rect()
//                    val rectProfileImageVisible = profile_detail_button_edit.getGlobalVisibleRect(rectProfileImage)
//
//                    if (rectProfileImageVisible) {
//                        setDarkerModeOnImage(uid_image, touchCoordinateX, touchCoordinateY)
//                    }


            val rectEditButton = Rect()
            val profileEditButtonVisible = rootView.profile_detail_button_edit.getGlobalVisibleRect(rectEditButton)

            if (profileEditButtonVisible) {
                iOnTouchListenerDelegate.setRippleEffectIfTouch(
                    rootView.profile_detail_button_edit,
                    touchCoordinateX,
                    touchCoordinateY
                )
            }

            val rectFollowButton = Rect()
            val profileFollowButtonVisible = rootView.profile_detail_button_edit.getGlobalVisibleRect(rectFollowButton)

            if (profileFollowButtonVisible) {
                iOnTouchListenerDelegate.setRippleEffectIfTouch(
                    rootView.profile_detail_button_follow,
                    touchCoordinateX,
                    touchCoordinateY
                )
            }

            val rectArrowButton = Rect()
            val profileRectArrowButton = rootView.profile_detail_arrow_back.getGlobalVisibleRect(rectArrowButton)
            if (profileRectArrowButton) {
                iOnTouchListenerDelegate.setRippleEffectIfTouch(
                    rootView.profile_detail_arrow_back,
                    touchCoordinateX,
                    touchCoordinateY
                )
            }
            return false
        }

        if (event?.action == MotionEvent.ACTION_UP) {
            val touchCoordinateX = event.x
            val touchCoordinateY = event.y

            if (iOnTouchListenerDelegate.isTouchActionUpOnViewPlace(
                    rootView.profile_detail_image_background_clear,
                    touchCoordinateX,
                    touchCoordinateY
                )
            ) {
                when {
                    iOnTouchListenerDelegate.isTouchActionUpOnViewPlace(
                        rootView.profile_detail_arrow_back,
                        touchCoordinateX,
                        touchCoordinateY
                    ) -> {
                        iOnTouchListenerDelegate.setTouchIntention(ProfileDetailFragmentEvent.PopBackStack)
                    }
                    iOnTouchListenerDelegate.isTouchActionUpOnViewPlace(
                        rootView.uid_image,
                        touchCoordinateX,
                        touchCoordinateY
                    )-> {
                        iOnTouchListenerDelegate.setTouchIntention(ProfileDetailFragmentEvent.LoadProfileImage)
                    }
                    iOnTouchListenerDelegate.isTouchActionUpOnViewPlace(
                        rootView.profile_detail_image_background_clear,
                        touchCoordinateX,
                        touchCoordinateY
                    )-> {
                        iOnTouchListenerDelegate.setTouchIntention(ProfileDetailFragmentEvent.LoadBackgroundImage)
                    }
                }
            }

            if (iOnTouchListenerDelegate.isTouchActionUpOnViewPlace(rootView.uid_image, touchCoordinateX, touchCoordinateY)) {
                iOnTouchListenerDelegate.setTouchIntention(ProfileDetailFragmentEvent.LoadProfileImage)
            }

            if (iOnTouchListenerDelegate.isTouchActionUpOnViewPlace(rootView.profile_detail_button_edit, touchCoordinateX, touchCoordinateY)) {
                iOnTouchListenerDelegate.setTouchIntention(ProfileDetailFragmentEvent.NavigateToProfileEdit)
            } else {
                rootView.profile_detail_button_edit.isPressed = false
            }
//
            if (iOnTouchListenerDelegate.isTouchActionUpOnViewPlace(rootView.profile_detail_button_follow, touchCoordinateX, touchCoordinateY)) {
                if (rootView.profile_detail_button_follow.isSelected) {
                    iOnTouchListenerDelegate.setTouchIntention(ProfileDetailFragmentEvent.Unfollow)
                } else {
                    iOnTouchListenerDelegate.setTouchIntention(ProfileDetailFragmentEvent.Follow)
                }
            } else {
                rootView.profile_detail_button_follow.isPressed = false //may delete
            }
        }

        return false
    }
}