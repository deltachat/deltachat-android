# native methods
-keep class com.b44t.messenger.** { * ; }

# Keep metadata needed by the JSON parser
-keep class chat.delta.rpc.** { * ; }
-keepattributes *Annotation*,EnclosingMethod,Signature
-keepnames class com.fasterxml.jackson.** { *; }

# bug with video recoder
-keep class com.coremedia.iso.** { *; }

# unused SealedData constructor needed by JsonUtils
-keep class org.thoughtcrime.securesms.crypto.KeyStoreHelper* { *; }

-dontwarn com.google.firebase.analytics.connector.AnalyticsConnector

# Keep WebRTC classes
-keep class org.webrtc.** { *; }
-keepclassmembers class org.webrtc.** { *; }
-keepattributes InnerClasses

# Keep LuaJ plugin engine classes (referenced via reflection / Class.forName,
# e.g. org.luaj.vm2.lib.Bit32Lib$Bit32LibV, otherwise stripped by R8)
-keep class org.luaj.vm2.** { *; }
-dontwarn org.luaj.**


# WorkManager-related rules
-keep class * extends androidx.room.RoomDatabase { *; }
