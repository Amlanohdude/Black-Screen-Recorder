# Proguard & R8 rules for Black Screen Recorder Pro

# -------------------------------------------------------------
# Crash Reporting & Stacktrace Deobfuscation
# -------------------------------------------------------------
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# -------------------------------------------------------------
# Store Billing SDKs (Play, Amazon, Xiaomi, Razorpay)
# -------------------------------------------------------------
-keep class com.android.billingclient.api.** { *; }
-dontwarn com.android.billingclient.**
-keep class com.amazon.device.iap.** { *; }
-dontwarn com.amazon.device.**
-keep class com.xiaomi.billingclient.api.** { *; }
-dontwarn com.xiaomi.billingclient.**
-keep class com.razorpay.** { *; }
-dontwarn com.razorpay.**

# -------------------------------------------------------------
# Google Mobile Ads (AdMob) & User Messaging Platform (UMP)
# -------------------------------------------------------------
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.android.ump.** { *; }
-dontwarn com.google.android.gms.ads.**
-dontwarn com.google.android.ump.**

# -------------------------------------------------------------
# Room Database & Storage Entities
# -------------------------------------------------------------
-keep class com.example.storage.** { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-dontwarn androidx.room.**

# -------------------------------------------------------------
# CameraX Video Recording & Surface Rendering
# -------------------------------------------------------------
-keep class androidx.camera.video.** { *; }
-keep class androidx.camera.core.** { *; }
-keep class androidx.camera.camera2.** { *; }
-keep class androidx.camera.lifecycle.** { *; }
-keep class androidx.camera.view.** { *; }

# -------------------------------------------------------------
# Core App Logic & State Containers
# -------------------------------------------------------------
-keep class com.example.recording.** { *; }
-keep class com.example.billing.** { *; }
-keep class com.example.covers.** { *; }
-keep class com.example.ads.** { *; }
-keep class com.example.ui.MainViewModel { *; }

# -------------------------------------------------------------
# Kotlin Coroutines
# -------------------------------------------------------------
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**
