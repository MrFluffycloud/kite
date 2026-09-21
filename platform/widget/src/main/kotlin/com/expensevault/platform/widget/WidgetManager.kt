package com.expensevault.platform.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object WidgetManager {
    fun updateAllWidgets(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ExpenseGlanceWidget().updateAll(context)
            } catch (e: Exception) {
                // Ignore if no widgets active or context dead
            }
        }
    }
}
