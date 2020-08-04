package com.rober.blogapp.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude

//@Entity(tableName = "users", indices = arrayOf(Index(value = ["username"], unique = true)))
@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    @get:Exclude val db_user_id: Long = 0,
    val user_id: String = "",
    val username: String = "",
    val biography: String = "",
    val location: String = ""
) {
    constructor() : this(0, "", "", "", "")

    fun isEmpty(): Boolean{
        if(this.username.isEmpty() || this.username.equals("")){
            return true
        }
        return false
    }
}

