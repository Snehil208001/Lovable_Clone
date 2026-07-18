# AuraCode ProGuard / R8 rules

# --- kotlinx.serialization ---
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
# Keep generated serializers and @Serializable classes' companion serializer accessors.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep all DTOs and domain models (serialized over the wire).
-keep class com.snehil.auracode.data.remote.dto.** { *; }
-keep class com.snehil.auracode.domain.model.** { *; }

# --- Retrofit / OkHttp ---
-keepattributes Signature, Exceptions
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# OkHttp SSE
-dontwarn okhttp3.internal.sse.**
