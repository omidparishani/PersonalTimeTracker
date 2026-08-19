# Personal Time Tracker (Native Android)

اپلیکیشن کاملاً آفلاین حضور و غیاب + تسک + گزارش + پشتیبان‌گیری

**Kotlin + Room + Material 3**

## باز کردن در Android Studio

1. Android Studio را باز کنید (Hedgehog یا جدیدتر)
2. **File → Open** و پوشه `PersonalTimeTracker` را انتخاب کنید
3. صبر کنید تا Gradle Sync تمام شود
4. مسیر SDK را در `local.properties` تنظیم کنید:

```
sdk.dir=C:\\Users\\YOUR_USER\\AppData\\Local\\Android\\Sdk
```

5. دکمه **Run** را بزنید

## ساخت APK ریلیز (بدون هشدار Play Protect)

نصب APK دیباگ معمولاً هشدار اسکن گوگل می‌دهد. برای نصب روی گوشی از **release امضاشده** استفاده کنید.

یک‌بار کلید امضا بسازید:

```
powershell -ExecutionPolicy Bypass -File scripts/create-keystore.ps1
```

سپس `keystore.properties.example` را به `keystore.properties` کپی کنید و رمز را پر کنید. فایل‌های `ptt-release.jks` و `keystore.properties` را commit نکنید.

از Android Studio:

```
Build → Generate Signed Bundle / APK → APK → release
```

یا با Gradle (اگر Wrapper دارید):

```
gradle :app:assembleRelease
```

خروجی:

```
app/build/outputs/apk/release/app-release.apk
```

همین کلید را برای همه آپدیت‌ها نگه دارید؛ در غیر این صورت نصب روی نسخه قبلی خطا می‌دهد.

## ساخت APK در GitHub Actions

فایل گردش‌کار: `.github/workflows/android-release.yml`

با هر push به `main`/`master`، تگ `v*`، Pull Request، یا اجرای دستی **Run workflow** یک APK ریلیز ساخته می‌شود.

در Settings → Secrets and variables → Actions این‌ها را اضافه کنید:

| Secret | مقدار |
|---|---|
| `KEYSTORE_BASE64` | خروجی base64 فایل `ptt-release.jks` |
| `KEYSTORE_PASSWORD` | رمز keystore |
| `KEY_ALIAS` | `ptt` |
| `KEY_PASSWORD` | رمز کلید |

ساخت base64 در PowerShell:

```
[Convert]::ToBase64String([IO.File]::ReadAllBytes("ptt-release.jks")) | Set-Clipboard
```

APK از تب **Actions → artifact** با نام `PersonalTimeTracker-release` دانلود می‌شود.

بدون این secretها هم بیلد انجام می‌شود ولی با کلید دیباگ امضا می‌شود و Play Protect ممکن است هشدار بدهد.

## آیکون پویا

آیکون لانچر ساعت کار امروز را به‌صورت ۰ تا ۱۰+ نشان می‌دهد و هر ۱۵ دقیقه (و بعد از ورود/خروج) به‌روز می‌شود.

برای نمایش دقیق «ساعت:دقیقه» به فارسی، ویجت ۱×۱ **ساعت کار امروز** را روی صفحه اصلی بگذارید.

## قابلیت‌ها

- ورود / خروج + ویرایش/حذف
- محاسبه مرخصی و پایان پیشنهادی کار
- تسک با تایمر و لاگ
- گزارش روزانه/هفتگی/ماهانه
- تقویم شمسی و تعطیلات
- Dark Mode
- پشتیبان JSON + پشتیبان خودکار
- ویجت ورود/خروج و ویجت ساعت کار

## نیازمندی‌ها

- Android Studio
- JDK 17
- minSdk 26 (Android 8.0+)
