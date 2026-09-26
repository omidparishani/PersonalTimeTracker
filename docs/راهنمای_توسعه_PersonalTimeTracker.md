# راهنمای کامل آموزش و توسعه — Personal Time Tracker

> مخاطب این سند: برنامه‌نویس **جاوا** که هنوز اندروید کار نکرده است.  
> هدف: فهم کل پروژه، معماری، هر ماژول، و نحوهٔ توسعه و بهبود بدون به‌هم‌ریختن فرمت و خوانایی.

---

## فهرست مطالب

1. [اپ چیست و چه می‌کند؟](#1-اپ-چیست-و-چه-میکند)
2. [از جاوا تا اندروید — مفاهیم ضروری](#2-از-جاوا-تا-اندروید--مفاهیم-ضروری)
3. [ساختار پوشه‌ها و فایل‌ها](#3-ساختار-پوشهها-و-فایلها)
4. [معماری کلی اپ](#4-معماری-کلی-اپ)
5. [لایه داده (Room و Repository)](#5-لایه-داده-room-و-repository)
6. [رابط کاربری (Activity و Fragment)](#6-رابط-کاربری-activity-و-fragment)
7. [ماژول جیرا (Jira)](#7-ماژول-جیرا-jira)
8. [ابزارها و Utilityها](#8-ابزارها-و-utilityها)
9. [ویجت و کارهای پس‌زمینه](#9-ویجت-و-کارهای-پسزمینه)
10. [تنظیمات بیلد و نسخه ریلیز](#10-تنظیمات-بیلد-و-نسخه-ریلیز)
11. [چطور یک فیچر جدید اضافه کنیم؟](#11-چطور-یک-فیچر-جدید-اضافه-کنیم)
12. [نکات مهم هنگام توسعه](#12-نکات-مهم-هنگام-توسعه)
13. [نقشه راه بهبود](#13-نقشه-راه-بهبود)

---

## 1. اپ چیست و چه می‌کند؟

**Personal Time Tracker** یک اپ اندروید شخصی/شرکتی است که دو دنیای مختلف را با هم ترکیب می‌کند:

| حوزه | توضیح |
|------|--------|
| **تردد (Attendance)** | ورود/خروج روزانه، مرخصی، اضافه‌کار، کسری — **آفلاین** روی گوشی |
| **جیرا (Jira)** | لیست Issue، Worklog، ایجاد/ویرایش/حذف Issue — **آنلاین** با سرور شرکت |
| **تقویم و گزارش** | نمایش کارهای روز، جمع ساعات، نمودار |
| **تنظیمات** | توکن جیرا، تم، اثرانگشت، بکاپ، تعطیلات |

داده‌های حساس (توکن، لاگ‌ها) روی **دیتابیس محلی گوشی** ذخیره می‌شوند. ارتباط با جیرا با **Personal Access Token** است.

---

## 2. از جاوا تا اندروید — مفاهیم ضروری

اگر فقط جاوا بلدید، این جدول معادل‌های ذهنی است:

| در جاوا / بک‌اند | در این پروژه اندروید |
|------------------|----------------------|
| `main()` | کلاس `App` (Application) + `MainActivity` |
| Servlet / Controller | `Fragment` (هر تب یک صفحه) |
| Service لایه بیزنس | `AppRepository` |
| JPA Entity | کلاس با `@Entity` در Room |
| Repository / DAO | اینترفیس `@Dao` |
| HTTP Client (مثلاً RestTemplate) | Retrofit + OkHttp |
| JSON (Jackson) | Gson |
| Thread / Executor | Coroutines (`suspend` + `lifecycleScope`) |
| `synchronized` UI | همه کار UI فقط روی **Main Thread**؛ شبکه و DB روی **IO** |

### 2.1 Activity و Fragment

- **Activity**: یک «پنجره» اپ. در این پروژه تقریباً فقط `MainActivity` است.
- **Fragment**: یک «تکه صفحه» داخل Activity. تب‌های پایین هر کدام یک Fragment هستند:
  - داشبورد
  - تردد
  - تسک‌ها / جیرا
  - گزارش
  - تنظیمات

جابه‌جایی بین تب‌ها یعنی تعویض Fragment داخل همان Activity — نه باز کردن Activity جدید.

### 2.2 Lifecycle (چرخه عمر)

اندروید هر لحظه ممکن است صفحه را ببندد یا بسازد (چرخش صفحه، کمبود حافظه). برای همین:

- کار سنگین را در `onCreate` / `onCreateView` شروع می‌کنید.
- با `viewLifecycleOwner.lifecycleScope` وقتی صفحه از بین رفت، Coroutine خودکار لغو می‌شود.
- دادهٔ پایدار را در Room می‌گذارید، نه فقط در متغیر حافظه.

### 2.3 Kotlin در برابر جاوا (کافی برای خواندن کد)

| Kotlin | معادل ذهنی جاوا |
|--------|------------------|
| `val x = 1` | `final int x = 1` |
| `var x = 1` | `int x = 1` |
| `x?.length` | اگر null نباشد length |
| `x ?: "default"` | اگر null بود default |
| `suspend fun f()` | تابعی که می‌تواند بدون بلاک کردن Thread صبر کند (Coroutine) |
| `data class User(...)` | POJO با equals/hashCode/toString خودکار |
| `fun interface` / lambda | مثل Java 8 lambda |

زبان پروژه **Kotlin** است، ولی منطق همان OOP جاوا است.

---

## 3. ساختار پوشه‌ها و فایل‌ها

```
PersonalTimeTracker/
├── app/
│   ├── build.gradle.kts          ← وابستگی‌ها، minify، امضای ریلیز
│   ├── proguard-rules.pro        ← قوانین فشرده‌سازی کد ریلیز
│   └── src/main/
│       ├── AndroidManifest.xml   ← مجوزها، Activity، ویجت، Receiver
│       ├── java/com/personal/timetracker/
│       │   ├── App.kt            ← نقطه شروع سراسری اپ
│       │   ├── data/             ← دیتابیس و Repository
│       │   ├── jira/             ← کلاینت REST جیرا
│       │   ├── ui/               ← صفحه‌ها
│       │   ├── util/             ← ابزارهای مشترک
│       │   └── widget/           ← ویجت صفحه اصلی گوشی
│       └── res/                  ← layout، رنگ، آیکون، منو
├── build.gradle.kts              ← تنظیمات سطح پروژه
├── settings.gradle.kts
├── keystore.properties.example   ← نمونه تنظیمات امضای APK
└── docs / README
```

### بسته‌های Java/Kotlin

| بسته | نقش |
|------|-----|
| `com.personal.timetracker` | `App` |
| `...data.entity` | جداول دیتابیس |
| `...data.dao` | کوئری‌های SQL |
| `...data.db` | تعریف Room Database |
| `...data.repository` | منطق کسب‌وکار + هماهنگی DB و شبکه |
| `...jira` | API، مدل‌ها، فرم ایجاد Issue |
| `...ui.*` | Fragmentها و MainActivity |
| `...util` | تاریخ، تم، دیالوگ، بکاپ، نوتیف |
| `...widget` | ویجت |

---

## 4. معماری کلی اپ

معماری ساده و عملیاتی است (نزدیک به **Repository Pattern**):

```
┌─────────────────────────────────────────────┐
│  UI (Fragment / Activity)                   │
│  فقط نمایش و رویداد کلیک                    │
└──────────────────┬──────────────────────────┘
                   │ صدا زدن متدهای repository
                   ▼
┌─────────────────────────────────────────────┐
│  AppRepository                              │
│  • تردد، تسک، تنظیمات، تعطیلات              │
│  • سینک جیرا، Worklog، گزارش               │
└──────────┬───────────────────┬──────────────┘
           │                   │
           ▼                   ▼
┌──────────────────┐   ┌──────────────────────┐
│ Room (SQLite)    │   │ JiraService / Retrofit│
│ داده محلی        │   │ REST به سرور شرکت    │
└──────────────────┘   └──────────────────────┘
```

**قانون طلایی:** Fragment نباید مستقیماً Retrofit یا DAO را صدا بزند. همیشه از `AppRepository` عبور کند تا منطق یک‌جا بماند.

دسترسی به Repository از UI:

```kotlin
val repo = (requireActivity().application as App).repository
```

`App` در `onCreate` یک بار `AppRepository` را می‌سازد و همه Fragmentها همان نمونه را می‌گیرند (شبیه Singleton سطح اپ).

---

## 5. لایه داده (Room و Repository)

### 5.1 Room چیست؟

Room یک ORM روی SQLite است (شبیه یک Hibernate خیلی سبک برای موبایل).

سه جزء:

1. **Entity** — یک ردیف جدول (`@Entity`)
2. **DAO** — اینترفیس متدهای CRUD با `@Query` / `@Insert`
3. **Database** — کلاس abstract با `@Database` که DAOها را می‌دهد

فایل اصلی: `data/db/AppDatabase.kt` — فعلاً **version = 14**.

هر بار ساختار جدول عوض شود باید:

- شماره version را زیاد کنید
- یک `Migration` بنویسید (یا در توسعه `fallbackToDestructiveMigration` — که داده را پاک می‌کند)

### 5.2 Entityهای مهم

| Entity | جدول | کاربرد |
|--------|------|--------|
| `AttendanceEntity` | تردد روزانه | ورود، خروج، مدت، مرخصی |
| `TaskEntity` | تسک محلی قدیمی | در کنار جیرا هنوز هست |
| `TaskLogEntity` | لاگ زمانی محلی | fallback وقتی Worklog جیرا نباشد |
| `SettingsEntity` | تنظیمات | URL جیرا، توکن، تم، … |
| `HolidayEntity` | تعطیلات | برای محاسبه کسری |
| `JiraIssueCacheEntity` | کش Issue | کلید، خلاصه، وضعیت، پروژه |
| `JiraWorklogCacheEntity` | کش Worklog | مدت، تاریخ، نویسنده، remoteId |
| `JiraFavoriteEntity` | علاقه‌مندی | ستاره روی Issue |
| `JiraStatusEntity` | وضعیت‌های workflow | کش وضعیت‌ها |

**نکته Worklog:** فیلد `remoteId` یکتاست (ایندکس unique) تا بعد از سینک، ردیف تکراری ساخته نشود.

### 5.3 AppRepository — قلب اپ

فایل بزرگ: `data/repository/AppRepository.kt`

گروه‌های متد:

| گروه | نمونه متد | کار |
|------|-----------|-----|
| تنظیمات | `getSettings`, `saveSettings` | خواندن/نوشتن تنظیمات |
| تردد | `checkIn`, `checkOut`, `addAttendance` | ثبت ورود/خروج |
| تسک محلی | `saveTask`, `startTimer`, `stopTimer` | تایمر قدیمی |
| جیرا Issue | `refreshJiraStatuses`, مشاهده کش | همگام‌سازی لیست |
| Worklog | `syncWorklogsForDateRange`, `getJiraWorklogsForDate` | سینک و نمایش |
| گزارش | `report`, `jiraSummaryRange`, `dayBreakdown` | جمع ساعات |
| علاقه‌مندی | `toggleJiraFavorite` | ستاره |

**فیلتر کاربر در تقویم/گزارش:**  
متد `filterOwnWorklogs` فقط Worklogهایی را نگه می‌دارد که `authorName` با کاربر جاری یکی باشد. در کارت جزئیات Issue عمداً **همه** لاگ‌ها نشان داده می‌شود.

### 5.4 Coroutine و Dispatcher

```kotlin
suspend fun syncWorklogsForDate(date: String): Result<Int> =
    withContext(Dispatchers.IO) {
        // شبکه و دیتابیس اینجا
    }
```

- `Dispatchers.IO` = نخ مناسب شبکه/دیسک  
- `lifecycleScope.launch { }` از UI = شروع کار بدون قفل کردن صفحه  
- `Result<T>` = یا موفقیت یا Exception قابل‌نمایش

---

## 6. رابط کاربری (Activity و Fragment)

### 6.1 MainActivity

- تم روشن/تاریک و رنگ اصلی را از تنظیمات می‌خواند.
- منوی پایین (`bottomNav`) Fragment درست را باز می‌کند.
- در ورود، در صورت فعال بودن اثرانگشت، قفل می‌کند.
- سینک اولیه Worklog هفته جاری را در پس‌زمینه می‌زند.

### 6.2 Fragmentها

| Fragment | نقش کاربر |
|----------|-----------|
| `DashboardFragment` | خلاصه امروز، میانبرها |
| `AttendanceFragment` | ورود/خروج و لیست تردد |
| `TasksFragment` | **مرکز جیرا**: لیست Issue، ایجاد، ویرایش، لاگ، کامنت |
| `CalendarFragment` | روزبه‌روز تردد + Worklog همان روز (فقط خود کاربر) |
| `ReportsFragment` | بازه زمانی، نمودار، جمع پروژه‌ها/جیرا |
| `SettingsFragment` | URL، توکن، پروژه‌ها، بکاپ، تم |
| `JiraFragment` | نسخه/مسیر قدیمی‌تر؛ مسیر اصلی الان Tasks است |

بیشتر UI به‌صورت **کد Kotlin** ساخته می‌شود (نه فقط XML)، با کمک `ThemeHelper` و `DialogHelper` برای یکدست بودن ظاهر.

### 6.3 الگوی دیالوگ‌ها

`DialogHelper.show(..., onPositive = { ... })`:

- اگر `onPositive` مقدار **`false`** برگرداند، دیالوگ **بسته نمی‌شود** (مثلاً خطای اعتبارسنجی یا خطای سرور).
- اگر **`true`** برگرداند، دیالوگ بسته می‌شود.
- برای ایجاد Issue: اول درخواست شبکه؛ فقط در **موفقیت** `dialog.dismiss()` صدا زده می‌شود. در خطا، متن قرمز داخل فرم می‌ماند.

---

## 7. ماژول جیرا (Jira)

مسیر: `jira/`

### 7.1 لایه‌ها

```
TasksFragment / Calendar
        │
        ▼
  AppRepository.jiraServiceOrNull()
        │
        ▼
   JiraService   ← منطق خوانا برای اپ
        │
        ▼
   JiraApi (Retrofit interface)  ← تعریف URLها
        │
        ▼
   JiraClient (OkHttp + Gson + Bearer Token)
```

### 7.2 فایل‌ها

| فایل | نقش |
|------|-----|
| `JiraClient.kt` | ساخت Retrofit، هدر Authorization، `parseError` فارسی |
| `JiraApi.kt` | اینترفیس endpointها |
| `JiraModels.kt` | DTOهای JSON |
| `JiraService.kt` | متدهای سطح بالا: search، worklog، createmeta، create issue |
| `JiraIssueFormHelper.kt` | فرم پویا از روی meta فیلدها |

### 7.3 احراز هویت

در تنظیمات کاربر می‌گذارد:

- Base URL مثل `https://jira.demisco.com`
- Personal Access Token

`JiraClient` روی هر درخواست هدر می‌گذارد:

```text
Authorization: Bearer <token>
```

### 7.4 ایجاد Issue — جریان دو مرحله‌ای

1. **مرحله ۱:** انتخاب پروژه + نوع Issue (Task / Event / …)  
2. **مرحله ۲:** `fetchFieldsForIssueType` فیلدهای همان نوع را می‌گیرد و فرم را می‌سازد.

منابع meta:

- API جدید: `/rest/api/2/issue/createmeta/{project}/issuetypes/{typeId}`
- در صورت نیاز، پارس JSON خام (برای از دست نرفتن `allowedValues`)
- createmeta کلاسیک به‌عنوان fallback

### 7.5 انواع ویجت فیلد در فرم

| نوع schema / تشخیص | کنترل UI | ارسال به API |
|--------------------|----------|--------------|
| string / summary | EditText | رشته |
| option / priority با allowedValues | Spinner | `{ "id": "..." }` |
| array option/component | چندانتخابی | لیست object |
| user / assignee | جستجوی کاربر | `{ "name": "..." }` |
| date | DatePicker → `yyyy-MM-dd` | تاریخ |
| datetime / فیلدهای رویداد | DatePicker → ISO با ساعت | مثلاً `2026-09-23T00:00:00.000+03:30` |
| ScriptRunner DB Picker | جستجو از API مخصوص | id انتخاب‌شده |

### 7.6 ScriptRunner Database Picker

فیلدهایی مثل ActivityType / BudgetType / DemisCustomer در createmeta معمولی `allowedValues` ندارند. گزینه‌ها از:

```text
POST /rest/scriptrunner-jira/latest/generic-picker/search
     ?fcsId=...&pid=...&issueTypeId=...&inputValue=...
```

می‌آیند. نگاشت fcsId شرکت در `DemiscoScriptRunnerFields` است.

**مهم:** این فیلدها فقط وقتی در createmeta **همان نوع Issue** باشند نشان داده می‌شوند — برای Event به‌زور اضافه نمی‌شوند.

### 7.7 Worklog و سینک

JQL نمونه:

```text
worklogAuthor = currentUser() AND worklogDate >= "..." AND worklogDate <= "..."
```

سپس برای هر Issue، `getWorklogs` زده می‌شود و فقط ردیف‌های همان بازه و همان نویسنده در Room ذخیره می‌شوند.

نمایش:

- **تقویم / گزارش:** فقط Worklog خود کاربر  
- **کارت Issue در تسک‌ها:** همه Worklogهای آن Issue  

### 7.8 تاریخ شروع/پایان رویداد

- `customfield_10815` → شروع (ساعت 00:00)  
- `customfield_10816` → پایان (ساعت 23:59)  
فرمت باید datetime باشد؛ وگرنه سرور خطای `Error parsing time` می‌دهد.

---

## 8. ابزارها و Utilityها

| فایل | کار |
|------|-----|
| `TimeUtils.kt` | امروز، اختلاف دقیقه، تبدیل شمسی/میلادی نمایشی |
| `TimeCalc.kt` | محاسبه کارکرد، اضافه‌کار، مرخصی با قوانین تنظیمات |
| `ThemeHelper.kt` | رنگ متن/کارت/دکمه بر اساس تم |
| `DialogHelper.kt` | دیالوگ یک‌شکل با هدر و دکمه |
| `JalaliDatePickerDialog.kt` | انتخاب تاریخ شمسی برای UI |
| `BiometricHelper.kt` | اثرانگشت / قفل بیومتریک |
| `BackupHelper.kt` | خروجی/ورود بکاپ |
| `AutoBackupWorker.kt` | بکاپ دوره‌ای با WorkManager |
| `NotifHelper.kt` | یادآوری و چک پس‌زمینه موقعیت |
| `GeoHelper.kt` | ورود/خروج خودکار بر اساس مکان |
| `DynamicAppIcon.kt` | تغییر آیکون لانچر بر اساس ساعت کار |
| `BootReceiver.kt` | بعد از روشن شدن گوشی، زمان‌بندی مجدد |

---

## 9. ویجت و کارهای پس‌زمینه

- `WorkWidgetProvider` / `IconHoursWidgetProvider`: ویجت صفحه اصلی برای میانبر تردد یا نمایش ساعت.
- `WorkManager`: کارهای دوره‌ای (بکاپ، چک geo) حتی وقتی اپ باز نیست.
- مجوزهای مرتبط در `AndroidManifest.xml` تعریف شده‌اند (اینترنت، لوکیشن، نوتیفیکیشن، بوت).

---

## 10. تنظیمات بیلد و نسخه ریلیز

### 10.1 وابستگی‌های کلیدی (`app/build.gradle.kts`)

- AndroidX (AppCompat, Material, Fragment, Lifecycle)
- Room + KSP
- Retrofit + Gson + OkHttp
- Coroutines
- WorkManager، Biometric

### 10.2 انواع بیلد

| نوع | minify | debuggable | کاربرد |
|-----|--------|------------|--------|
| debug | خیر | بله | توسعه روزانه |
| release | بله (R8) | خیر | دادن به همکاران |

### 10.3 امضای ریلیز

1. ساخت keystore (یک‌بار)  
2. فایل `keystore.properties` در ریشه (از روی `.example`)  
3. `gradlew assembleRelease`  
4. خروجی: `app/build/outputs/apk/release/app-release.apk`

بدون keystore معتبر، ممکن است بیلد release با کلید debug امضا شود — برای توزیع داخلی بهتر است حتماً keystore اختصاصی بگذارید.

### 10.4 ProGuard

در `proguard-rules.pro` کلاس‌های مدل، Room، Retrofit و Workerها keep می‌شوند تا بعد از minify اپ نشکند.

---

## 11. چطور یک فیچر جدید اضافه کنیم؟

مثال فرضی: «افزودن برچسب رنگی به Issue در لیست»

### مرحله ۱ — داده (اگر لازم است)

- فیلد جدید در Entity  
- `version` دیتابیس + Migration  
- متد DAO + متد Repository  

### مرحله ۲ — شبکه (اگر از جیرا می‌آید)

- فیلد در `JiraModels` / `fields` پارامتر search  
- مپ کردن در `JiraService` به Entity  

### مرحله ۳ — UI

- در `TasksFragment` هنگام ساخت ردیف لیست، View جدید اضافه کنید.  
- استایل را از `ThemeHelper` بگیرید تا تم تیره/روشن نشکند.

### مرحله ۴ — تست دستی

1. نصب debug روی گوشی  
2. سناریو با پروژه واقعی شرکت (مثلاً DHMS / Task / Event)  
3. حالت آفلاین و خطای توکن را هم چک کنید  

### قوانین PR ذهنی

- منطق بیزنس در Repository بماند، نه در Fragment.  
- دیالوگ‌های شبکه: تا موفقیت `dismiss` نکنید.  
- پیام خطا برای کاربر فارسی و قابل‌اقدام باشد (`JiraClient.parseError`).  

---

## 12. نکات مهم هنگام توسعه

1. **Thread:** روی Main Thread شبکه نزنید؛ از `lifecycleScope` + `suspend` استفاده کنید.  
2. **Null safety:** در Kotlin به‌جای NPE، با `?.` و `?:` کار کنید.  
3. **توکن:** در لاگ OkHttp سطح BODY نگذارید تا توکن لو نرود (الان BASIC است).  
4. **تکراری Worklog:** همیشه با `remoteId` dedupe کنید.  
5. **فیلدهای createmeta:** فقط فیلدهای همان `issuetype` را نشان دهید؛ چیزی hardcode اجباری برای همه نوع‌ها نگذارید.  
6. **تاریخ جیرا:** بین `date` و `datetime` فرق بگذارید؛ datetime باید ISO با timezone باشد.  
7. **تغییر DB:** بدون Migration، کاربر با آپدیت اپ داده از دست می‌دهد.  
8. **RTL:** متن‌ها فارسی‌اند؛ layout از `supportsRtl` پشتیبانی می‌کند.  

---

## 13. نقشه راه بهبود

پیشنهادهای مرتب‌شده از «ارزش بالا / دردسر معقول»:

### کوتاه‌مدت

- [ ] صفحه تمام‌صفحه برای ایجاد Issue به‌جای دیالوگ (فرم‌های شلوغ راحت‌تر می‌شوند)  
- [ ] کش کردن createmeta هر پروژه/نوع برای سرعت  
- [ ] تست واحد برای `TimeCalc` و dedupe Worklog  
- [ ] نمایش واضح‌تر وضعیت سینک (آخرین زمان همگام‌سازی)  

### میان‌مدت

- [ ] ViewModel به‌ازای هر Fragment (جداسازی بهتر UI از منطق)  
- [ ] Navigation Component به‌جای تعویض دستی Fragment  
- [ ] پشتیبانی بهتر از فیلدهای ScriptRunner دیگر بدون hardcode  
- [ ] خروجی گزارش Excel/CSV  

### بلندمدت

- [ ] همگام‌سازی دوطرفه Conflict resolution برای Worklog آفلاین  
- [ ] ماژول‌ار کردن Gradle (core / jira / ui)  
- [ ] CI برای بیلد ریلیز امضاشده  

---

## پیوست الف — مسیر خواندن کد برای تازه‌وارد

اگر می‌خواهید کد را از صفر بخوانید، این ترتیب پیشنهاد می‌شود:

1. `App.kt`  
2. `ui/MainActivity.kt`  
3. `data/db/AppDatabase.kt` + یک Entity و یک Dao  
4. ابتدای `AppRepository` (تنظیمات و checkIn/checkOut)  
5. `jira/JiraClient.kt` → `JiraApi.kt` → `JiraService.kt`  
6. `ui/tasks/TasksFragment.kt` (بخش ایجاد Issue و لیست)  
7. `jira/JiraIssueFormHelper.kt`  
8. `ui/calendar/CalendarFragment.kt` و `ui/reports/ReportsFragment.kt`  

---

## پیوست ب — دستورات روزمره

```text
# اجرای نسخه debug روی دستگاه
gradlew :app:installDebug

# بیلد APK ریلیز (با keystore.properties)
gradlew :app:assembleRelease

# مسیر خروجی ریلیز
app/build/outputs/apk/release/app-release.apk
```

در Android Studio: اجرای ▶ روی `app` همان installDebug است.

---

## پیوست ج — واژه‌نامه کوتاه

| واژه | معنی در این پروژه |
|------|-------------------|
| Issue | یک کار/تیکت در جیرا (مثلاً DHMS-123) |
| Worklog | ثبت زمان صرف‌شده روی یک Issue |
| createmeta | متادیتای فیلدهای مجاز هنگام ساخت Issue |
| PAT | Personal Access Token برای API جیرا |
| Room | دیتابیس محلی SQLite با ORM |
| Fragment | یک صفحه داخل Activity |
| Coroutine | مدل async سبک Kotlin |
| R8 / ProGuard | فشرده‌سازی و ابهام‌سازی کد ریلیز |

---

*این سند بر اساس ساختار فعلی پروژه Personal Time Tracker نوشته شده و برای آموزش توسعه‌دهنده جاوا‌زبان بدون پیش‌زمینه اندروید تنظیم شده است.*
