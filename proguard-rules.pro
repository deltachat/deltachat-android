# native methods
-keep class com.b44t.messenger.** { * ; }

# Keep metadata needed by the JSON parser
-keep class chat.delta.rpc.** { * ; }
-keepattributes *Annotation*,EnclosingMethod,Signature
-keepnames class com.fasterxml.jackson.** { *; }

# unused SealedData constructor needed by JsonUtils
-keep class org.thoughtcrime.securesms.crypto.KeyStoreHelper* { *; }

-dontwarn com.google.firebase.analytics.connector.AnalyticsConnector

# Keep WebRTC classes
-keep class org.webrtc.** { *; }
-keepclassmembers class org.webrtc.** { *; }
-keepattributes InnerClasses

# WorkManager-related rules
-keep class * extends androidx.room.RoomDatabase { *; }

# Temporary fix until we upgrade AGP that comes with patched https://issuetracker.google.com/issues/562960348
# Keep LogSessionId class and related classes
-keep class android.media.metrics.LogSessionId { *; }
-keep class android.media.metrics.** { *; }
# Keep Media3 classes that use reflection
-keep class androidx.media3.** { *; }
-dontwarn android.media.metrics.**
