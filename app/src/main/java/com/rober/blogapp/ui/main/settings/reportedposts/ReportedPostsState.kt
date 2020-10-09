package com.rober.blogapp.ui.main.settings.reportedposts

import com.rober.blogapp.entity.ReportPost

sealed class ReportedPostsState {

    data class SetTotalPosts(val totalReportedPosts: String) : ReportedPostsState()
    data class SetTotalPostsAndList(val totalReportedPosts: String, val listReportPost: List<ReportPost>) :
        ReportedPostsState()

    data class Error(val message: String) : ReportedPostsState()

}