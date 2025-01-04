package com.example.MeetLogger

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.example.MeetLogger.databinding.ItemParticipantBinding

class ParticipantsAdapter(private val context: Context, private val participants: List<Participant>) : BaseAdapter() {

    // Inflate item layout for each grid item (participant)
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val binding = ItemParticipantBinding.inflate(LayoutInflater.from(context), parent, false)

        val participant = getItem(position)

        binding.nameTextView.text = participant.name
        binding.profileImageView.setImageResource(participant.profileImage) // You can also load the image from a URL

        return binding.root
    }

    override fun getItem(position: Int): Participant {
        return participants[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return participants.size
    }
}
