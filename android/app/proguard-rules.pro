# ============================================================================
# ProGuard / R8 Rules for NKUST Suggest Android
# ============================================================================

# 1. 基礎防護與除錯堆疊保留
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# 2. 保留專案自身資料模型 (Data Models) 防止反射序列化異常
-keep class com.nkust.suggest.data.model.** { *; }

# 3. OkHttp 3 / 4 官方 R8 規則
-keepattributes Signature
-keepattributes *Annotation*
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# 4. JavaMail (Android) & Java Activation Framework (JAF) 反射保留
# JavaMail 依賴 META-INF/services 與 Class.forName 動態尋找 Protocol Provider (imaps)
-keep class javax.mail.** { *; }
-keep interface javax.mail.** { *; }
-keep class com.sun.mail.** { *; }
-keep interface com.sun.mail.** { *; }
-keep class javax.activation.** { *; }
-keep interface javax.activation.** { *; }
-dontwarn javax.mail.**
-dontwarn com.sun.mail.**
-dontwarn javax.activation.**

# 5. Jetpack Security (EncryptedSharedPreferences / Tink)
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**
-dontwarn com.google.crypto.tink.**
-dontwarn com.google.errorprone.annotations.**

# 6. Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler { *; }
-dontwarn kotlinx.coroutines.**
