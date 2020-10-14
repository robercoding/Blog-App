package com.rober.blogapp.ui.main.feed.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rober.blogapp.R
import com.rober.blogapp.entity.Post
import com.rober.blogapp.entity.User
import com.rober.blogapp.util.RecyclerViewActionInterface
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs


class PostAdapter(val itemView: View, val viewHolder: Int, val recyclerViewActionInterface: RecyclerViewActionInterface) :
    RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        //return PostViewHolder()
        val view = LayoutInflater.from(itemView.context).inflate(viewHolder, parent, false)
        return PostViewHolder(view, recyclerViewActionInterface, differUser)
    }

    private val differPostCallback = object : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem.postId == newItem.postId
        }

        override fun getChangePayload(oldItem: Post, newItem: Post): Any? {
            return super.getChangePayload(oldItem, newItem)
        }

        override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem == newItem
        }
    }

    private val differUserCallback = object : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.userId == newItem.userId
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }

    private val differPost = AsyncListDiffer(this, differPostCallback)
    val differUser = AsyncListDiffer(this, differUserCallback)


    override fun getItemCount(): Int {
        return differPost.currentList.size
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = differPost.currentList[position]
        holder.bind(post)

//        if(position == differPost.currentList.size -1){
//            recyclerViewActionInterface.loadOldFeedPosts()
//        }
    }

    fun setPosts(newListPost: MutableList<Post>) {
        differPost.submitList(newListPost)
        notifyDataSetChanged()
    }

    fun setUsers(newListUsers: MutableList<User>) {
        differUser.submitList(newListUsers)
        notifyDataSetChanged()
    }

    class PostViewHolder(
        itemView: View,
        val recyclerViewActionInterface: RecyclerViewActionInterface,
        val differUser: AsyncListDiffer<User>
    ) : RecyclerView.ViewHolder(itemView) {

        var uid_image: ImageView? = null
        var uid_name: TextView? = null
        var title: TextView? = null
        var text: TextView? = null
        var time: TextView? = null

        var container_post: ConstraintLayout? = null
        var container_no_more_posts: ConstraintLayout? = null

        init {
            uid_image = itemView.findViewById(R.id.uid_image)
            uid_name = itemView.findViewById(R.id.uid_name)
            title = itemView.findViewById(R.id.title)
            text = itemView.findViewById(R.id.text)
            time = itemView.findViewById(R.id.time)
            container_post = itemView.findViewById(R.id.feed_viewholder_container_post)
            container_no_more_posts = itemView.findViewById(R.id.feed_viewholder_container_no_more_posts)
        }

        fun bind(post: Post) {
            Log.i("PostAdapter", post.userCreatorId)
            Log.i("PostAdapter", "${differUser.currentList}")
            if (post.createdAt == 0.toLong()) {
                container_no_more_posts?.visibility = View.VISIBLE
                container_post?.visibility = View.GONE
            } else {
                val user = differUser.currentList.find { user -> user.userId == post.userCreatorId }
                user?.let { tempUser ->
                    uid_image?.let {
                        Glide.with(itemView)
                            .load(tempUser.profileImageUrl)
                            .into(it)
                    }

                    uid_name?.text = "@${tempUser.username}"
                    val diffTime = getDifferenceTime(post.createdAt)
                    time?.text = diffTime
                    setupClickListeners()
                } ?: kotlin.run {
                    uid_name?.text = "@Unknown user"
                }
                title?.text = post.title
                text?.text = post.text
                container_no_more_posts?.visibility = View.GONE
                container_post?.visibility = View.VISIBLE
                time?.visibility = View.VISIBLE
            }
        }

        private fun setupClickListeners() {
            title?.setOnClickListener { recyclerViewActionInterface.clickListenerOnPost(adapterPosition) }
            text?.setOnClickListener { recyclerViewActionInterface.clickListenerOnPost(adapterPosition) }
            container_post?.setOnClickListener { recyclerViewActionInterface.clickListenerOnPost(adapterPosition) }

            uid_name?.setOnClickListener { recyclerViewActionInterface.clickListenerOnUser(adapterPosition) }
            uid_image?.setOnClickListener { recyclerViewActionInterface.clickListenerOnUser(adapterPosition) }
        }

        private fun getDifferenceTime(postDate: Long): String {
            val instant = Instant.ofEpochSecond(postDate)
            val instantPostMillis = instant.toEpochMilli()

            val instantNow = Instant.now().toEpochMilli()
            val diff = abs(instantNow - instantPostMillis)
            val diffDays = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS).toInt()

            when (diffDays) {
                in 1..364 -> {
                    return "${diffDays}d"
                }

                0 -> {
                    val diffHours = TimeUnit.HOURS.convert(diff, TimeUnit.MILLISECONDS).toInt()
                    if (diffHours > 0) {
                        return "${diffHours}h"
                    }
                    val diffMinutes = TimeUnit.MINUTES.convert(diff, TimeUnit.MILLISECONDS).toInt()
                    if (diffMinutes > 0) {
                        return "${diffMinutes}m"
                    }
                    val diffSeconds = TimeUnit.SECONDS.convert(diff, TimeUnit.MILLISECONDS).toInt()
                    return "${diffSeconds}s "
                }

                else -> {
                    val diffDays = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS).toInt()
                    return "${diffDays / 365}y"
                }

            }
        }
    }


}