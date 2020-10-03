package com.rober.blogapp.ui.main.settings.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.rober.blogapp.R
import com.rober.blogapp.entity.Option
import com.rober.blogapp.util.RecyclerViewActionInterface
import kotlinx.android.synthetic.main.adapter_settings_viewholder_option.view.*

class AdapterSettings(newListSettings: List<Option>, val recyclerViewActionInterface: RecyclerViewActionInterface) : RecyclerView.Adapter<AdapterSettings.SettingsViewHolder>() {
    var listSettingsUser = newListSettings

    inner class SettingsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SettingsViewHolder {
        return SettingsViewHolder(
            LayoutInflater.from(
                parent.context
            ).inflate(
                R.layout.adapter_settings_viewholder_option, parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: SettingsViewHolder, position: Int) {
        val optionSettings = listSettingsUser[position]

        holder.itemView.apply {
            adapter_settings_list_row_option_text.text = optionSettings.text
            adapter_settings_list_row_option_icon.setImageResource(optionSettings.icon)

            val context = adapter_settings_list_row_option_icon.context
            if(optionSettings.text == "Delete account"){
                adapter_settings_list_row_option_text.setTextColor(ContextCompat.getColor(context, R.color.red))
                adapter_settings_list_row_option_icon.setColorFilter(ContextCompat.getColor(context, R.color.red))
            }

            setOnClickListener {
                recyclerViewActionInterface.clickListenerOnSettings(position)
            }
        }
    }

    fun setListSettings(newListSettings: List<Option>){
        listSettingsUser = newListSettings
    }

    override fun getItemCount(): Int {
        return listSettingsUser.size
    }
}