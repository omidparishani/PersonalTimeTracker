# یکپارچه‌سازی کامل جیرا

## آنچه پیاده‌سازی شده

### تنظیمات
- آدرس سرور + Personal Access Token
- فعال/غیرفعال‌سازی
- تست اتصال (`/rest/api/2/myself`)

### صفحه جیرا (از تب تسک‌ها → «جیرا شرکت»)
سه بخش:

1. **اساین‌شده**
   - لیست Issueهای assign‌شده به شما از سرور شرکت
   - فیلتر «فقط باز» / «همه»
   - جستجوی محلی روی عنوان و کلید
   - ثبت Worklog، مشاهده جزئیات، افزودن به علاقه‌مندی

2. **علاقه‌مندی‌ها**
   - برای Issueهایی که به شما assign نیستند (Daily، جلسات، پشتیبانی و ...)
   - افزودن با کلید Issue (مثلاً `DAILY-1`)
   - ثبت لاگ مستقیم روی همان Issue برای روز دلخواه
   - حذف از علاقه‌مندی‌ها

3. **جستجو**
   - جستجو با کلید دقیق یا متن در سرور جیرا

### عملیات روی هر Issue
- دریافت و نمایش **Worklogهای ثبت‌شده روی سرور**
- **ثبت Worklog** برای روز خاص (با توضیح)
- دریافت و نمایش **کامنت‌ها**
- **افزودن کامنت**
- دکمه سراسری «لاگ روی Issue دلخواه» بدون نیاز به assign یا علاقه‌مندی

### همگام با تسک محلی
- دکمه «سینک Issueهای اساین به تسک محلی» در صفحه تسک‌ها
- با ثبت لاگ روی تسک محلی که `jiraNumber` دارد، Worklog به جیرا هم ارسال می‌شود

## ساختار فایل‌ها

```
jira/
  JiraApi.kt          # Retrofit endpoints
  JiraClient.kt       # OkHttp + Bearer PAT
  JiraModels.kt       # مدل‌های JSON
  JiraService.kt      # منطق سطح بالا

data/entity/JiraFavoriteEntity.kt
data/dao/JiraFavoriteDao.kt

ui/jira/JiraFragment.kt   # UI اصلی جیرا
```

نسخه دیتابیس: **۹** (جدول `jira_favorites`)

## وابستگی‌ها (app/build.gradle.kts)
```
okhttp 4.12
logging-interceptor 4.12
retrofit 2.11
converter-gson 2.11
gson 2.11
```

## نحوه استفاده

1. تنظیمات → اتصال به جیرا → آدرس + توکن → تست → ذخیره
2. تسک‌ها → **جیرا شرکت**
3. برای جلسات/Daily: تب علاقه‌مندی‌ها → ＋ علاقه‌مندی → کلید Issue
4. روی هر کارت: ⏱ لاگ برای ثبت زمان روی سرور شرکت

## APIهای استفاده‌شده
- `GET /myself`, `GET /search`, `GET /issue/{key}`
- `GET|POST /issue/{key}/worklog`
- `PUT|DELETE /issue/{key}/worklog/{id}`
- `GET|POST /issue/{key}/comment`
- `POST /issue` (ایجاد — آماده در سرویس)
- `PUT /issue/{key}` (ویرایش summary — آماده در سرویس)

## محدودیت‌های فعلی
- ایجاد Issue از UI هنوز فرم جدا ندارد (متد سرویس آماده است)
- ویرایش/حذف Worklog از UI هنوز نیست (API آماده است)
- انتقال وضعیت (transition) Issue پیاده نشده

## امنیت
توکن را در گیت commit نکنید. در صورت لو رفتن، در جیرا revoke کنید.
