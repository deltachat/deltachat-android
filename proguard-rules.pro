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