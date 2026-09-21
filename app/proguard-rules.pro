# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.recipebookmark.** {
    *** Companion;
}
-keepclasseswithmembers class com.recipebookmark.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.recipebookmark.**$$serializer { *; }

# Room
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-dontwarn androidx.room.paging.**

# Jsoup
-keep class org.jsoup.** { *; }
-dontwarn org.jsoup.**
