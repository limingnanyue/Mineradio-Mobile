# ExoPlayer / Retrofit / OkHttp / Gson keep rules
-keep class com.google.gson.** { *; }
-keep class com.squareup.okhttp3.** { *; }
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.mineradio.player.data.api.dto.** { *; }

# ExoPlayer
-keep class androidx.media3.** { *; }
