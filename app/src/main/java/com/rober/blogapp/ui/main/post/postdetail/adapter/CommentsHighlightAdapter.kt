package com.rober.blogapp.ui.main.post.postdetail.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rober.blogapp.R
import com.rober.blogapp.entity.Comment
import com.rober.blogapp.entity.User
import com.rober.blogapp.util.RecyclerViewActionInterface
import com.rober.blogapp.util.Utils

class CommentsHighlightAdapter(
    val listSelectedComment: List<Comment>,
    val listUsers: List<User>,
    val recyclerViewActionInterface: RecyclerViewActionInterface,
    val selectedCommentPosition: Int,
    val usernameReply: String?
) :
    RecyclerView.Adapter<CommentsHighlightAdapter.CommentsViewHolder>() {

    class CommentsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var containerComments: ConstraintLayout? = null
        var userPicture: ImageView? = null
        var userName: TextView? = null
        var userText: TextView? = null
        var time: TextView? = null
        var date: TextView? = null
        var options: ImageButton? = null
        var replyingText: TextView? = null
        var replyingToUsername: TextView? = null

        init {
            containerComments = itemView.findViewById(R.id.row_comment_container_comment)
            userPicture = itemView.findViewById(R.id.row_comment_uid_picture)
            userName = itemView.findViewById(R.id.row_comment_uid_name)
            userText = itemView.findViewById(R.id.row_comment_text)
            time = itemView.findViewById(R.id.row_comment_time)
            date = itemView.findViewById(R.id.row_comment_date)
            options = itemView.findViewById(R.id.row_comment_options)
            replyingText = itemView.findViewById(R.id.row_comment_reply_text)
            replyingToUsername = itemView.findViewById(R.id.row_comment_reply_text_to_username)
        }

        fun bind(
            comment: Comment,
            user: User,
            recyclerViewActionInterface: RecyclerViewActionInterface,
            highlightedComment: Boolean,
            usernameReply: String?
        ) {
            val profilePicture = user.profileImageUrl
            if (profilePicture.isNotEmpty())
                userPicture?.apply {
                    Glide.with(itemView).load(user.profileImageUrl).into(this)
                }

            userName?.text = "@${user.username}"
            userText?.text = comment.message

            val differenceText = Utils.getDifferenceTimeMilliseconds(comment.repliedAt, true)
            time?.text = differenceText

            containerComments?.setOnClickListener {
                recyclerViewActionInterface.clickListenerOnItem(adapterPosition)
            }

            if (highlightedComment) {
                time?.visibility = View.GONE

                options?.visibility = View.VISIBLE
                date?.visibility = View.VISIBLE
                replyingText?.visibility = View.VISIBLE
                replyingToUsername?.visibility = View.VISIBLE
                replyingToUsername?.text = "@${usernameReply}"

                userText?.textSize = 16f
//                val constraintLayout =
////                    ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, 300)
////                containerComments?.layoutParams = constraintLayout
            } else {
                Log.i("SeeResize", "Not resized")
            }
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.row_list_comments, parent, false)
        return CommentsViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentsViewHolder, position: Int) {
        val userId = listSelectedComment[position].commentUserId
        val userComment = listUsers.find { user -> user.userId == userId }

        var highlightedComment = false
        if (position == selectedCommentPosition)
            highlightedComment = true

        userComment?.let { tempUser ->
            holder.bind(
                listSelectedComment[position],
                tempUser,
                recyclerViewActionInterface,
                highlightedComment,
                usernameReply
            )
        }
    }

    override fun getItemCount(): Int {
        return listSelectedComment.size
    }

}