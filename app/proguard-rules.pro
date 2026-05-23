# --------------------------------------------------------------------------
# Inkleaf R8 / ProGuard rules
# --------------------------------------------------------------------------

# Keep all Room entities & DAOs (Room generates code via annotation processing
# but field/method names are reflected on at migration time).
-keep class com.prawalsharma.inkleaf.data.model.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keepclassmembers @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# Hilt / Dagger generated code
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.HiltAndroidApp
-keep,allowobfuscation @interface dagger.hilt.**
-keep,allowobfuscation @interface javax.inject.**
-keepclasseswithmembers class * { @javax.inject.Inject <init>(...); }

# Kotlin coroutines
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# Compose runtime — keep @Composable annotations
-keep,allowobfuscation,allowshrinking class androidx.compose.runtime.Composable

# Glance widget receiver is referenced from manifest
-keep class com.prawalsharma.inkleaf.ui.widget.** { *; }

# kotlinx.serialization / DataStore proto
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite { *; }

# Keep generic signatures for reflection (Room, Hilt, kotlin.reflect)
-keepattributes Signature, InnerClasses, EnclosingMethod, RuntimeVisibleAnnotations,
                RuntimeVisibleParameterAnnotations, AnnotationDefault

# Don't strip line numbers — useful for crash reports if you ever add them later.
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile

