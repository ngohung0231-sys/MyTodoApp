package com.hungday.mytodoapp.fragment // Giữ nguyên package của bro

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.imageview.ShapeableImageView
import com.hungday.mytodoapp.R

class SetupProfileFragment : Fragment(R.layout.fragment_setup_profile) {
    private var currentUri: android.net.Uri? = null
    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            currentUri = uri
            val imageView = view?.findViewById<ShapeableImageView>(R.id.avatar)
            imageView?.setImageURI(uri)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val imageView = view.findViewById<ShapeableImageView>(R.id.avatar)
        val etUserName = view.findViewById<EditText>(R.id.etUserName)
        val btnContinue = view.findViewById<Button>(R.id.btnContinue)

        imageView.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        btnContinue.setOnClickListener {
            val name = etUserName.text.toString().trim()

            if (name.isEmpty()) {
                etUserName.error = "Vui lòng nhập tên của bạn nhé!"
            } else {
                val sharedPref = requireActivity().getSharedPreferences("MyTodoPrefs", android.content.Context.MODE_PRIVATE)
                sharedPref.edit().apply(){
                    putString("user_name", name)
                    putString("user_avatar", currentUri?.toString())
                    apply()
                }
                findNavController().navigate(R.id.action_setupProfile_to_homeFragment)
            }
        }
    }
}