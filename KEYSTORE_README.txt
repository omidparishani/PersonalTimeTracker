============================================
  Keystore انتشار Personal Time Tracker
============================================

فایل‌ها:
  - ptt-release.jks          ← فایل کلید (خیلی مهم)
  - keystore.properties      ← تنظیمات Gradle

مقادیر:
  storePassword / keyPassword : PttBazaar2026Secure!
  keyAlias                    : ptt_release
  اعتبار                      : حدود 25 سال

نصب در پروژه:
  1) ptt-release.jks را در ریشه پروژه (کنار build.gradle.kts) کپی کنید
  2) keystore.properties را هم در ریشه پروژه بگذارید
  3) این دو فایل را در Git commit نکنید (.gitignore)

ساخت APK ریلیز:
  gradlew.bat :app:assembleRelease

خروجی:
  app\build\outputs\apk\release\app-release.apk

هشدار مهم:
  - اگر این فایل را گم کنید، دیگر نمی‌توانید آپدیت روی همان اپ در بازار بدهید.
  - رمز و فایل را در جای امن (بکاپ آفلاین) نگه دارید.
  - بعد از اولین انتشار، برای هر آپدیت باید با همین کلید امضا کنید.
============================================
