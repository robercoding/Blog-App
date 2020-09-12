package com.rober.blogapp.util

import android.graphics.Bitmap

interface AsyncResponse {
    fun processFinish(processedBitmap: Bitmap?)
}