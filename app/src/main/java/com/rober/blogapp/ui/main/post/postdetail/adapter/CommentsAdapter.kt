package com.rober.blogapp.ui.main.post.postdetail.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rober.blogapp.R
import com.rober.blogapp.entity.Comment
import com.rober.blogapp.entity.User
import com.rober.blogapp.util.Utils

class CommentsAdapter(val listComments: List<Comment>, val listUsers: List<User>) :
    RecyclerView.Adapter<CommentsAdapter.CommentsViewHolder>() {

    class CommentsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var userPicture: ImageView? = null
        var userName: TextView? = null
        var userText: TextView? = null
        var time: TextView? = null

        init {
            userPicture = itemView.findViewById(R.id.row_comment_uid_picture)
            userName = itemView.findViewById(R.id.row_comment_uid_name)
            userText = itemView.findViewById(R.id.row_comment_text)
            time = itemView.findViewById(R.id.row_comment_time)
        }

        fun bind(comment: Comment, user: User) {
            val profilePicture = user.profileImageUrl
            if (profilePicture.isNotEmpty())
                userPicture?.apply {
                    Glide.with(itemView).load(user.profileImageUrl).into(this)
                }

            userName?.text = user.username
            userText?.text = comment.message

            val differenceText = Utils.getDifferenceTimeMilliseconds(comment.repliedAt, true)
            time?.text = differenceText
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.row_list_comments, parent, false)
        return CommentsViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentsViewHolder, position: Int) {
        val userId = listComments[position].commentUserId
        val userComment = listUsers.find { user -> user.userId == userId }
        userComment?.let { tempUser ->
            holder.bind(listComments[position], tempUser)
        }
    }

    override fun getItemCount(): Int {
        return listComments.size
    }
}