package com.rober.blogapp.util

import android.graphics.Bitmap

interface AsyncResponse {
    fun processFinish(output: Bitmap)
}