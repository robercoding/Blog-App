package com.rober.blogapp.entity

import android.os.Parcelable
import androidx.room.*
import com.google.firebase.firestore.Exclude
import com.rober.blogapp.util.Converters
import kotlinx.android.parcel.Parcelize
import java.util.*


@Parcelize
@Entity
@TypeConverters(Converters::class)
data class Post (
    @PrimaryKey(autoGenerate = true)
    @get:Exclude var id: Long,
    var postId: String,
    var title: String,
    var text: String,
    var userCreatorId: String,
    val createdAt: Long,
    var likes: Int
) : Parcelable {

    constructor() : this(0, "","", "", "", 0, 0)
}