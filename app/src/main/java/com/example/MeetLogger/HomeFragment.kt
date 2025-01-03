package com.example.MeetLogger

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.textview.MaterialTextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class HomeFragment : Fragment() {

    private lateinit var profilePic: ImageView
    private lateinit var bottomNavBar: BottomNavigationView
    private lateinit var meetButton: LinearLayout // The LinearLayout you click to open the options
    private lateinit var meetOptionsLayout: LinearLayout // The options popup menu
    private lateinit var closeButton: ImageView // Close button (X)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        profilePic = view.findViewById(R.id.profilePic)
        bottomNavBar = view.findViewById(R.id.bottomNavBar)
        meetButton = view.findViewById(R.id.meetButton) // Assuming there's a "Meet" button
        meetOptionsLayout = view.findViewById(R.id.meetOptionsLayout)
        closeButton = view.findViewById(R.id.closeButton)

        // Set Home as the default selected item
        bottomNavBar.selectedItemId = R.id.menu_home // This makes Home selected first and blue



        // Set onClickListener to show or hide the Meet Options popup when Meet button is clicked
        meetButton.setOnClickListener {
            // If the options are hidden, show it, else hide it
            if (meetOptionsLayout.visibility == View.GONE) {
                meetOptionsLayout.visibility = View.VISIBLE
                meetButton.visibility = View.GONE // Hide Meet button when options are visible
            } else {
                meetOptionsLayout.visibility = View.GONE
                meetButton.visibility = View.VISIBLE // Show Meet button when options are hidden
            }
        }

        closeButton.setOnClickListener {
            // Close the Meet options and show the Meet button again
            meetOptionsLayout.visibility = View.GONE
            meetButton.visibility = View.VISIBLE // Show Meet button when options are closed
        }

        // Handle the actions of the options buttons
        val joinMeetButton: MaterialTextView = view.findViewById(R.id.btnJoinMeet)
        val createMeetButton: MaterialTextView= view.findViewById(R.id.btnCreateMeet)
        val scheduleMeetButton: MaterialTextView = view.findViewById(R.id.btnScheduleMeet)

        joinMeetButton.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, JoinMeetFragment())
                .addToBackStack(null) // Allows users to press back to return
                .commit()
        }

        createMeetButton.setOnClickListener {
            val createMeetBottomSheet = CreateMeetBottomSheetFragment()
            createMeetBottomSheet.show(parentFragmentManager, "CreateMeetBottomSheet")
        }


        scheduleMeetButton.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ScheduleMeetFragment())
                .addToBackStack(null)
                .commit()
        }

        // Setup BottomNavigationView to manage fragment switching
        setupBottomNavigation()

        loadUserProfile() // Load the profile data for the user
        // Set up OnClickListener to navigate to ProfileFragment when the profile image is clicked
        profilePic.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ProfileFragment())
                .addToBackStack(null) // Add this transaction to the back stack so the user can press back
                .commit()
        }

        return view
    }



    private fun setupBottomNavigation() {
        bottomNavBar.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_home -> {
                    // No need to switch fragments, HomeFragment is already here
                    true
                }
                R.id.menu_profile -> {
                    // Switch to ProfileFragment
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, ProfileFragment())
                        .addToBackStack(null)
                        .commit()
                    true
                }
                R.id.menu_report -> {
                    // Switch to ReportFragment
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, ReportFragment())
                        .addToBackStack(null)
                        .commit()
                    true
                }
                else -> false
            }
        }
    }

    private fun loadUserProfile() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("Users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                val profileUrl = document.getString("photoUrl") ?: return@addOnSuccessListener
                Glide.with(this)
                    .load(profileUrl)
                    .circleCrop()
                    .placeholder(R.drawable.default_profile_pic)
                    .into(profilePic)
            }
            .addOnFailureListener {
                // Handle failure to load profile
            }
    }
}
