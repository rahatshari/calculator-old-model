# Proguard configuration for maximum optimization
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}
-dontobfuscate
