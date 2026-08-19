# Keep app, Room, and entry points used from the manifest / WorkManager
-keep class com.personal.timetracker.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class * extends android.appwidget.AppWidgetProvider { *; }
-keep class * extends android.content.BroadcastReceiver { *; }
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }

-dontwarn org.bouncycastle.**
-dontwarn javax.annotation.**
