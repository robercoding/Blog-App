package com.rober.blogapp.entity

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Comment constructor(
    val message: String,
    var commentId: String,
    val commentUserId: String,
    val replyToldId: String,
    val repliedAt: Long
) : Parcelable {

    constructor() : this("", "", "", "", 0)
}