package com.hungday.mytodoapp.utils

import android.graphics.Color
import androidx.core.graphics.ColorUtils

object ColorHelper {
    fun lightenColor(colorStr: String): Int {
        val baseColor = Color.parseColor(colorStr)
        return ColorUtils.blendARGB(baseColor, Color.WHITE, 0.85f)
    }
}