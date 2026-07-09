package com.hungday.mytodoapp.fragment

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import android.view.View
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
import com.hungday.mytodoapp.database.TodoDatabase
import com.hungday.mytodoapp.utils.HandleImage
import com.hungday.mytodoapp.activity.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

class SettingFragment : Fragment(R.layout.fragment_setting) {
    // Biến lưu trữ tạm thời
    private var currentUri: Uri? = null
    private var selectedBirthdate: LocalDate? = null
    private var isUpdatingTheme = false

    // Pick Avatar Launcher
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
                        avatar.setImageURI(internalUri)

                        // Save the new avatar URI to SharedPreferences
                        val sharedPref = activity?.getSharedPreferences("MyTodoPrefs", Context.MODE_PRIVATE)
                        sharedPref?.edit { putString("USER_AVATAR", internalUri.toString()) }
                    }
                }
            }
        }
    }

    // UI Components
    private lateinit var btnBack: ImageView
    private lateinit var avatar: ShapeableImageView
    private lateinit var tvUserName: TextView
    private lateinit var switchThemeColor: SwitchCompat
    private lateinit var tvThemeColor: TextView
    private lateinit var tvLanguage: TextView
    private lateinit var tvBirthDate: TextView
    private lateinit var lnlUserName: LinearLayout
    private lateinit var lnlLanguage: LinearLayout
    private lateinit var lnlBirthDay: LinearLayout
    private lateinit var lnlThemeColor: LinearLayout
    private lateinit var lnlTrashBin: LinearLayout
    private lateinit var lnlDeleteAllData: LinearLayout

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupInitialState()
        setupListeners()
    }

    private fun initViews(view: View) {
        btnBack = view.findViewById(R.id.btnBack)
        avatar = view.findViewById(R.id.avatar)
        tvUserName = view.findViewById(R.id.tvUserName)
        switchThemeColor = view.findViewById(R.id.switchThemeColor)
        tvThemeColor = view.findViewById(R.id.tvThemeColor)
        tvLanguage = view.findViewById(R.id.tvLanguage)
        tvBirthDate = view.findViewById(R.id.tvBirthdate)
        lnlUserName = view.findViewById(R.id.lnlUsername)
        lnlLanguage = view.findViewById(R.id.lnlLanguage)
        lnlBirthDay = view.findViewById(R.id.lnlBirthday)
        lnlThemeColor = view.findViewById(R.id.lnlThemeColor)
        lnlTrashBin = view.findViewById(R.id.lnlTrashBin)
        lnlDeleteAllData = view.findViewById(R.id.lnlDeleteData)
    }

    private fun setupInitialState() {
        val act = activity ?: return
        val ctx = context ?: return
        val sharedPref = act.getSharedPreferences("MyTodoPrefs", Context.MODE_PRIVATE)
        val name = sharedPref.getString("USER_NAME", "User Name")
        val avatarUriString = sharedPref.getString("USER_AVATAR", null)
        val birthday = sharedPref.getString("USER_BIRTHDAY", "Not set")

        // Read language from AppCompatDelegate
        val appLocales = AppCompatDelegate.getApplicationLocales()
        val languageCode = if (appLocales.isEmpty) {
            // If empty, it means system default. Check if it's Vietnamese.
            if (java.util.Locale.getDefault().language == "vi") "vi" else "en"
        } else {
            appLocales.get(0)?.language ?: "en"
        }

        tvUserName.text = name
        tvBirthDate.text = birthday
        avatarUriString?.let {
            currentUri = it.toUri()
            avatar.setImageURI(currentUri)
        }

        val isPinkTheme = sharedPref.getBoolean("IS_PINK_THEME", false)
        switchThemeColor.isChecked = isPinkTheme
        tvThemeColor.text = if (isPinkTheme) ctx.getString(R.string.pink) else ctx.getString(R.string.blue)
        
        tvLanguage.text = if (languageCode == "vi") "Tiếng Việt" else "English"
    }

    private fun setupListeners() {
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

        // Đổi ngôn ngữ
        lnlLanguage.setOnClickListener {
            showLanguageDialog()
        }

        // Đổi màu chủ đạo
        lnlThemeColor.setOnClickListener {
            switchThemeColor.isChecked = !switchThemeColor.isChecked
        }

        switchThemeColor.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingTheme) return@setOnCheckedChangeListener
            showChangeThemeDialog(isChecked)
        }

        // Thay đổi ngày sinh
        lnlBirthDay.setOnClickListener {
            showDatePicker()
        }

        // Xóa dữ liệu
        lnlDeleteAllData.setOnClickListener {
            showDeleteConfirmDialog()
        }

        // Thùng rác
        lnlTrashBin.setOnClickListener {
            findNavController().navigate(R.id.trashBinFragment)
        }
    }

    private fun updateLauncherIcon(isPink: Boolean) {
        val context = requireContext()
        val pm = context.packageManager
        
        val blueAlias = "com.hungday.mytodoapp.activity.MainActivityBlue"
        val pinkAlias = "com.hungday.mytodoapp.activity.MainActivityPink"
        
        val blueComponent = android.content.ComponentName(context, blueAlias)
        val pinkComponent = android.content.ComponentName(context, pinkAlias)
        
        if (isPink) {
            pm.setComponentEnabledSetting(pinkComponent, android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED, android.content.pm.PackageManager.DONT_KILL_APP)
            pm.setComponentEnabledSetting(blueComponent, android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED, android.content.pm.PackageManager.DONT_KILL_APP)
        } else {
            pm.setComponentEnabledSetting(blueComponent, android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED, android.content.pm.PackageManager.DONT_KILL_APP)
            pm.setComponentEnabledSetting(pinkComponent, android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED, android.content.pm.PackageManager.DONT_KILL_APP)
        }
    }

    //-------------------- Các hàm chức năng bổ trợ (Helper Functions) --------------------//

    private fun showLanguageDialog() {
        val ctx = context ?: return
        val dialogView = LayoutInflater.from(ctx).inflate(R.layout.dialog_select_language, null)
        val alertDialog = AlertDialog.Builder(ctx)
            .setView(dialogView)
            .create()
        
        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        alertDialog.show()

        val btnEnglish = dialogView.findViewById<LinearLayout>(R.id.btnEnglish)
        val btnVietnamese = dialogView.findViewById<LinearLayout>(R.id.btnVietnamese)
        val btnCancel = dialogView.findViewById<TextView>(R.id.btnCancelLanguage)

        btnEnglish.setOnClickListener {
            setLocale("en")
            alertDialog.dismiss()
        }

        btnVietnamese.setOnClickListener {
            setLocale("vi")
            alertDialog.dismiss()
        }

        btnCancel.setOnClickListener { alertDialog.dismiss() }
    }

    private fun setLocale(languageCode: String) {
        val ctx = context ?: return
        val act = activity ?: return
        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(appLocale)
        
        // Khởi động lại Activity và xóa stack để quay về Home
        val intent = Intent(ctx, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        act.finish()
    }

    private fun showEditUsernameDialog() {
        val act = activity ?: return
        val ctx = context ?: return
        val sharedPref = act.getSharedPreferences("MyTodoPrefs", Context.MODE_PRIVATE)
        val dialogView = LayoutInflater.from(ctx).inflate(R.layout.dialog_edit_username, null)
        val alertDialog = AlertDialog.Builder(ctx)
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
                Toast.makeText(ctx, "Please enter your name!", Toast.LENGTH_SHORT).show()
            } else if(etNewUsername.length() > 11) {
                etNewUsername.error = "Username must be 11 characters or less!"
            } else {
                sharedPref.edit { putString("USER_NAME", newName) }
                tvUserName.text = newName
                Toast.makeText(ctx, "Updated username! 🚀", Toast.LENGTH_SHORT).show()
                alertDialog.dismiss()
            }
        }
    }

    private fun showChangeThemeDialog(isChecked: Boolean) {
        val act = activity ?: return
        val ctx = context ?: return
        val sharedPref = act.getSharedPreferences("MyTodoPrefs", Context.MODE_PRIVATE)
        val wasChecked = sharedPref.getBoolean("IS_PINK_THEME", false)
        val dialogView = LayoutInflater.from(ctx).inflate(R.layout.dialog_confirm_change_theme, null)
        val alertDialog = AlertDialog.Builder(ctx)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        alertDialog.show()
        val btnCancel = dialogView.findViewById<TextView>(R.id.btnCancelDialog)
        val btnOk = dialogView.findViewById<TextView>(R.id.btnChangeDialog)

        btnCancel.setOnClickListener {
            isUpdatingTheme = true
            switchThemeColor.isChecked = !isChecked
            isUpdatingTheme = false
            alertDialog.dismiss()
        }

        btnOk.setOnClickListener {
            if (isChecked != wasChecked) {
                sharedPref.edit { putBoolean("IS_PINK_THEME", isChecked) }
                tvThemeColor.text = if (isChecked) ctx.getString(R.string.pink) else ctx.getString(R.string.blue)

                // Cập nhật launcher icon
                updateLauncherIcon(isChecked)

                // Khởi động lại Activity và xóa stack để quay về Home và áp dụng theme mới
                val intent = Intent(ctx, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                act.finish()
            }
            alertDialog.dismiss()
        }
    }

    private fun showDatePicker() {
        val act = activity ?: return
        val ctx = context ?: return
        val sharedPref = act.getSharedPreferences("MyTodoPrefs", Context.MODE_PRIVATE)
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            ctx,
            R.style.CustomCalendarTheme,
            { _, selectedYear, selectedMonth, selectedDay ->
                selectedBirthdate = LocalDate.of(selectedYear, selectedMonth + 1, selectedDay)
                val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                val dateString = selectedBirthdate?.format(formatter) ?: ""
                tvBirthDate.text = dateString
                sharedPref.edit { putString("USER_BIRTHDAY", dateString) }
                Toast.makeText(ctx, "Updated birthday! 🚀", Toast.LENGTH_SHORT).show()
            },
            calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
        )

        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    private fun showDeleteConfirmDialog() {
        val ctx = context ?: return
        val dialogView = LayoutInflater.from(ctx).inflate(R.layout.dialog_confirm_delete_data, null)
        val alertDialog = AlertDialog.Builder(ctx)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        alertDialog.show()

        val btnCancel = dialogView.findViewById<TextView>(R.id.btnCancelDelete)
        val btnConfirm = dialogView.findViewById<TextView>(R.id.btnConfirmDelete)

        btnCancel.setOnClickListener { alertDialog.dismiss() }

        btnConfirm.setOnClickListener {
            alertDialog.dismiss()
            resetAppData()
        }
    }

    private fun resetAppData() {
        val appContext = context?.applicationContext ?: return
        val activity = activity ?: return
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. Xóa Database
                val database = TodoDatabase.getDatabase(appContext)
                database.clearAllTables()
                
                // 1.1 Khởi tạo lại dữ liệu mặc định ngay lập tức
                TodoDatabase.initializeData(appContext)

                // 2. Xóa SharedPreferences
                val sharedPref = appContext.getSharedPreferences("MyTodoPrefs", Context.MODE_PRIVATE)
                sharedPref.edit(commit = true) { clear() } // Dùng commit để đảm bảo xóa xong ngay lập tức

                // 3. Xóa cache và files (optional nhưng tốt cho việc reset hoàn toàn)
                appContext.cacheDir.deleteRecursively()
                appContext.filesDir.deleteRecursively()

                withContext(Dispatchers.Main) {
                    Toast.makeText(appContext, "Data cleared! Restarting...", Toast.LENGTH_SHORT).show()
                    
                    // 4. Khởi động lại ứng dụng
                    val intent = appContext.packageManager.getLaunchIntentForPackage(appContext.packageName)
                    intent?.let {
                        it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(it)
                    }
                    activity.finishAffinity()
                    
                    // Kết thúc process để đảm bảo mọi singleton/static variable được reset
                    android.os.Process.killProcess(android.os.Process.myPid())
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(appContext, "Failed to clear data completely.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
