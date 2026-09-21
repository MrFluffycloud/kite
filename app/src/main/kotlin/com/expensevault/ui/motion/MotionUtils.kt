package com.expensevault.ui.motion

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

const val PREFS_SETTINGS = "expense_vault_settings"
const val PREF_REDUCE_MOTION = "pref_reduce_motion"

fun isReduceMotionActive(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE)
    if (prefs.contains(PREF_REDUCE_MOTION)) {
        return prefs.getBoolean(PREF_REDUCE_MOTION, false)
    }
    val resolver = context.contentResolver
    val durationScale = Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    val transitionScale = Settings.Global.getFloat(resolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 1f)
    return durationScale == 0f || transitionScale == 0f
}

@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    var isReduceMotion by remember { mutableStateOf(isReduceMotionActive(context)) }
    DisposableEffect(context) {
        val prefs = context.getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == PREF_REDUCE_MOTION) {
                isReduceMotion = isReduceMotionActive(context)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
    return isReduceMotion
}
