# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Android\sdk/tools/proguard/proguard-android.txt
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

##---------------Begin: proguard configuration for Gson  ----------
# Gson uses generic type information stored in a class file when working with fields. Proguard
# removes such information by default, so configure it to keep all of it.
# see https://github.com/google/gson/blob/master/examples/android-proguard-example/proguard.cfg
-keepattributes Signature

# Gson specific classes
-dontwarn sun.misc.**
#-keep class sun.misc.Unsafe { *; }

# Application classes that will be serialized/deserialized over Gson
# Recommend all models go into a specific package and then just one
# line to exclude them all.
-keep class com.coolftc.prompt.source.** { <fields>; }
#-keep class com.coolftc.prompt.source.** { *; }

# Prevent R8 from leaving Data object members always null
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}