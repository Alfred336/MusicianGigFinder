# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Models — must not be obfuscated (used with Firestore @DocumentId)
-keep class com.musicianfinder.models.** { *; }

# ClickPesa / JSON responses
-keep class com.musicianfinder.payment.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { *; }
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

# CircleImageView
-keep class de.hdodenhof.** { *; }

# Suppress warnings
-dontwarn javax.annotation.**
