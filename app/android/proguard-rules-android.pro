-dontwarn org.slf4j.helpers.SubstituteLogger

# Keep JNI native methods used by libvivoseckey.so
-keepclassmembers class com.vivo.seckeysdk.utils.** { native <methods>; }
-keep class com.vivo.seckeysdk.utils.** { *; }
-keepclassmembers class com.mytiantian.updater.crypto.** { native <methods>; }
