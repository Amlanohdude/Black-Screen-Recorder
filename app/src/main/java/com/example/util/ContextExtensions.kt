package com.example.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 * Traverses ContextWrapper hierarchy to find the enclosing Activity.
 * Resolves cases where LocalContext.current is wrapped in ContextThemeWrapper
 * or navigation/compose wrappers, preventing null casts to Activity.
 */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
