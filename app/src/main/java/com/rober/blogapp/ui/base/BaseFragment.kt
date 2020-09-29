package com.rober.blogapp.ui.base

import androidx.fragment.app.Fragment

open class BaseFragment : Fragment() {

    fun getEmoji(codePoint: Int): String{
        return String(Character.toChars(codePoint))
    }
}