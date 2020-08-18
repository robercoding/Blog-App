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
import com.rober.blogapp.R
import com.rober.blogapp.entity.Post
import com.rober.blogapp.util.RecyclerViewActionInterface


class PostAdapter (val itemView: View, val viewHolder: Int, val recyclerViewActionInterface: RecyclerViewActionInterface) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    private var TAG = "PostAdapter"


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        //return PostViewHolder()
        val view = LayoutInflater.from(itemView.context).inflate(viewHolder, parent, false)
        return PostViewHolder(view, recyclerViewActionInterface)
    }

    private val differCallback = object: DiffUtil.ItemCallback<Post>(){
        override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem.post_id == newItem.post_id
        }

        override fun getChangePayload(oldItem: Post, newItem: Post): Any? {
            return super.getChangePayload(oldItem, newItem)
        }

        override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem == newItem
        }
    }

    val differ = AsyncListDiffer(this, differCallback)

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = differ.currentList[position]
        holder.bind(post)

//        if(position == differ.currentList.size -1){
//            recyclerViewActionInterface.loadOldFeedPosts()
//        }
    }

    fun setPosts(newListPost: MutableList<Post>){
        differ.submitList(newListPost)
        notifyDataSetChanged()
    }

    fun clear(){
        differ.currentList.clear()
        notifyDataSetChanged()
    }

    class PostViewHolder(itemView: View, val recyclerViewActionInterface: RecyclerViewActionInterface): RecyclerView.ViewHolder(itemView) {

        var uid_image : ImageView? = null
        var uid_name : TextView? = null
        var title: TextView? = null
        var text: TextView? = null

        var container_post: ConstraintLayout? = null
        var container_no_more_posts: ConstraintLayout? = null

        init{
            uid_image = itemView.findViewById(R.id.uid_image)
            uid_name = itemView.findViewById(R.id.uid_name)
            title = itemView.findViewById(R.id.title)
            text = itemView.findViewById(R.id.text)
            container_post = itemView.findViewById(R.id.feed_viewholder_container_post)
            container_no_more_posts = itemView.findViewById(R.id.feed_viewholder_container_no_more_posts)
        }

        fun bind(post: Post){
            Log.i("bind", post.user_creator_id)
            if(post.post_id == "no_more_posts"){
                container_no_more_posts?.visibility = View.VISIBLE
                container_post?.visibility = View.GONE
            }else{
                uid_name?.text = "@${post.user_creator_id}"
                title?.text = post.title
                text?.text = post.text
                container_no_more_posts?.visibility = View.GONE
                container_post?.visibility = View.VISIBLE
            }
            setupClickListeners()
        }

        private fun setupClickListeners(){
            title?.setOnClickListener {recyclerViewActionInterface.clickListenerOnPost(adapterPosition)}
            text?.setOnClickListener {recyclerViewActionInterface.clickListenerOnPost(adapterPosition)}

            uid_name?.setOnClickListener { recyclerViewActionInterface.clickListenerOnUser(adapterPosition) }
            uid_image?.setOnClickListener { recyclerViewActionInterface.clickListenerOnUser(adapterPosition)}
        }
    }
}