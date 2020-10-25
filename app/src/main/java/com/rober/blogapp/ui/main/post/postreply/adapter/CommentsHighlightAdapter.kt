package com.rober.blogapp.ui.main.post.postreply.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rober.blogapp.R
import com.rober.blogapp.entity.Comment
import com.rober.blogapp.entity.User
import com.rober.blogapp.ui.main.post.postreply.PostReplyClickListener
import com.rober.blogapp.util.RecyclerViewActionInterface
import com.rober.blogapp.util.Utils

class CommentsHighlightAdapter(
    val listHighlightComments: List<Comment>,
    val listUsers: List<User>,
    val postReplyClickListener: PostReplyClickListener,
    val usernameReply: String
) :
    RecyclerView.Adapter<CommentsHighlightAdapter.CommentsViewHolder>() {

    class CommentsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var containerComments: ConstraintLayout? = null
        var userPicture: ImageView? = null
        var userName: TextView? = null
        var userText: TextView? = null
        var time: TextView? = null
        var dateTextView: TextView? = null
        var options: ImageButton? = null
        var replyingText: TextView? = null
        var replyingToUsername: TextView? = null
        var continueReplyTop: View? = null
        var continueReplyBottom: View? = null
        var topDivider: View? = null

        init {
            containerComments = itemView.findViewById(R.id.row_comment_container_comment)
            userPicture = itemView.findViewById(R.id.row_comment_uid_picture)
            userName = itemView.findViewById(R.id.row_comment_uid_name)
            userText = itemView.findViewById(R.id.row_comment_text)
            time = itemView.findViewById(R.id.row_comment_time)
            dateTextView = itemView.findViewById(R.id.row_comment_date)
            options = itemView.findViewById(R.id.row_comment_options)
            replyingText = itemView.findViewById(R.id.row_comment_reply_text)
            replyingToUsername = itemView.findViewById(R.id.row_comment_reply_text_to_username)
            continueReplyTop = itemView.findViewById(R.id.row_comment_continue_reply_top)
            continueReplyBottom = itemView.findViewById(R.id.row_comment_continue_reply_bottom)
            topDivider = itemView.findViewById(R.id.row_comment_top_divider)
        }

        fun bind(
            comment: Comment,
            user: User,
            postReplyClickListener: PostReplyClickListener,
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
                postReplyClickListener.onClickHighlightComment(adapterPosition)
            }

            topDivider?.visibility = View.GONE
            continueReplyTop?.visibility = View.VISIBLE
            continueReplyBottom?.visibility = View.VISIBLE

            if (highlightedComment) {
                time?.visibility = View.GONE
                continueReplyBottom?.visibility = View.GONE

                options?.visibility = View.VISIBLE
                dateTextView?.visibility = View.VISIBLE
                replyingText?.visibility = View.VISIBLE
                continueReplyTop?.visibility = View.VISIBLE
                replyingToUsername?.visibility = View.VISIBLE
                replyingToUsername?.text = "@${usernameReply}"

                val date = Utils.getDateDayMonthYearInSeconds(comment.repliedAt)
                val time = Utils.getDateHourMinutesInSeconds(comment.repliedAt)
                dateTextView?.text = "${date}    |    ${time}"


                userText?.textSize = 16f
                val constraintLayout =
                    ConstraintLayout.LayoutParams(
                        ConstraintLayout.LayoutParams.WRAP_CONTENT,
                        ConstraintLayout.LayoutParams.WRAP_CONTENT
                    )
                constraintLayout.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                constraintLayout.topToBottom = R.id.row_comment_reply_text
                constraintLayout.marginStart = 60

                userText?.layoutParams = constraintLayout
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
        val userId = listHighlightComments[position].commentUserId
        val commentUser = listUsers.find { user -> user.userId == userId }

        var highlightedComment = false
        if (position == listHighlightComments.size - 1)
            highlightedComment = true

        commentUser?.let { tempUser ->
            holder.bind(
                listHighlightComments[position],
                tempUser,
                postReplyClickListener,
                highlightedComment,
                usernameReply
            )
        }
    }

    override fun getItemCount(): Int {
        return listHighlightComments.size
    }

}