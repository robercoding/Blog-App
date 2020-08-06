package com.rober.blogapp.ui.main.feed.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rober.blogapp.R
import com.rober.blogapp.entity.Post


class PostAdapter(val itemView: View, val initListPost: List<Post>) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    private var TAG = "PostAdapter"

    private var listPosts: List<Post> = initListPost
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        //return PostViewHolder()
        val view = LayoutInflater.from(itemView.context).inflate(R.layout.adapter_post_holder, parent, false)
        return PostViewHolder(view)
    }

    override fun getItemCount(): Int {
        return listPosts.size
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(listPosts[position])
        Log.i(TAG, "${listPosts[position].user_creator_id}")
    }

    fun setPosts(newListPost: List<Post>){
        listPosts = newListPost
    }

    class PostViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        var uid_image : ImageView? = null
        var uid_name : TextView? = null
        var title: TextView? = null
        var text: TextView? = null

        init{
            uid_image = itemView.findViewById(R.id.uid_image)
            uid_name = itemView.findViewById(R.id.uid_name)
            title = itemView.findViewById(R.id.title)
            text = itemView.findViewById(R.id.text)

        }

        fun bind(post: Post){
            Log.i("bind", "${post.user_creator_id}")
            uid_name?.text = "@${post.user_creator_id}"
            title?.text = post.title
            text?.text = post.text
        }
    }
}