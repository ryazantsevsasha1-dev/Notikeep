# Notikeep ProGuard/R8 rules.
# Room and Hilt generate code that is safe under R8; keep only what reflection needs.

# Keep the NotificationListenerService entry point (bound by the system by name).
-keep class com.notikeep.data.service.NotikeepListenerService { *; }

# Room generated implementations rely on their names.
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }

# Keep Kotlin metadata for reflection-free serialization safety.
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
