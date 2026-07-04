package com.hungday.mytodoapp.utils

import android.graphics.Color
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.toColorInt

object ColorHelper {
    /**
     * Lighten a color by blending it with White.
     * @param color The color as an Int.
     * @return The lightened color as an Int.
     */
    fun lightenColor(color: Int): Int {
        return ColorUtils.blendARGB(color, Color.WHITE, 0.85f)
    }

    /**
     * Lighten a color string (e.g., "#RRGGBB").
     * @param colorStr The color as a hex string.
     * @return The lightened color as an Int.
     */
    fun lightenColor(colorStr: String): Int {
        return lightenColor(colorStr.toColorInt())
    }
}