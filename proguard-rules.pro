# EMFAD® Android App ProGuard Rules
# Optimized for Samsung S21 Ultra Production Build

# Basic ProGuard Configuration
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# Keep line numbers for debugging
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep annotations
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# EMFAD Core Classes - Keep all EMFAD-specific classes
-keep class com.emfad.app.** { *; }
-keepclassmembers class com.emfad.app.** { *; }

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.emfad.app.**$$serializer { *; }
-keepclassmembers class com.emfad.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.emfad.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Hilt Dependency Injection
-dontwarn com.google.dagger.hilt.processor.internal.**
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# Room Database
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-dontwarn androidx.room.paging.**

# Jetpack Compose
-keep class androidx.compose.** { *; }
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.material3.** { *; }
-dontwarn androidx.compose.**

# Navigation Component
-keep class androidx.navigation.** { *; }
-keepclassmembers class * extends androidx.navigation.Navigator { *; }

# Coroutines
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-keep class kotlinx.coroutines.** { *; }

# USB Serial Communication
-keep class com.hoho.android.usbserial.** { *; }
-keepclassmembers class com.hoho.android.usbserial.** { *; }

# Nordic BLE Library
-keep class no.nordicsemi.android.ble.** { *; }
-keepclassmembers class no.nordicsemi.android.ble.** { *; }

# OSMDroid Maps
-keep class org.osmdroid.** { *; }
-keepclassmembers class org.osmdroid.** { *; }

# Google Play Services
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Timber Logging
-keep class timber.log.** { *; }
-dontwarn timber.log.**

# Remove Logging in Release
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

-assumenosideeffects class timber.log.Timber {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Samsung S21 Ultra specific optimizations
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify

# ================================
# EMFAD SPECIFIC RULES
# ================================

# Keep EMFAD Application class
-keep class com.emfad.app.EMFADApplication { *; }

# Keep all model classes (data classes)
-keep class com.emfad.app.models.** { *; }
-keep class com.emfad.app.database.entities.** { *; }

# Keep all enum classes
-keepclassmembers enum com.emfad.app.models.enums.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Bluetooth related classes
-keep class com.emfad.app.bluetooth.** { *; }
-keep class com.emfad.app.services.bluetooth.** { *; }

# Keep AI/ML related classes
-keep class com.emfad.app.ai.** { *; }
-keep class com.emfad.app.services.ai.** { *; }

# Keep AR related classes
-keep class com.emfad.app.ar.** { *; }

# Keep measurement service classes
-keep class com.emfad.app.services.measurement.** { *; }

# Keep export service classes
-keep class com.emfad.app.services.export.** { *; }

# Keep ViewModels
-keep class com.emfad.app.viewmodels.** { *; }

# Keep UI components that might be referenced by name
-keep class com.emfad.app.ui.** { *; }

# ================================
# ANDROID FRAMEWORK RULES
# ================================

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep custom views
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
    *** get*();
}

# Keep Activity classes
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep Serializable classes
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ================================
# JETPACK COMPOSE RULES
# ================================

# Keep Compose runtime
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.foundation.** { *; }
-keep class androidx.compose.material3.** { *; }
-keep class androidx.compose.animation.** { *; }

# Keep Compose compiler generated classes
-keep class androidx.compose.compiler.** { *; }

# Keep Composable functions
-keep @androidx.compose.runtime.Composable class * { *; }
-keep class * {
    @androidx.compose.runtime.Composable <methods>;
}

# ================================
# HILT/DAGGER RULES
# ================================

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ApplicationComponentManager { *; }
-keep class **_HiltModules { *; }
-keep class **_HiltModules$* { *; }

# Keep Dagger generated classes
-keep class **_Factory { *; }
-keep class **_MembersInjector { *; }
-keep class **_Provide*Factory { *; }

# Keep @Inject constructors
-keepclasseswithmembers class * {
    @javax.inject.Inject <init>(...);
}

# Keep @Inject fields
-keepclasseswithmembers class * {
    @javax.inject.Inject <fields>;
}

# Keep @Inject methods
-keepclasseswithmembers class * {
    @javax.inject.Inject <methods>;
}

# ================================
# ROOM DATABASE RULES
# ================================

# Keep Room generated classes
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# Keep Room annotations
-keep class androidx.room.** { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# ================================
# KOTLIN RULES
# ================================

# Keep Kotlin metadata
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Keep Kotlin coroutines
-keep class kotlinx.coroutines.** { *; }
-keep class kotlin.coroutines.** { *; }

# Keep Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep @Serializable classes
-keep @kotlinx.serialization.Serializable class * { *; }

# ================================
# BLUETOOTH RULES
# ================================

# Keep Nordic BLE library
-keep class no.nordicsemi.android.ble.** { *; }
-keep class no.nordicsemi.android.ble.ktx.** { *; }

# Keep Bluetooth classes
-keep class android.bluetooth.** { *; }

# ================================
# ARCORE RULES
# ================================

# Keep ARCore classes
-keep class com.google.ar.core.** { *; }
-keep class com.google.ar.sceneform.** { *; }

# Keep OpenGL classes
-keep class android.opengl.** { *; }
-keep class javax.microedition.khronos.** { *; }

# ================================
# TENSORFLOW LITE RULES
# ================================

# Keep TensorFlow Lite classes
-keep class org.tensorflow.lite.** { *; }
-keep class org.tensorflow.lite.support.** { *; }

# Keep native TensorFlow methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# ================================
# NETWORKING RULES
# ================================

# Keep Retrofit classes
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# Keep Gson classes
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# ================================
# CHART LIBRARY RULES
# ================================

# Keep MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }

# Keep Vico charts
-keep class com.patrykandpatrick.vico.** { *; }

# ================================
# SECURITY RULES
# ================================

# Keep security related classes
-keep class androidx.security.crypto.** { *; }
-keep class androidx.biometric.** { *; }

# ================================
# OPTIMIZATION SETTINGS
# ================================

# Optimization settings
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# Remove Timber logging in release builds
-assumenosideeffects class timber.log.Timber {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# ================================
# SAMSUNG S21 ULTRA OPTIMIZATIONS
# ================================

# Keep Vulkan API classes
-keep class android.hardware.vulkan.** { *; }

# Keep Samsung specific classes (if any)
-keep class com.samsung.** { *; }

# Keep Snapdragon optimizations
-keep class com.qualcomm.** { *; }

# ================================
# DEBUGGING RULES (REMOVE IN PRODUCTION)
# ================================

# Uncomment for debugging obfuscation issues
# -printmapping mapping.txt
# -printseeds seeds.txt
# -printusage usage.txt

# Keep line numbers for crash reports
-keepattributes SourceFile,LineNumberTable

# Rename source file attribute to hide original file names
-renamesourcefileattribute SourceFile
