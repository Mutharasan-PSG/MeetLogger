package com.example.MeetLogger

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import com.example.MeetLogger.databinding.FragmentCreateMeetBottomsheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

class CreateMeetBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentCreateMeetBottomsheetBinding? = null
    private val binding get() = _binding!!
    private lateinit var uniqueMeetLink: String
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateMeetBottomsheetBinding.inflate(inflater, container, false)
        generateRealTimeMeetingLink()
        setupListeners()
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun generateUniqueMeetingId(): String {
        val charPool = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        val randomId = (1..10).map { charPool.random() }.joinToString("")
        return "MLog_$randomId"
    }


    private fun generatePasskey(): String {
        return (10000..99999).random().toString()
    }

    private fun generateRealTimeMeetingLink() {
        val meetingId = generateUniqueMeetingId()
        val passkey = generatePasskey()
        val currentUser = auth.currentUser
        val userId = currentUser?.uid ?: "Anonymous"
        val userEmail = currentUser?.email ?: "Unknown"
        val userName = currentUser?.displayName ?: "Anonymous User"

        val calendar = Calendar.getInstance()
        val year = SimpleDateFormat("yyyy", Locale.getDefault()).format(calendar.time)
        val month = SimpleDateFormat("MMMM", Locale.getDefault()).format(calendar.time)
        val day = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(calendar.time)

        val combinedDocId = "$year-$month-$day-$meetingId"

        // Data to store
        val meetingData = mapOf(
            "meetingId" to meetingId,
            "passkey" to passkey,
            "status" to "active",
            "creatorId" to userId,
            "creatorEmail" to userEmail,
            "creatorName" to userName,
            "createdAt" to Timestamp.now()
        )

        // Save to Combined Structure
        firestore.collection("MeetingInfo").document(combinedDocId)
            .set(meetingData)
            .addOnSuccessListener {
                uniqueMeetLink = "$meetingId"
                binding.meetLinkTextView.text = uniqueMeetLink
                binding.passKeyTextView.text = passkey
                Log.d("Firestore", "Successfully saved meeting to MeetingInfo Collection in DB")
                // Save to Old Hierarchical Structure
                saveToOldStructure(year, month, day, meetingId,meetingData)
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to generate meet link", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveToOldStructure(
        year: String,
        month: String,
        day: String,
        meetingId: String,
        meetingData: Map<String, Any>
    ) {
        // Call generateTimeSlots to get the time slot for the current hour
        val currentHour = LocalTime.now().hour
        val formatter = DateTimeFormatter.ofPattern("h:00 a")
        val timeSlot = LocalTime.of(currentHour, 0).format(formatter)

        firestore.collection("MeetingData")
            .document(year) // Year document
            .collection(month) // Month collection
            .document(day) // Day document
            .collection(timeSlot) // Time slot collection
            .document(meetingId) // Meeting ID document
            .set(meetingData) // Meeting data fields
            .addOnSuccessListener {
                Log.d("Firestore", "Successfully saved meeting to MeetingData Collection in DB")
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to save to old structure", Toast.LENGTH_SHORT).show()
            }
    }




    private fun setupListeners() {
        binding.copyIcon.setOnClickListener {
            copyToClipboard(uniqueMeetLink)
        }

        binding.passKeyCopyIcon.setOnClickListener {
            copyToClipboard(binding.passKeyTextView.text.toString())
        }


        binding.shareButton.setOnClickListener {
            shareLink(uniqueMeetLink, binding.passKeyTextView.text.toString())
        }

        binding.joinButton.setOnClickListener {
            joinMeeting(uniqueMeetLink)
        }

        binding.dismissTextView.setOnClickListener {
            dismiss()
        }
    }

    private fun copyToClipboard(data: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Meet Link", data)
        clipboard.setPrimaryClip(clip)
        //Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun shareLink(link: String, passkey: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Join meet: $link\nPasskey: $passkey")
        }
        startActivity(Intent.createChooser(intent, "Share via"))
    }

    private fun joinMeeting(link: String) {
        val meetingFragment = MeetingFragment().apply {
            arguments = bundleOf("MEETING_LINK" to link)
        }

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, meetingFragment)
            .addToBackStack(null)
            .commit()
        dismiss()
    }
}
