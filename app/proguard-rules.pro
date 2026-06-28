# DriverPro Security Rules

# Ofuscar todo excepto lo necesario
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

# Mantener clases Android necesarias
-keep public class * extends android.app.Activity
-keep public class * extends android.accessibilityservice.AccessibilityService
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver

# Mantener Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Ofuscar logica del bot
-obfuscationdictionary proguard-dict.txt
-classobfuscationdictionary proguard-dict.txt
-packageobfuscationdictionary proguard-dict.txt

# Anti-debug
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# Remover strings de debug
-assumenosideeffects class java.io.PrintStream {
    public void println(...);
    public void print(...);
}
