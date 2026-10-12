package com.example.paircapture.util

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

enum class HapticKind { LIGHT, MEDIUM, SUCCESS }

/** Restrained haptics built on View feedback constants, which respect system settings. */
fun View.performGameHaptic(kind: HapticKind) {
    val constant = when (kind) {
        HapticKind.LIGHT -> HapticFeedbackConstants.CLOCK_TICK
        HapticKind.MEDIUM -> HapticFeedbackConstants.VIRTUAL_KEY
        HapticKind.SUCCESS ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM
            else HapticFeedbackConstants.LONG_PRESS
    }
    performHapticFeedback(constant)
}
