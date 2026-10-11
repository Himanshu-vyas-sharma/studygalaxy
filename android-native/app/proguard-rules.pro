# Keep Retrofit models (uses reflection via serialization)
-keep,allowobfuscation,allowshrinking class kotlinx.serialization.** { *; }
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
