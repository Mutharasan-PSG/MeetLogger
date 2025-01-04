package com.example.MeetLogger

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.MeetLogger.databinding.FragmentJoinMeetBinding
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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
        firestore.collection("MeetingInfo")
            .whereEqualTo("meetingId", meetingId)
            .whereEqualTo("passkey", passkey)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot != null && !querySnapshot.isEmpty) {
                    navigateToMeetingFragment(meetingId)
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
        val meetingFragment = MeetingFragment().apply {
            arguments = Bundle().apply {
                putString("MEETING_ID", meetingId)
            }
        }

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, meetingFragment)
            .addToBackStack(null)
            .commit()
    }
}
