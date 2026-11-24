# Preserve Google API Client & Gmail model classes for JSON reflection (@Key)
-keep class com.google.api.client.** { *; }
-keep class com.google.api.services.gmail.** { *; }
-keepclassmembers class * extends com.google.api.client.json.GenericJson {
    <init>(...);
    @com.google.api.client.util.Key <fields>;
}
-keepattributes Signature,RuntimeVisibleAnnotations,AnnotationDefault

# Suppress missing desktop JDK / legacy Apache HTTP warnings from Google's HTTP client on Android
-dontwarn javax.naming.**
-dontwarn org.apache.http.**