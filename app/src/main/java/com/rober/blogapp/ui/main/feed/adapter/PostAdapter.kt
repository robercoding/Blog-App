package com.rober.blogapp.ui.main.feed.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rober.blogapp.R
import com.rober.blogapp.entity.Post


class PostAdapter(val itemView: View, val initListPost: List<Post>) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

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
    }

    fun setPosts(newListPost: List<Post>){
        listPosts = newListPost
    }

    class PostViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        var uid : TextView? = null
        var title: TextView? = null

        init{
            uid = itemView.findViewById(R.id.uid)
            title = itemView.findViewById(R.id.title)
        }

        fun bind(post: Post){
            uid?.text = post.post_id
            title?.text = post.title
        }
    }
}