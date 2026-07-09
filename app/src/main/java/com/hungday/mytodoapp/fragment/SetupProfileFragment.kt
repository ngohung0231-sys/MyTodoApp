package com.hungday.mytodoapp.fragment

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.google.android.material.imageview.ShapeableImageView
import com.hungday.mytodoapp.R
import com.hungday.mytodoapp.receiver.NotificationReceiver
import com.hungday.mytodoapp.utils.HandleImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar

class SetupProfileFragment : Fragment(R.layout.fragment_setup_profile) {
    // Biến lưu trữ tạm thời
    private var currentUri: Uri? = null
    private var selectedBirthdate: LocalDate? = null

    // Permission & Media Launchers
    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let {
            lifecycleScope.launch(Dispatchers.IO) {
                val ctx = context ?: return@launch
                try {
                    val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    ctx.contentResolver.takePersistableUriPermission(it, takeFlags)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                val internalUri = HandleImage().copyUriToInternalStorage(ctx, it)
                withContext(Dispatchers.Main) {
                    if (internalUri != null && isAdded) {
                        currentUri = internalUri
                        imgAvatar.setImageURI(internalUri)
                    }
                }
            }
        }
    }

    // UI Components
    private lateinit var imgAvatar: ShapeableImageView
    private lateinit var etUserName: EditText
    private lateinit var btnContinue: Button
    private lateinit var rowSelectBirthdate: LinearLayout
    private lateinit var tvBirthdate: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupInitialState()
        setupListeners()
    }

    private fun initViews(view: View) {
        imgAvatar = view.findViewById(R.id.avatar)
        etUserName = view.findViewById(R.id.etUserName)
        btnContinue = view.findViewById(R.id.btnContinue)
        rowSelectBirthdate = view.findViewById(R.id.rowSelectBirthdate)
        tvBirthdate = view.findViewById(R.id.tvBirthdate)
    }

    private fun setupInitialState() {
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
            }
        })
    }

    private fun setupListeners() {
        // Chọn ngày sinh
        rowSelectBirthdate.setOnClickListener {
            showDatePicker()
        }

        // Chọn ảnh đại diện
        imgAvatar.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        // Nút Tiếp tục
        btnContinue.setOnClickListener {
            handleProfileSetup()
        }
    }

    //-------------------- Các hàm chức năng bổ trợ (Helper Functions) --------------------//

    private fun showDatePicker() {
        val ctx = context ?: return
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            ctx,
            R.style.CustomCalendarTheme,
            { _, selectedYear, selectedMonth, selectedDay ->
                selectedBirthdate = LocalDate.of(selectedYear, selectedMonth + 1, selectedDay)
                val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                tvBirthdate.text = selectedBirthdate?.format(formatter)
                tvBirthdate.setTextColor(ContextCompat.getColor(ctx, R.color.black))
            },
            year, month, day
        )

        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun handleProfileSetup() {
        val act = activity ?: return
        val ctx = context ?: return
        val name = etUserName.text.toString().trim()
        val birthdateText = tvBirthdate.text.toString()
        val defaultBirthdateText = ctx.getString(R.string.choose_your_birthday)

        if (name.isEmpty()) {
            etUserName.error = ctx.getString(R.string.enter_name_error)
            return
        }

        if (name.length > 11) {
            etUserName.error = ctx.getString(R.string.username_length_error)
        }
        
        if (birthdateText == defaultBirthdateText) {
            Toast.makeText(ctx, ctx.getString(R.string.choose_birthday_error), Toast.LENGTH_SHORT).show()
            return
        }

        // Save to SharedPreferences
        val sharedPref = act.getSharedPreferences("MyTodoPrefs", Context.MODE_PRIVATE)
        sharedPref.edit().apply {
            putString("USER_NAME", name)
            putString("USER_AVATAR", currentUri?.toString())
            putString("USER_BIRTHDAY", birthdateText)
            putBoolean("IS_PROFILE_SETUP", true)
            apply()
        }

        // Schedule birthday notification
        selectedBirthdate?.let {
            scheduleBirthdayNotification(ctx, it)
        }

        Log.d("datePicker", birthdateText)

        // Navigate to Home
        if (isAdded) {
            findNavController().navigate(
                R.id.action_setupProfile_to_homeFragment,
                null,
                navOptions {
                    popUpTo(R.id.onboardingFragment) {
                        inclusive = true
                    }
                }
            )
        }
    }

    private fun scheduleBirthdayNotification(context: Context, birthdate: LocalDate) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = NotificationReceiver.ACTION_BIRTHDAY
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Set alarm for the next occurrence of this birthday
        val now = LocalDate.now()
        var birthdayThisYear = birthdate.withYear(now.year)
        if (birthdayThisYear.isBefore(now) || birthdayThisYear.isEqual(now)) {
            birthdayThisYear = birthdayThisYear.plusYears(1)
        }

        val zdt = birthdayThisYear.atStartOfDay(ZoneId.systemDefault())
        val timeInMillis = zdt.toInstant().toEpochMilli()

        alarmManager.setExactAndAllowWhileIdle(
            android.app.AlarmManager.RTC_WAKEUP,
            timeInMillis,
            pendingIntent
        )
    }
}
