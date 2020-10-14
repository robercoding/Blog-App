package com.rober.blogapp.entity

import androidx.room.PrimaryKey

data class Follower(
    @PrimaryKey
    val followerId: String
) {
  constructor() : this("")
}