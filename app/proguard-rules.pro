# ============================================================
# StreamLauncher ProGuard Rules
# ============================================================
# 대부분의 AndroidX / Firebase / Hilt / Coil / Paging3 라이브러리는
# AAR 내에 자체 consumer ProGuard 룰을 포함하므로 별도 추가 불필요.
# 아래 항목만 명시적으로 관리한다.

# ------------------------------------------------------------
# 디버깅용: 스택트레이스에 소스 파일명 / 줄번호 보존
# (Firebase Crashlytics 리포트에서도 사용됨)
# ------------------------------------------------------------
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ------------------------------------------------------------
# kotlinx-serialization
# @Serializable 클래스의 companion object(serializer)를 보존한다.
# R8이 unused member로 제거하는 것을 방지.
# ------------------------------------------------------------
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keep @kotlinx.serialization.Serializable class * {
    static ** Companion;
    static ** serializer(...);
    ** INSTANCE;
}
-keepclassmembers @kotlinx.serialization.Serializable class * {
    static ** $serializer;
    static ** Companion;
    public static ** serializer(...);
    public <fields>;
}

# ------------------------------------------------------------
# Retrofit2
# 서비스 인터페이스 메서드 시그니처 보존 (Retrofit이 리플렉션으로 파싱)
# ------------------------------------------------------------
-keepattributes Signature, Exceptions

-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# ------------------------------------------------------------
# OkHttp3
# platform 클래스 관련 경고 억제
# ------------------------------------------------------------
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**

# ------------------------------------------------------------
# xmlutil (nl.adaptivity.xmlutil)
# XML 직렬화 어노테이션 처리 보존
# ------------------------------------------------------------
-keep class nl.adaptivity.xmlutil.** { *; }
-dontwarn nl.adaptivity.xmlutil.**

# ------------------------------------------------------------
# 릴리스 빌드에서 Log.v / Log.d 제거
# ------------------------------------------------------------
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}

# ------------------------------------------------------------
# Enum — .name / valueOf() 런타임 사용
# SettingsMenu.COLOR.name 으로 내비게이션 인자를 만들고,
# SettingsMenu.valueOf(route.menu) 로 다시 파싱한다.
# R8이 enum 상수 이름(COLOR → a)을 난독화하면 valueOf()가
# IllegalArgumentException 으로 크래시하므로 상수 이름 전체를 보존한다.
# ------------------------------------------------------------
-keep enum org.comon.streamlauncher.settings.navigation.SettingsMenu { *; }

# ------------------------------------------------------------
# Firebase Firestore DTO 클래스
# Firestore toObject() 는 리플렉션으로 getter/setter를 탐색한다.
# R8이 getter 이름을 난독화하면 BeanMapper가
# "Found two getters or fields with conflicting case sensitivity"
# 오류를 발생시키므로, DTO 패키지 전체를 보존한다.
# ------------------------------------------------------------
-keep class org.comon.streamlauncher.data.remote.firestore.** { *; }
-keepclassmembers class org.comon.streamlauncher.data.remote.firestore.** {
    *;
}
