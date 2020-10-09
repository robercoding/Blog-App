package com.rober.blogapp.entity

import android.os.Parcelable
import com.rober.blogapp.entity.base.Report
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ReportPost constructor(
    val reportedPostId: String,
    val userIdOwnerReportedPost: String,
    override val userIdReported: String,
    override val reportedCause: String,
    override val message: String
) : Report, Parcelable {

    constructor() : this("", "", "", "", "")
}