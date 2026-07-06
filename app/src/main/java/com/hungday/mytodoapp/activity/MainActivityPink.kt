package com.hungday.mytodoapp.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivityPink : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Chuyển tiếp ngay sang MainActivity gốc
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        // Đóng cổng vào ngay lập tức
        finish()
    }
}