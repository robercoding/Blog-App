package com.rober.blogapp.entity

import androidx.room.*
import com.google.firebase.firestore.Exclude
import com.rober.blogapp.util.Converters
import java.util.*


@Entity
@TypeConverters(Converters::class)
data class Post (
    @PrimaryKey(autoGenerate = true)
    @get:Exclude var id: Long,
    var post_id: String,
    var title: String,
    var text: String,
    var user_creator_id: String,
    val created_at: Date,
    var likes: Int
){

    constructor() : this(0, "","", "", "", Date(), 0)
}