package com.rober.blogapp.ui.main.search.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.rober.blogapp.R
import com.rober.blogapp.entity.User


class UserSearchAdapter (val itemView: View, val viewHolder: Int) : RecyclerView.Adapter<UserSearchAdapter.UserViewHolder>() {

    private var TAG = "PostAdapter"

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        //return PostViewHolder()
        val view = LayoutInflater.from(itemView.context).inflate(viewHolder, parent, false)
        return UserViewHolder(view)
    }

    private val differCallback = object: DiffUtil.ItemCallback<User>(){
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.username == newItem.username
        }

        override fun getChangePayload(oldItem: User, newItem:User): Any? {
            return super.getChangePayload(oldItem, newItem)
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }

    val differ = AsyncListDiffer(this, differCallback)

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = differ.currentList[position]
        holder.bind(user)
    }

    fun setUsers(newListUser: MutableList<User>){
        differ.submitList(newListUser)
        notifyDataSetChanged()
    }

    fun clear(){
        differ.currentList.clear()
        notifyDataSetChanged()
    }

    class UserViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        var uid_image : ImageView? = null
        var uid_name : TextView? = null
        var uid_biography : TextView? = null

        init{
            uid_image = itemView.findViewById(R.id.adapter_search_profile_image)
            uid_name = itemView.findViewById(R.id.adapter_search_username)
            uid_biography = itemView.findViewById(R.id.adapter_search_biography)
        }

        fun bind(user: User){
            uid_name?.text = user.username
            uid_biography?.text = user.biography
            Log.i("UserSearchAdapter", "biography: ${user.biography}")
        }
    }
}