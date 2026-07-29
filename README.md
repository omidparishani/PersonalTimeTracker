# Personal Time Tracker (Native Android)

اپلیکیشن کاملاً آفلاین حضور و غیاب + تسک + گزارش + پشتیبان‌گیری

**Kotlin + Room + Material 3 — بدون Flutter**

## باز کردن در Android Studio

1. Android Studio را باز کنید (Hedgehog یا جدیدتر پیشنهاد می‌شود)
2. **File → Open** و پوشه `PersonalTimeTracker` را انتخاب کنید
3. صبر کنید تا Gradle Sync تمام شود
4. اگر SDK path خواست، مسیر SDK را در `local.properties` تنظیم کنید:

```
sdk.dir=C:\\Users\\YOUR_USER\\AppData\\Local\\Android\\Sdk
```

5. یک Emulator بسازید یا گوشی را با USB Debugging وصل کنید
6. دکمه **Run** (سبز) را بزنید

## ساخت APK

منوی:

```
Build → Build Bundle(s) / APK(s) → Build APK(s)
```

یا از Terminal داخل Android Studio:

```
./gradlew assembleRelease
```

خروجی:

```
app/build/outputs/apk/release/app-release-unsigned.apk
```

برای نصب روی گوشی می‌توانید از `assembleDebug` استفاده کنید:

```
./gradlew assembleDebug
```

فایل: `app/build/outputs/apk/debug/app-debug.apk`

## قابلیت‌ها

- ورود / خروج (الان یا تاریخ دلخواه) + ویرایش/حذف
- محاسبه مرخصی و پایان پیشنهادی کار
- تسک با تایمر Start/Stop و ویرایش مدت
- پروژه‌های Jira در تنظیمات
- گزارش روزانه/هفتگی/ماهانه
- تقویم
- تاریخ شمسی
- RTL فارسی
- Dark Mode
- پشتیبان JSON + بازیابی + پاک کردن داده
- کاملاً آفلاین (Room/SQLite)

## نیازمندی‌ها

- Android Studio
- JDK 17
- minSdk 26 (Android 8.0+)
