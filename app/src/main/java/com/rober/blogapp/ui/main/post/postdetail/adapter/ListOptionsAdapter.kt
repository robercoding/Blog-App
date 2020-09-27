package com.rober.blogapp.ui.main.post.postdetail.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.rober.blogapp.R
import com.rober.blogapp.entity.Option
import kotlinx.android.synthetic.main.listview_row_post_detail_options.view.*

class ListOptionsAdapter(private val context: Context, private val listOptions: List<Option>) : BaseAdapter() {

    private class ViewHolder(row: View?){
        var optionText : TextView? = null
        var optionIcon : ImageView? = null

        init {
            optionText = row?.findViewById(R.id.listview_row_option_text)
            optionIcon = row?.findViewById(R.id.listview_row_option_image)
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View?
        val viewHolder: ViewHolder

        if(convertView == null){
            var layout = LayoutInflater.from(context)

            view = layout.inflate(R.layout.listview_row_post_detail_options, parent, false)
            viewHolder = ViewHolder(view)
            view.tag = viewHolder
        }else {
            view = convertView
            viewHolder = view.tag as ViewHolder
        }
        val option : Option = getItem(position)
        viewHolder.optionText?.text = option.text
        viewHolder.optionIcon?.setImageResource(option.icon)

        return view as View
    }

    override fun getCount(): Int {
        return listOptions.size
    }

    override fun getItem(position: Int) : Option {
        return listOptions.get(position)
    }


    override fun getItemId(position: Int): Long {
        return position.toLong()
    }
}