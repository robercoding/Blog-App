package com.rober.blogapp.ui.main.settings.utils

import android.app.Application
import com.rober.blogapp.R
import javax.inject.Inject

class RowsNaming @Inject constructor(val application: Application) {
    val REPORTED_POSTS = getString(R.string.row_list_settings_reports)
    val POSTS = getString(R.string.row_list_settings_count_posts)
    val DELETE_ACCOUNT = getString(R.string.row_list_settings_delete_account)
    val PREFERENCES = getString(R.string.row_list_settings_preferences)

    private fun getString(resource: Int): String {
        return application.applicationContext.getString(resource)
    }
}