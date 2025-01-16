package com.example.MeetLogger

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.MeetLogger.databinding.FragmentJoinMeetBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class JoinMeetFragment : Fragment() {

    private var _binding: FragmentJoinMeetBinding? = null
    private val binding get() = _binding!!
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentJoinMeetBinding.inflate(inflater, container, false)
        setupListeners()
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupListeners() {
        binding.joinMeetingButton.setOnClickListener {
            val meetingId = binding.meetingIdEditText.text.toString().trim()
            val passkey = binding.passkeyEditText.text.toString().trim()

            if (meetingId.isEmpty() || passkey.isEmpty()) {
                Toast.makeText(context, "Please enter both Meeting ID and Passkey", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Update button UI and disable it temporarily
            binding.joinMeetingButton.text = "Joining..."
            binding.joinMeetingButton.setTextColor(resources.getColor(R.color.red, null)) // Set color to red
            binding.joinMeetingButton.isEnabled = false

            validateMeeting(meetingId, passkey)
        }
    }

    private fun validateMeeting(meetingId: String, passkey: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid ?: return
        val userEmail = currentUser.email ?: "Unknown"
        val userName = currentUser.displayName ?: "Anonymous User"
        val profileImage = currentUser.photoUrl?.toString() ?: ""

        firestore.collection("MeetingInfo")
            .whereEqualTo("meetingId", meetingId)
            .whereEqualTo("passkey", passkey)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot != null && !querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0]
                    val meetingRef = firestore.collection("MeetingInfo").document(document.id)

                    // Check if the user is already a participant
                    val participants = document["participants"] as? List<Map<String, Any>> ?: emptyList()
                    val isAlreadyParticipant = participants.any { it["userId"] == userId }

                    if (isAlreadyParticipant) {
                        // Update only status
                        updateParticipantStatus(meetingRef, participants, userId,meetingId)
                    } else {
                        // Add as a new participant
                        addParticipant(meetingRef, userId, userName, userEmail, profileImage,meetingId)
                    }
                } else {
                    showErrorAndResetUI("Invalid Meeting ID or Passkey")
                }
            }
            .addOnFailureListener {
                showErrorAndResetUI("An error occurred. Please try again.")
            }
    }

    private fun updateParticipantStatus(meetingRef: DocumentReference, participants: List<Map<String, Any>>, userId: String,meetingId: String) {
        val updatedParticipants = participants.map { participant ->
            if (participant["userId"] == userId) {
                participant.toMutableMap().apply { put("status", "active") }
            } else {
                participant
            }
        }

        meetingRef.update("participants", updatedParticipants)
            .addOnSuccessListener {
                navigateToMeetingFragmentAndResetUI(meetingId)
            }
            .addOnFailureListener {
                showErrorAndResetUI("Error updating status.")
            }
    }

    private fun addParticipant(meetingRef: DocumentReference, userId: String, userName: String, userEmail: String, profileImage: String,meetingId: String) {
        val newParticipant = mapOf(
            "userId" to userId,
            "name" to userName,
            "email" to userEmail,
            "profileImage" to profileImage,
            "status" to "active"
        )

        meetingRef.update("participants", FieldValue.arrayUnion(newParticipant))
            .addOnSuccessListener {
                meetingRef.update("count", FieldValue.increment(1))
                    .addOnSuccessListener {
                        navigateToMeetingFragmentAndResetUI(meetingId)
                    }
                    .addOnFailureListener {
                        showErrorAndResetUI("Error joining meeting.")
                    }
            }
            .addOnFailureListener {
                showErrorAndResetUI("Error joining meeting.")
            }
    }

    private fun navigateToMeetingFragmentAndResetUI(meetingId: String) {
        navigateToMeetingFragment(meetingId)
        resetJoinMeetingUI()
    }

    private fun showErrorAndResetUI(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        resetJoinMeetingUI()
    }

    private fun resetJoinMeetingUI() {
        binding.joinMeetingButton.text = "Join Meeting"
        binding.joinMeetingButton.setTextColor(resources.getColor(R.color.black, null)) // Reset color to default
        binding.joinMeetingButton.isEnabled = true
    }

    private fun getHourSlot(): String {
        val calendar = android.icu.util.Calendar.getInstance()
        val hour = calendar.get(android.icu.util.Calendar.HOUR)
        val isAM = calendar.get(android.icu.util.Calendar.AM_PM) == android.icu.util.Calendar.AM
        val period = if (isAM) "AM" else "PM"
        val adjustedHour = if (hour == 0) 12 else hour
        return String.format("%02d:00 %s", adjustedHour, period)
    }

    private fun navigateToMeetingFragment(meetingId: String) {
        val fragmentTransaction = parentFragmentManager.beginTransaction()

        val currentFragment = parentFragmentManager.findFragmentById(R.id.fragment_container)
        if (currentFragment != null) {
            fragmentTransaction.hide(currentFragment)
        }

        val meetingFragment = parentFragmentManager.findFragmentByTag("MeetingFragment") as? MeetingFragment
        if (meetingFragment != null) {
            fragmentTransaction.show(meetingFragment)
        } else {
            fragmentTransaction.add(
                R.id.fragment_container,
                MeetingFragment().apply {
                    arguments = Bundle().apply {
                        putString("MEETING_ID", meetingId)
                    }
                },
                "MeetingFragment"
            )
        }

        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()
    }
}
