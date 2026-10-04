# Elenchos ProGuard / R8 Rules

# Preserve Kotlinx Serialization models
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt

-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keepclassmembers class * {
    companion object;
}
-keepnames class com.example.elenchos.domain.model.** { *; }

# Preserve Coroutines internal mechanics
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler { *; }

# Compose and Material3 Keep Rules
-keep class androidx.compose.material3.** { *; }
-keep class androidx.compose.ui.** { *; }

# Preserve Accessibility Service class
-keep public class com.example.elenchos.service.ElenchosLabAccessibilityService {
    public <init>();
    public *;
}
