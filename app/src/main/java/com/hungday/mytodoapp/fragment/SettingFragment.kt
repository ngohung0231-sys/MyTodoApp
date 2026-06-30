package com.hungday.mytodoapp.fragment

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.transition.ChangeBounds
import android.transition.TransitionManager
import android.transition.TransitionSet
import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.imageview.ShapeableImageView
import com.hungday.mytodoapp.R
import com.hungday.mytodoapp.utils.HandleImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar

class SettingFragment : Fragment(R.layout.fragment_setting) {
    // Biến lưu trữ tạm thời
    private var currentUri: Uri? = null
    private var selectedBirthdate: LocalDate? = null

    // Pick Avatar Launcher
    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    requireContext().contentResolver.takePersistableUriPermission(it, takeFlags)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                val internalUri = HandleImage().copyUriToInternalStorage(requireContext(), it)
                withContext(Dispatchers.Main) {
                    if (internalUri != null) {
                        currentUri = internalUri
                        avatar.setImageURI(internalUri)

                        // Save the new avatar URI to SharedPreferences
                        val sharedPref = requireActivity().getSharedPreferences("MyTodoPrefs", Context.MODE_PRIVATE)
                        sharedPref.edit().putString("USER_AVATAR", internalUri.toString()).apply()
                    }
                }
            }
        }
    }

    // UI Components
    private lateinit var btnBack: ImageView
    private lateinit var avatar: ShapeableImageView
    private lateinit var tvUserName: TextView
    private lateinit var switchDarkMode: SwitchCompat
    private lateinit var tvDarkMode: TextView
    private lateinit var tvLanguage: TextView
    private lateinit var tvBirthDate: TextView
    private lateinit var lnlUserName: LinearLayout
    private lateinit var lnlDarkMode: LinearLayout
    private lateinit var lnlLanguage: LinearLayout
    private lateinit var lnlBirthDay: LinearLayout

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //------------------------------------ Init Views ------------------------------------//
        btnBack = view.findViewById(R.id.btnBack)
        avatar = view.findViewById(R.id.avatar)
        tvUserName = view.findViewById(R.id.tvUserName)
        switchDarkMode = view.findViewById(R.id.switchDarkMode)
        tvDarkMode = view.findViewById(R.id.tvDarkMode)
        tvLanguage = view.findViewById(R.id.tvLanguage)
        tvBirthDate = view.findViewById(R.id.tvBirthdate)
        lnlUserName = view.findViewById(R.id.lnlUsername)
        lnlDarkMode = view.findViewById(R.id.lnlDarkMode)
        lnlLanguage = view.findViewById(R.id.lnlLanguage)
        lnlBirthDay = view.findViewById(R.id.lnlBirthday)
        //------------------------------------------------------------------------------------//

        //------------------------------------ Initial State Setup ------------------------------------//
        val sharedPref = requireActivity().getSharedPreferences("MyTodoPrefs", Context.MODE_PRIVATE)
        val name = sharedPref.getString("USER_NAME", "User Name")
        val avatarUriString = sharedPref.getString("USER_AVATAR", null)
        val birthday = sharedPref.getString("USER_BIRTHDAY", "Not set")

        tvUserName.text = name
        tvBirthDate.text = birthday
        avatarUriString?.let {
            currentUri = Uri.parse(it)
            avatar.setImageURI(currentUri)
        }
        //---------------------------------------------------------------------------------------------//

        //------------------------------------ Setup Listeners ------------------------------------//
        
        // Quay lại
        btnBack.setOnClickListener { findNavController().navigateUp() }

        // Thay ảnh đại diện
        avatar.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        // Đổi tên người dùng
        lnlUserName.setOnClickListener {
            showEditUsernameDialog()
        }

        // Bật/tắt Dark Mode
        lnlDarkMode.setOnClickListener {
            switchDarkMode.isChecked = !switchDarkMode.isChecked
        }

        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            TransitionManager.beginDelayedTransition(view as ViewGroup, TransitionSet().addTransition(ChangeBounds()).setDuration(300))
            tvDarkMode.text = if (isChecked) "Dark" else "Light"
            // TODO: Apply Theme changes
        }

        // Thay đổi ngày sinh
        lnlBirthDay.setOnClickListener {
            showDatePicker()
        }
        //-----------------------------------------------------------------------------------------//
    }

    //-------------------- Các hàm chức năng bổ trợ (Helper Functions) --------------------//

    private fun showEditUsernameDialog() {
        val sharedPref = requireActivity().getSharedPreferences("MyTodoPrefs", Context.MODE_PRIVATE)
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_username, null)
        val alertDialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()
        
        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        alertDialog.show()

        val etNewUsername = dialogView.findViewById<EditText>(R.id.etNewUsername)
        val btnCancel = dialogView.findViewById<TextView>(R.id.btnCancelDialog)
        val btnSave = dialogView.findViewById<TextView>(R.id.btnSaveDialog)

        etNewUsername.setText(sharedPref.getString("USER_NAME", ""))
        etNewUsername.setSelection(etNewUsername.text.length)

        btnCancel.setOnClickListener { alertDialog.dismiss() }

        btnSave.setOnClickListener {
            val newName = etNewUsername.text.toString().trim()
            if (newName.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter your name!", Toast.LENGTH_SHORT).show()
            } else if(etNewUsername.length() > 11) {
                etNewUsername.error = "Username must be 11 characters or less!"
            } else {
                sharedPref.edit().putString("USER_NAME", newName).apply()
                tvUserName.text = newName
                Toast.makeText(requireContext(), "Updated username! 🚀", Toast.LENGTH_SHORT).show()
                alertDialog.dismiss()
            }
        }
    }

    private fun showDatePicker() {
        val sharedPref = requireActivity().getSharedPreferences("MyTodoPrefs", Context.MODE_PRIVATE)
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            R.style.CustomCalendarTheme,
            { _, selectedYear, selectedMonth, selectedDay ->
                selectedBirthdate = LocalDate.of(selectedYear, selectedMonth + 1, selectedDay)
                val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                val dateString = selectedBirthdate?.format(formatter) ?: ""
                tvBirthDate.text = dateString
                sharedPref.edit().putString("USER_BIRTHDAY", dateString).apply()
            },
            calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
        )

        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        datePickerDialog.show()
        Toast.makeText(requireContext(), "Updated birthday! 🚀", Toast.LENGTH_SHORT).show()
    }
}
