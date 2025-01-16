package com.example.MeetLogger

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.MeetLogger.databinding.FragmentMeetingBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MeetingFragment : Fragment() {

    private var _binding: FragmentMeetingBinding? = null
    private val binding get() = _binding!!
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var isCreator = false
    private lateinit var meetingId: String
    private lateinit var userId: String
    private var participantsList: MutableList<Map<String, String>> = mutableListOf()
    private lateinit var adapter: ParticipantsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMeetingBinding.inflate(inflater, container, false)

        // Extract meeting ID and set up user ID
        arguments?.getString("MEETING_ID")?.let {
            meetingId = it
            userId = auth.currentUser?.uid.orEmpty()
            fetchMeetingDetails(meetingId)
        } ?: run {
            Toast.makeText(context, "No Meeting ID provided", Toast.LENGTH_SHORT).show()
        }

        setupParticipantsRecyclerView()

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupParticipantsRecyclerView() {
        val recyclerView = binding.participantsRecyclerView
        adapter = ParticipantsAdapter(participantsList, ::calculateItemHeight)
        recyclerView.adapter = adapter

        val gridLayoutManager = GridLayoutManager(requireContext(), 1)
        recyclerView.layoutManager = gridLayoutManager

        // Fetch participants from Firestore before setting the grid
        firestore.collection("MeetingInfo")
            .whereEqualTo("meetingId", meetingId)
            .get()
            .addOnSuccessListener { documents ->
                val participants = documents.flatMap {
                    it["participants"] as? List<Map<String, String>> ?: emptyList()
                }.filter { it["status"] != "inactive" }

                updateParticipantsGrid(participants)
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to load participants: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateParticipantsGrid(participants: List<Map<String, String>>) {
        participantsList.clear()
        participantsList.addAll(participants)

        val activeParticipants = participantsList.size
        val spanCount = when {
            activeParticipants <= 2 -> 1
            activeParticipants <= 4 -> 2
            else -> 2 // Keep 2 columns for larger groups
        }

        (binding.participantsRecyclerView.layoutManager as GridLayoutManager).spanCount = spanCount
        adapter.notifyDataSetChanged()
    }

    private fun calculateItemHeight(): Int {
        val displayMetrics = requireContext().resources.displayMetrics
        val totalHeight = displayMetrics.heightPixels
        val participantCount = participantsList.size
        return when {
            participantCount <= 2 -> (totalHeight - dpToPx(100)) / participantCount
            participantCount <= 4 -> (totalHeight - dpToPx(150)) / 2
            else -> (totalHeight - dpToPx(150)) / 4 // Scrolling case
        }
    }

    private fun dpToPx(dp: Int): Int {
        val density = requireContext().resources.displayMetrics.density
        return (dp * density).toInt()
    }

    private fun fetchMeetingDetails(meetingId: String) {
        firestore.collection("MeetingInfo")
            .whereEqualTo("meetingId", meetingId)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    if (document["meetingId"] == meetingId) {
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
        if (isCreator) {
            Toast.makeText(context, "You are the creator of this meeting.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Welcome to the meeting!", Toast.LENGTH_SHORT).show()
        }

        binding.leaveMeetingButton.setOnClickListener { leaveMeeting() }
        binding.muteUnmuteButton.setOnClickListener { toggleMic() }
    }

    private fun leaveMeeting() {
        firestore.collection("MeetingInfo")
            .whereEqualTo("meetingId", meetingId)
            .get()
            .addOnSuccessListener { documents ->
                documents.forEach { doc ->
                    val meetingRef = doc.reference
                    val participants = doc.get("participants") as? List<Map<String, Any>> ?: emptyList()

                    val updatedParticipants = participants.map { participant ->
                        if (participant["userId"] == userId) {
                            participant.toMutableMap().apply { put("status", "inactive") }
                        } else {
                            participant
                        }
                    }

                    meetingRef.update("participants", updatedParticipants)
                        .addOnSuccessListener {
                            updateMeetingCount()
                            parentFragmentManager.beginTransaction()
                                .replace(R.id.fragment_container, HomeFragment())
                                .addToBackStack(null)
                                .commit()
                            Toast.makeText(context, "You have left the meeting.", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { error ->
                            Toast.makeText(context, "Failed to leave meeting: ${error.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { error ->
                Toast.makeText(context, "Error fetching meeting data: ${error.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateMeetingCount() {
        firestore.collection("MeetingInfo")
            .whereEqualTo("meetingId", meetingId)
            .get()
            .addOnSuccessListener { documents ->
                documents.firstOrNull()?.reference?.update("count", participantsList.size)
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to update participant count", Toast.LENGTH_SHORT).show()
            }
    }

    private fun toggleMic() {
        Toast.makeText(context, "Toggle mic functionality not yet implemented.", Toast.LENGTH_SHORT).show()
    }
}
