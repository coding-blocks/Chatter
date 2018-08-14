# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\Sheng\Develop\android\android-sdk-windows/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# -libraryjars libs/java-WebSocket-1.2.1.jar
-dontwarn org.java_websocket.**
-keep class org.java_websocket.** { *; }
-keep interface org.java_websocket.** { *; }
-keep class com.elirex.fayeclient.** { *; }
-keep interface com.elirex.fayeclient.** { *; }
-keepattributes Signature
