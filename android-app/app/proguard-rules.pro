# tabzero. - Production ProGuard / R8 Rules

# 1. Prevent R8 from illegal package repackaging / renaming of Kotlin standard library
-keep class kotlin.** { *; }
-keep class kotlin.text.** { *; }
-keep class kotlinx.** { *; }
-dontwarn kotlin.**
-dontwarn kotlinx.**

# 2. Preserve Annotations and Reflection Metadata
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature

# 3. Room Database & Generated Implementations
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Entity class * { *; }
-keep class com.example.detoxproject.data.local.** { *; }
-keep class * extends androidx.room.migration.Migration
-dontwarn androidx.room.paging.**

# 4. Kotlinx Serialization
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,allowobfuscation,allowshrinking @kotlinx.serialization.Serializable class * { *; }

# 5. Health Connect Platform & Records
-keep class androidx.health.connect.client.records.** { *; }
-keep class androidx.health.connect.client.PermissionController { *; }
-keep class androidx.health.connect.client.contracts.** { *; }
-keepclassmembers class * extends androidx.health.platform.client.proto.GeneratedMessageLite { *; }

# 6. Tabzero Domain Models & Entities
-keep class com.example.detoxproject.domain.model.** { *; }
-keep class com.example.detoxproject.data.local.entity.** { *; }
