
package com.example.MeetLogger

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.MeetLogger.databinding.FragmentMeetingBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*

class MeetingFragment : Fragment() {

    private var _binding: FragmentMeetingBinding? = null
    private val binding get() = _binding!!
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var meetingId: String
    private lateinit var userId: String
    private var isCreator = false
    private lateinit var participantsList: MutableList<Map<String, String>>

    private val participantAdapter: ArrayAdapter<String> by lazy {
        ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMeetingBinding.inflate(inflater, container, false)

        arguments?.getString("MEETING_ID")?.let {
            meetingId = it
            userId = auth.currentUser?.uid ?: ""
            fetchMeetingDetails(meetingId)
        } ?: Toast.makeText(context, "No Meeting ID provided", Toast.LENGTH_SHORT).show()

        setupParticipantsGridView()

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupParticipantsGridView() {
        val participantsGridView = binding.participantsGridView
        participantsList = mutableListOf()

        participantsGridView.adapter = participantAdapter

        // Listen for real-time updates in the participants collection
        firestore.collection("MeetingInfo")
            .whereEqualTo("meetingId", meetingId)
            .addSnapshotListener { querySnapshot, e ->
                if (e != null) {
                    Toast.makeText(context, "Error fetching participants", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                querySnapshot?.documents?.forEach { document ->
                    val participants = document["participants"] as? List<Map<String, String>> ?: emptyList()
                    updateParticipantList(participants)
                }
            }
    }

    private fun updateParticipantList(participants: List<Map<String, String>>) {
        // Filter out inactive participants
        val activeParticipants = participants.filter { it["status"] != "inactive" }

        // Update the participants list with only active ones
        participantsList.clear()
        participantsList.addAll(activeParticipants)

        val participantNames = participantsList.map { it["name"].toString() }
        participantAdapter.clear()
        participantAdapter.addAll(participantNames)

        // Update count and handle UI changes (expand/shrink GridView dynamically)
        updateMeetingCount()
    }

    private fun fetchMeetingDetails(meetingId: String) {
        firestore.collection("MeetingInfo").get()
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
        if (isCreator) {
            Toast.makeText(context, "You are the creator of this meeting.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Welcome to the meeting!", Toast.LENGTH_SHORT).show()
        }

        // Leave meeting button logic
        binding.leaveMeetingButton.setOnClickListener {
            leaveMeeting()
        }

        // Mute/unmute mic button logic
        binding.muteUnmuteButton.setOnClickListener {
            toggleMic()
        }
    }

    private fun updateMeetingCount() {
        // Count only active participants
        val activeCount = participantsList.size

        firestore.collection("MeetingInfo")
            .whereEqualTo("meetingId", meetingId)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty()) {
                    val doc = documents.first()
                    val meetingRef = doc.reference

                    // Decrement the count only for leaving participant, we use the current size of active participants
                    meetingRef.update("count", activeCount)
                }
            }
    }


    private fun toggleMic() {
        // Implement mute/unmute logic here
    }
    private fun leaveMeeting() {
        // Update the participant's status to inactive and decrease the participant count
        firestore.collection("MeetingInfo")
            .whereEqualTo("meetingId", meetingId)
            .get()
            .addOnSuccessListener { documents ->
                documents.forEach { doc ->
                    val meetingRef = doc.reference

                    // Fetch the current participants
                    val participants = doc.get("participants") as? List<Map<String, Any>> ?: listOf()

                    // Create a new list with updated status for the userId
                    val updatedParticipants = participants.map { participant ->
                        if (participant["userId"] == userId) {
                            participant.toMutableMap().apply { put("status", "inactive") }
                        } else {
                            participant
                        }
                    }

                    // Now update the 'participants' list in the Firestore document
                    meetingRef.update("participants", updatedParticipants)
                        .addOnSuccessListener {
                            // Update the participant count after leaving
                            updateMeetingCount()

                            // Navigate back to the home fragment
                            parentFragmentManager.beginTransaction()
                                .replace(R.id.fragment_container, HomeFragment())
                                .addToBackStack(null)
                                .commit()

                            // Show success message
                            Toast.makeText(context, "You have left the meeting.", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { error ->
                            Toast.makeText(
                                context,
                                "Failed to leave meeting. Please try again: ${error.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
            }
            .addOnFailureListener { error ->
                Toast.makeText(context, "Error fetching meeting data: ${error.message}", Toast.LENGTH_SHORT).show()
            }
    }


}