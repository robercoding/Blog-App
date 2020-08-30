package com.rober.blogapp.entity

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import kotlinx.android.parcel.Parcelize

//@Entity(tableName = "users", indices = arrayOf(Index(value = ["username"], unique = true)))

@Parcelize
@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    @get:Exclude val db_user_id: Long = 0,
    val user_id: String = "",
    var username: String = "",
    var biography: String = "",
    var location: String = "",
    var following: Int = 0,
    var follower: Int = 0
) : Parcelable {
    constructor() : this(0, "", "", "", "", 0, 0)

    fun isEmpty(): Boolean{
        if(this.username.isEmpty() || this.username.equals("")){
            return true
        }
        return false
    }
}

