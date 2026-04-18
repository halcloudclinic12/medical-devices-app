# ==============================================================================
# HAL Healthbox — ProGuard Rules
# ==============================================================================

# ------------------------------------------------------------------------------
# General: preserve source info for crash stack traces
# ------------------------------------------------------------------------------
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleParameterAnnotations

# ------------------------------------------------------------------------------
# Kotlin
# ------------------------------------------------------------------------------
-keep class kotlin.Metadata { *; }
-keep class kotlin.reflect.** { *; }
-keep class kotlin.reflect.jvm.internal.** { *; }
-keepclassmembers class ** {
    @kotlin.Metadata *;
}
-dontwarn kotlin.**
-dontwarn kotlin.reflect.**

# ------------------------------------------------------------------------------
# Kotlin Coroutines + Flow
# ------------------------------------------------------------------------------
-keep class kotlinx.coroutines.** { *; }
-keep class kotlinx.coroutines.flow.** { *; }
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler { *; }
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-keep class kotlin.coroutines.Continuation { *; }
-keep interface kotlin.coroutines.** { *; }
-keepclassmembers class * {
    kotlin.coroutines.Continuation *;
    kotlinx.coroutines.flow.StateFlow *;
    kotlinx.coroutines.flow.MutableStateFlow *;
    kotlinx.coroutines.flow.Flow *;
}
-dontwarn kotlinx.coroutines.**

# ------------------------------------------------------------------------------
# Hilt / Dagger
# ------------------------------------------------------------------------------
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class dagger.** { *; }
-keep interface dagger.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keepclassmembers @dagger.hilt.android.lifecycle.HiltViewModel class * {
    <init>(...);
}
-keepclassmembers,allowobfuscation class * {
    @javax.inject.* <fields>;
    @javax.inject.* <methods>;
    @dagger.* <fields>;
    @dagger.* <methods>;
}
-dontwarn dagger.hilt.**

# ------------------------------------------------------------------------------
# Retrofit 2
# ------------------------------------------------------------------------------
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-keep,allowobfuscation,allowshrinking class retrofit2.Response { *; }
-keep,allowobfuscation,allowshrinking class retrofit2.Call { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keepclassmembernames interface * {
    @retrofit2.http.* <methods>;
}
-if interface * { @retrofit2.http.* public *** *(...); }
-keep,allowobfuscation interface <1> { @retrofit2.http.* public *** *(...); }
-dontwarn retrofit2.**
-dontwarn org.codehaus.mojo.**

# ------------------------------------------------------------------------------
# OkHttp 3 / 4
# ------------------------------------------------------------------------------
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ------------------------------------------------------------------------------
# Gson
# ------------------------------------------------------------------------------
-keep class com.google.gson.** { *; }
-keep class com.google.gson.internal.** { *; }
-keep class com.google.gson.stream.** { *; }
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken { *; }
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken { *; }
-keep,allowobfuscation class * extends com.google.gson.TypeAdapter { *; }
-keep,allowobfuscation class * extends com.google.gson.TypeAdapterFactory { *; }
-keep,allowobfuscation class * implements com.google.gson.JsonSerializer { *; }
-keep,allowobfuscation class * implements com.google.gson.JsonDeserializer { *; }
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
    @com.google.gson.annotations.* <fields>;
    @com.google.gson.annotations.* <methods>;
}

# ------------------------------------------------------------------------------
# Moshi
# ------------------------------------------------------------------------------
-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.* <methods>;
    @com.squareup.moshi.* <fields>;
}
-keepclassmembers,allowobfuscation interface * {
    @com.squareup.moshi.* <methods>;
}
-dontwarn com.squareup.moshi.**

# ------------------------------------------------------------------------------
# Room
# ------------------------------------------------------------------------------
-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Database class * { *; }
-dontwarn androidx.room.**

# ------------------------------------------------------------------------------
# Firebase (Crashlytics + Analytics)
# ------------------------------------------------------------------------------
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# ------------------------------------------------------------------------------
# Glide
# ------------------------------------------------------------------------------
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { *; }
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}
-keep class com.bumptech.glide.** { *; }
-dontwarn com.bumptech.glide.**

# ------------------------------------------------------------------------------
# Picasso
# ------------------------------------------------------------------------------
-keep class com.squareup.picasso.** { *; }
-dontwarn com.squareup.picasso.**
-dontwarn com.squareup.okhttp.**

# ------------------------------------------------------------------------------
# Lottie
# ------------------------------------------------------------------------------
-keep class com.airbnb.lottie.** { *; }
-dontwarn com.airbnb.lottie.**

# ------------------------------------------------------------------------------
# Chucker (debug-only; release uses no-op artifact)
# ------------------------------------------------------------------------------
-dontwarn com.chuckerteam.chucker.**

# ------------------------------------------------------------------------------
# AndroidX Navigation
# ------------------------------------------------------------------------------
-keep class androidx.navigation.** { *; }
-dontwarn androidx.navigation.**

# ------------------------------------------------------------------------------
# AndroidX Fragment / ViewModel / Lifecycle
# ------------------------------------------------------------------------------
-keep class androidx.fragment.app.** { *; }
-keep class androidx.lifecycle.** { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>();
    <init>(...);
}
-keepclassmembers class * extends androidx.lifecycle.AndroidViewModel {
    <init>(android.app.Application);
}

# ------------------------------------------------------------------------------
# Material / DrawerLayout
# ------------------------------------------------------------------------------
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**
-keep class androidx.drawerlayout.** { *; }

# ------------------------------------------------------------------------------
# OTP View
# ------------------------------------------------------------------------------
-keep class com.chaos.view.** { *; }
-dontwarn com.chaos.view.**

# ------------------------------------------------------------------------------
# AiLink SDK (weighing scale)
# ------------------------------------------------------------------------------
-keep class com.ailink.** { *; }
-keep interface com.ailink.** { *; }
-dontwarn com.ailink.**

# ------------------------------------------------------------------------------
# Biosenselib AAR
# ------------------------------------------------------------------------------
-keep class com.biosense.** { *; }
-keep class biosense.** { *; }
-dontwarn com.biosense.**

# ------------------------------------------------------------------------------
# Prowess SDK
# ------------------------------------------------------------------------------
-keep class com.prowess.** { *; }
-keep interface com.prowess.** { *; }
-dontwarn com.prowess.**

# ------------------------------------------------------------------------------
# libQBlueQPP JAR
# ------------------------------------------------------------------------------
-keep class com.qpp.** { *; }
-keep class qpp.** { *; }
-dontwarn com.qpp.**
-dontwarn qpp.**

# ------------------------------------------------------------------------------
# Evolute SDK (AIDL service)
# ------------------------------------------------------------------------------
-dontwarn com.evolute.sdkservice.AidlSDK
-dontwarn com.evolute.sdkservice.Mode
-dontwarn com.evolute.**

# ------------------------------------------------------------------------------
# FPS SDK
# ------------------------------------------------------------------------------
-dontwarn com.fpscommonSDK.fpssdkservice.FPS_Service$1
-dontwarn com.fpscommonSDK.**

# ------------------------------------------------------------------------------
# App — ApiResponse sealed class (critical for R8 generic type preservation)
# ------------------------------------------------------------------------------
-keep class com.test.healthbox_app.domain.model.ApiResponse { *; }
-keep class com.test.healthbox_app.domain.model.ApiResponse$ApiSuccess { *; }
-keep class com.test.healthbox_app.domain.model.ApiResponse$ApiError { *; }
-keep class com.test.healthbox_app.domain.model.ApiResponse$ApiLoading { *; }
-keepclassmembers class com.test.healthbox_app.domain.model.ApiResponse$* {
    public <init>(...);
    *;
}

# ------------------------------------------------------------------------------
# App — Model & domain classes (Gson/Moshi/Retrofit deserialization)
# ------------------------------------------------------------------------------
-keep class com.test.healthbox_app.domain.model.** { *; }
-keep class com.test.healthbox_app.data.model.** { *; }
-keep class com.test.healthbox_app.data.remote.** { *; }
-keep class com.test.healthbox_app.data.network.** { *; }

# Keep all @SerializedName annotated fields project-wide
-keepclassmembers class com.test.healthbox_app.** {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ------------------------------------------------------------------------------
# App — DI Module (Hilt @Provides reflection)
# ------------------------------------------------------------------------------
-keep class com.test.healthbox_app.di.** { *; }
-keepclassmembers class com.test.healthbox_app.di.** { *; }

# ------------------------------------------------------------------------------
# App — Specific entry points
# ------------------------------------------------------------------------------
-keep class com.test.healthbox_app.presentation.splash.SplashFragment { *; }

# Keep all Activities, Fragments, Services, Receivers
-keep public class * extends android.app.Activity
-keep public class * extends androidx.fragment.app.Fragment
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# Keep View constructors used by XML inflation
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

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

# Keep R class fields
-keepclassmembers class **.R$* {
    public static <fields>;
}

-keep class com.test.healthbox_app.** { *; }

-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class cn.net.aicare.algorithmutil.BodyFatData { *; }
-keep class cn.net.aicare.algorithmutil.AlgorithmUtil { *; }
-keep class com.test.healthbox_app.data.ble.QppManager { *; }
-keep class com.test.healthbox_app.data.ble.GlucoseReading { *; }

# ------------------------------------------------------------------------------
# Suppress common benign warnings
# ------------------------------------------------------------------------------
-dontwarn sun.misc.**
-dontwarn java.lang.invoke.**
-dontwarn javax.annotation.**
-dontwarn org.jetbrains.annotations.**