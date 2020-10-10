package com.rober.blogapp.ui.main.settings.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.rober.blogapp.R
import com.rober.blogapp.entity.Option
import com.rober.blogapp.ui.main.settings.utils.RowsNaming
import com.rober.blogapp.util.RecyclerViewActionInterface

class AdapterSettings(
    newListSettings: List<Option>,
    val recyclerViewActionInterface: RecyclerViewActionInterface,
    val sumAdapterPositionToOtherOptions: Int,
    val rowsNaming: RowsNaming,
    val totalNumberPosts: Int
) : RecyclerView.Adapter<AdapterSettings.SettingsViewHolder>() {
    var listSettingsUser = newListSettings

    class SettingsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val container = itemView.findViewById(R.id.adapter_settings_list_container) as ConstraintLayout
        val row_text = itemView.findViewById(R.id.adapter_settings_list_row_option_text) as TextView
        val row_icon = itemView.findViewById(R.id.adapter_settings_list_row_option_icon) as ImageView
        val row_extra = itemView.findViewById(R.id.adapter_settings_list_row_option_extra) as TextView

        fun bind(
            option: Option,
            recyclerViewActionInterface: RecyclerViewActionInterface,
            sumAdapterPositionToOtherOptions: Int,
            rowsNaming: RowsNaming,
            totalNumberPosts: Int
        ) {
            row_text.text = option.text
            row_icon.setImageResource(option.icon)

            changeColorIfRowIsDeleteAccount(option, rowsNaming)
            setRowNotClickable(option, rowsNaming)
            setRowExtraData(option, rowsNaming, totalNumberPosts)

            container.setOnClickListener {
                recyclerViewActionInterface.clickListenerOnItem(adapterPosition + sumAdapterPositionToOtherOptions)
            }
        }

        private fun changeColorIfRowIsDeleteAccount(option: Option, rowsNaming: RowsNaming) {
            val context = row_text.context

            if (option.text == rowsNaming.DELETE_ACCOUNT) {
                row_text.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.red
                    )
                )
                row_icon.setColorFilter(
                    ContextCompat.getColor(
                        context,
                        R.color.red
                    )
                )
            }
        }

        private fun setRowNotClickable(option: Option, rowsNaming: RowsNaming) {
            if (option.text == rowsNaming.POSTS) {
                container.background = ContextCompat.getDrawable(row_text.context, R.color.primaryBackground)
            }
        }

        private fun setRowExtraData(option: Option, rowsNaming: RowsNaming, totalNumberPosts: Int) {
            if (option.text == rowsNaming.POSTS) {
                row_extra.text = ": ${totalNumberPosts}"
                row_extra.visibility = View.VISIBLE
            }
        }
    }

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

        holder.bind(
            optionSettings,
            recyclerViewActionInterface,
            sumAdapterPositionToOtherOptions,
            rowsNaming,
            totalNumberPosts
        )
    }

    fun setListSettings(newListSettings: List<Option>) {
        listSettingsUser = newListSettings
    }

    override fun getItemCount(): Int {
        return listSettingsUser.size
    }
}