package com.rober.blogapp.entity

import androidx.room.Entity

@Entity(tableName = "usernames")
data class Username (val uid: String, val username: String){
    constructor() : this("", "")
}