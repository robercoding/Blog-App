package com.rober.blogapp.entity

import androidx.room.PrimaryKey

data class Follower(
    @PrimaryKey
    val follower_id: String
) {
  constructor() : this("")
}