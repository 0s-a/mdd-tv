# ProGuard and R8 optimization rules for MDD TV

# 1. Android & Jetpack General
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# 2. Kotlin & Kotlinx Coroutines
-keep class kotlin.Metadata { *; }
-keepclassmembers class * extends kotlin.coroutines.jvm.internal.ContinuationImpl {
    <fields>;
}
-keep class kotlinx.coroutines.** { *; }

# 3. Kotlinx Serialization
-keepattributes *Annotation*,Signature
-keep class kotlinx.serialization.json.** { *; }
-keep class * implements kotlinx.serialization.KSerializer {
    *;
}
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepclassmembers class * {
    companion object;
}
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <methods>;
}
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable class *;
}
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers class * {
    @kotlinx.serialization.SerialInfo *;
}

# 4. Domain & Data Models (DTOs)
-keep class org.jellyfin.mobile.data.remote.dto.** { *; }
-keep class org.jellyfin.mobile.data.remote.model.** { *; }
-keep class org.jellyfin.mobile.data.local.entity.** { *; }
-keep class org.jellyfin.mobile.domain.model.** { *; }

# 5. Room Database
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Dao class * { *; }
-keep class * extends androidx.room.RoomDatabase {
    *;
}
-keep class *_Impl { *; }
-keep class androidx.room.paging.** { *; }

# 6. Retrofit & OkHttp
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# 7. Koin Dependency Injection
-keep class org.koin.** { *; }
-keep class io.insert.koin.** { *; }

# 8. Media3 (ExoPlayer, Session, UI, Cache & Extractors)
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.session.** { *; }
-keep class androidx.media3.ui.** { *; }
-keep class androidx.media3.common.** { *; }
-keep class androidx.media3.datasource.** { *; }
-keep class androidx.media3.database.** { *; }
-keep class androidx.media3.extractor.** { *; }
-keep class androidx.media3.decoder.** { *; }

# 9. Coil Image Loader
-keep class coil.** { *; }
-dontwarn coil.**

# 10. Compose & Material3
-keep class androidx.compose.material3.** { *; }
-keep class androidx.compose.ui.** { *; }
