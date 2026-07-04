# DriverPro MXL - Reglas de Seguridad Avanzada

# Optimizacion maxima
-optimizationpasses 10
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

# OFUSCACION AGRESIVA
# Renombrar clases criticas con nombres confusos
-repackageclasses 'x'
-allowaccessmodification
-flattenpackagehierarchy 'x'

# Mantener solo lo necesario de Android
-keep public class * extends android.app.Activity { public *; }
-keep public class * extends android.accessibilityservice.AccessibilityService { *; }
-keep public class * extends android.app.Service { *; }
-keep public class * extends android.content.BroadcastReceiver { *; }

# Mantener Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Mantener Maps
-keep class com.google.android.gms.maps.** { *; }

# ELIMINAR TODOS LOS LOGS
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
    public static *** wtf(...);
}

# Eliminar prints de debug
-assumenosideeffects class java.io.PrintStream {
    public void println(...);
    public void print(...);
}

# Ofuscar strings sensibles
-adaptclassstrings
-adaptresourcefilenames
-adaptresourcefilecontents

# Mantener SecurityManager sin ofuscar nombres de metodos publicos
-keep class com.mxl.driverpro.SecurityManager { *; }

# Ofuscar logica del bot completamente
-obfuscationdictionary proguard-dict.txt
-classobfuscationdictionary proguard-dict.txt
-packageobfuscationdictionary proguard-dict.txt

# Anti-debug adicional
-keep class com.mxl.driverpro.MainActivity {
    private void verificarSeguridad();
}
