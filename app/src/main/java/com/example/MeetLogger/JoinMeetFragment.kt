package com.example.MeetLogger

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.MeetLogger.databinding.FragmentJoinMeetBinding
import com.google.firebase.auth.FirebaseAuth
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

                    // Fetch existing participants
                    val participants = document["participants"] as? List<Map<String, Any>> ?: emptyList()

                    // Check if the user is already in the participants list
                    val isAlreadyParticipant = participants.any { it["userId"] == userId }

                    if (isAlreadyParticipant) {
                        // Update only the status of the user
                        val updatedParticipants = participants.map { participant ->
                            if (participant["userId"] == userId) {
                                participant.toMutableMap().apply { put("status", "active") }
                            } else {
                                participant
                            }
                        }

                        // Update the participants list
                        meetingRef.update("participants", updatedParticipants)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    context,
                                    "Updated status successfully.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                navigateToMeetingFragment(meetingId)
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Error updating status.", Toast.LENGTH_SHORT)
                                    .show()
                            }
                    } else {
                        // If not in the list, add the user as a new participant
                        val newParticipant = mapOf(
                            "userId" to userId,
                            "name" to userName,
                            "email" to userEmail,
                            "profileImage" to profileImage,
                            "status" to "active"
                        )

                        meetingRef.update("participants", FieldValue.arrayUnion(newParticipant))
                            .addOnSuccessListener {
                                // Increment the count field
                                meetingRef.update("count", FieldValue.increment(1))
                                    .addOnSuccessListener {
                                        Toast.makeText(
                                            context,
                                            "Joined meeting successfully.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        navigateToMeetingFragment(meetingId)
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(context, "Error joining meeting.", Toast.LENGTH_SHORT)
                                            .show()
                                    }
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Error joining meeting.", Toast.LENGTH_SHORT)
                                    .show()
                            }
                    }
                } else {
                    Toast.makeText(context, "Invalid Meeting ID or Passkey", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "An error occurred. Please try again.", Toast.LENGTH_SHORT).show()
            }
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
