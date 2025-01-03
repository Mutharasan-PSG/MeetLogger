package com.example.MeetLogger

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.MeetLogger.databinding.FragmentCreateMeetBottomsheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentReference

class CreateMeetBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentCreateMeetBottomsheetBinding? = null
    private val binding get() = _binding!!

    private lateinit var uniqueMeetLink: String
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateMeetBottomsheetBinding.inflate(inflater, container, false)
        generateRealTimeMeetingLink() // Generate real-time link for the meeting
        setupListeners()
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Generates a real-time meet link using a Firestore backend or another WebRTC server
     */
    private fun generateRealTimeMeetingLink() {
        // Generate a unique meeting ID and create a Firestore document to store it
        val meetingId = generateUniqueMeetingId() // Replace with your real-time backend generation logic

        // For simplicity, let's assume meeting is being saved in Firestore for now
        val meetingDocRef: DocumentReference = firestore.collection("meetings").document(meetingId)

        meetingDocRef.set(mapOf("meetingId" to meetingId, "status" to "active")).addOnSuccessListener {
            uniqueMeetLink = "https://example.com/meet/$meetingId"  // This should be your WebRTC-specific link
            binding.meetLinkTextView.text = uniqueMeetLink  // Display real-time meeting link
        }.addOnFailureListener {
            Toast.makeText(context, "Failed to generate meet link", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Generates a unique meeting ID, could integrate with WebRTC for live session creation.
     */
    private fun generateUniqueMeetingId(): String {
        return "meet_${System.currentTimeMillis()}"
    }

    /**
     * Sets up listeners for all buttons and actions
     */
    private fun setupListeners() {
        // Copy link to clipboard
        binding.copyIcon.setOnClickListener {
            copyLinkToClipboard(uniqueMeetLink)
        }

        // Share the link
        binding.shareButton.setOnClickListener {
            shareLink(uniqueMeetLink)
        }

        // Join the meeting
        binding.joinButton.setOnClickListener {
            joinMeet(uniqueMeetLink)
        }

        // Dismiss the bottom sheet
        binding.dismissTextView.setOnClickListener {
            dismiss()
        }
    }

    /**
     * Copies the link to the clipboard
     */
    private fun copyLinkToClipboard(link: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Meet Link", link)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    /**
     * Shares the link using intent
     */
    private fun shareLink(link: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Join my meet: $link")
        }
        startActivity(Intent.createChooser(shareIntent, "Share meet link via"))
    }

    /**
     * Handles joining the meet
     */
    private fun joinMeet(link: String) {
        // Launch an activity for the audio conferencing feature
        // You should open a WebRTC view or an external meeting app here
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(link)
        }
        startActivity(intent)
    }
}
