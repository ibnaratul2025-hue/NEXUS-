# ProGuard & R8 Optimization Rules for NEXUS
# Preserves JNI boundaries, Room SQLite entities, Kotlin Coroutines, and Moshi Serialization.

# Keep native methods and JNI callback classes
-keepclasseswithmembernames class * {
    native <methods>;
}

# Preserve JNI adapter and callback interfaces
-keep class com.example.nexus.core.model.LlamaCppNativeAdapter { *; }
-keep class com.example.nexus.core.model.NativeTokenCallback { *; }
-keepclassmembers class com.example.nexus.core.model.NativeTokenCallback {
    public boolean onToken(java.lang.String);
}

# Room Database Entities and DAOs
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public void <init>();
}
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.paging.**

# Moshi & JSON Model Classes
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keep @com.squareup.moshi.JsonClass class * { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json(name=*) <fields>;
}

# Kotlin Coroutines & Flow
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.** {
    volatile <fields>;
}

# Android Architecture Components & Lifecycle
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    public <init>(...);
}

# Preserve line numbers and source attributes for production crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

