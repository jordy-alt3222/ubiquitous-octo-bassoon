# kotlinx.serialization: keep generated serializers for the broadcast models.
-keepattributes *Annotation*, InnerClasses
-keepclassmembers @kotlinx.serialization.Serializable class com.example.spelunkrtv.data.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class com.example.spelunkrtv.data.**$$serializer { *; }
