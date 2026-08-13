# Clickme ProGuard rules

# Keep model, repo, service and receiver from being shrunk/obfuscated incorrectly
-keep class com.clickme.app.model.** { *; }
-keep class com.clickme.app.repo.** { *; }
-keep class com.clickme.app.NotificationService { *; }
-keep class com.clickme.app.BootReceiver { *; }
-keep class com.clickme.app.MainActivity { *; }
-keep class com.clickme.app.ui.** { *; }

# Keep parcelable/serializable if any
-keepattributes *Annotation*
