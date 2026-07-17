-keepattributes Signature
-keep class com.onlylipu.cloud.data.api.** { *; }
-keep class org.webrtc.** { *; }
-dontwarn org.webrtc.**
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
