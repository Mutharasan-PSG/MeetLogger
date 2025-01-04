package com.example.MeetLogger

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.MeetLogger.databinding.FragmentMeetingBinding
import com.google.firebase.firestore.FirebaseFirestore

class MeetingFragment : Fragment() {

    private var _binding: FragmentMeetingBinding? = null
    private val binding get() = _binding!!
    private val firestore = FirebaseFirestore.getInstance()

    private lateinit var meetingId: String
    private lateinit var userId: String
    private var isCreator = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMeetingBinding.inflate(inflater, container, false)

        arguments?.getString("MEETING_ID")?.let {
            meetingId = it
            fetchMeetingDetails(meetingId)
        } ?: Toast.makeText(context, "No Meeting ID provided", Toast.LENGTH_SHORT).show()

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun fetchMeetingDetails(meetingId: String) {
        // Fetch meeting info from Firestore
        firestore.collection("meetings").get() // Modify to your structure
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    if (document["meetingId"] == meetingId) {
                        isCreator = document["creatorId"] == userId
                        initializeMeeting()
                        break
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to fetch meeting details", Toast.LENGTH_SHORT).show()
            }
    }

    private fun initializeMeeting() {
        // Initialize WebRTC logic here
        if (isCreator) {
            Toast.makeText(context, "You are the creator of this meeting.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Welcome to the meeting!", Toast.LENGTH_SHORT).show()
        }
    }
}
