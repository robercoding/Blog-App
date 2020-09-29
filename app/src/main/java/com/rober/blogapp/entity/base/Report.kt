package com.rober.blogapp.entity.base

interface Report {
    val userIdReported: String
    val reportedCause: String
    val message: String
}