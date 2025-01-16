package com.example.MeetLogger

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ParticipantsAdapter(
    private val participants: List<Map<String, String>>,
    private val calculateItemHeight: () -> Int
) : RecyclerView.Adapter<ParticipantsAdapter.ParticipantViewHolder>() {

    inner class ParticipantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val participantName: TextView = itemView.findViewById(R.id.participantName)
        val participantImage: ImageView = itemView.findViewById(R.id.participantImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParticipantViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.participant_grid_item, parent, false)
        val params = view.layoutParams
        params.height = calculateItemHeight()
        view.layoutParams = params
        return ParticipantViewHolder(view)
    }

    override fun onBindViewHolder(holder: ParticipantViewHolder, position: Int) {
        val participant = participants[position]
        holder.participantName.text = participant["name"] ?: "Unknown"

        val profileImageUrl = participant["profileImage"]

        // Load profile image using Glide
        if (!profileImageUrl.isNullOrEmpty()) {
            Glide.with(holder.participantImage.context)
                .load(profileImageUrl)
                .placeholder(R.drawable.default_profile_pic)
                .error(R.drawable.default_profile_pic)
                .circleCrop()
                .into(holder.participantImage)
        } else {
            holder.participantImage.setImageResource(R.drawable.default_profile_pic) // Default image
        }
    }

    override fun getItemCount(): Int = participants.size
}
