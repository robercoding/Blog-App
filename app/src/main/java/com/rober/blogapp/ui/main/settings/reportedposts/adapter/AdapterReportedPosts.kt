package com.rober.blogapp.ui.main.settings.reportedposts.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rober.blogapp.R
import com.rober.blogapp.entity.ReportPost

class AdapterReportedPosts(val listReportedPosts: List<ReportPost>) :
    RecyclerView.Adapter<AdapterReportedPosts.ReportedPostsViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportedPostsViewHolder {
        val customView = LayoutInflater.from(parent.context)
            .inflate(R.layout.adapter_reported_posts_viewholder, parent, false)
        return ReportedPostsViewHolder(customView)
    }

    override fun onBindViewHolder(holder: ReportedPostsViewHolder, position: Int) {
        holder.bind(listReportedPosts[position], position + 1)
    }

    class ReportedPostsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var caseNumberTv = itemView.findViewById(R.id.reported_posts_viewholder_case_number) as TextView
        var messageTitle = itemView.findViewById(R.id.reported_posts_viewholder_message_title_tv) as TextView
        var messageContent =
            itemView.findViewById(R.id.reported_posts_viewholder_message_content_tv) as TextView

        var reportedCauseTitle =
            itemView.findViewById(R.id.reported_posts_viewholder_message_title_tv) as TextView
        var reportedCauseContent =
            itemView.findViewById(R.id.reported_posts_viewholder_reported_cause_content_tv) as TextView

        fun bind(reportPost: ReportPost, caseNumber: Int) {
            caseNumberTv.text = "#$caseNumber"
            reportedCauseContent.text = reportPost.reportedCause

            if (reportPost.message.isEmpty()) {
                displayMessageSection(false)
            } else {
                displayMessageSection(true)
                messageContent.text = reportPost.message
            }
        }

        private fun displayMessageSection(display: Boolean) {
            if (display) {
                messageTitle.visibility = View.VISIBLE
                messageContent.visibility = View.VISIBLE
            } else {
                messageTitle.visibility = View.GONE
                messageContent.visibility = View.GONE
            }
        }
    }

    override fun getItemCount(): Int {
        return listReportedPosts.size
    }
}