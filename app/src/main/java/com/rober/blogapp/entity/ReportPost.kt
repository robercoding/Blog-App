package com.rober.blogapp.entity

import com.rober.blogapp.entity.base.Report

data class ReportPost constructor(val reportedPostId: String, override val userIdReported: String, override val reportedCause: String, override val message: String) : Report{
}