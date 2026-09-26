# مرجع کامل کد و راهنمای توسعه — Personal Time Tracker

> مخاطب: برنامه‌نویس **جاوا** (اندروید تازه‌کار).  
> این سند **مرجع کلاس‌ها، فیلدها و متدها** به‌همراه **راهنمای توسعه و بهبود** است.  
> نسخه هم‌تراز با ساختار فعلی پروژه (Room version 14، ماژول Jira کامل).

---

## فهرست

1. [مفاهیم پایه برای برنامه‌نویس جاوا](#بخش-۱--مفاهیم-پایه)
2. [نقشه معماری و جریان داده](#بخش-۲--معماری)
3. [بسته ریشه و Application](#بخش-۳--appkt)
4. [لایه Entity (جداول)](#بخش-۴--entity)
5. [لایه DAO](#بخش-۵--dao)
6. [AppDatabase](#بخش-۶--appdatabase)
7. [AppRepository — مرجع متدها](#بخش-۷--apprepository)
8. [بسته jira — کلاینت و فرم](#بخش-۸--jira)
9. [بسته ui — صفحه‌ها](#بخش-۹--ui)
10. [بسته util](#بخش-۱۰--util)
11. [ویجت](#بخش-۱۱--widget)
12. [منابع res و Manifest](#بخش-۱۲--res-و-manifest)
13. [راهنمای توسعه عملی](#بخش-۱۳--توسعه-عملی)
14. [راهنمای بهبود و Refactor](#بخش-۱۴--بهبود)
15. [چک‌لیست باگ‌های رایج](#بخش-۱۵--چکلیست)
16. [واژه‌نامه](#بخش-۱۶--واژهنامه)

---

## بخش ۱ — مفاهیم پایه

| مفهوم اندروید | معادل ذهنی جاوا | نقش در این پروژه |
|---------------|-----------------|------------------|
| `Application` | مثل static context سراسری | `App` یک `AppRepository` نگه می‌دارد |
| `Activity` | یک پنجره | تقریباً فقط `MainActivity` |
| `Fragment` | یک صفحه داخل پنجره | هر تب یک Fragment |
| `Context` | دسترسی به منابع سیستم | برای Toast، DB، Preference |
| Room `@Entity` | JPA Entity | جدول SQLite |
| Room `@Dao` | Repository interface + SQL | کوئری‌ها |
| Retrofit interface | Feign / Retrofit جاوا | `JiraApi` |
| `suspend fun` | async بدون callback جهنمی | شبکه و DB |
| `Flow` | Reactive stream / Observable | مشاهده زنده لیست از DB |
| `lifecycleScope` | Executor وابسته به عمر صفحه | جلوگیری از leak |

**قانون UI:** کار شبکه/دیسک را روی Main Thread انجام ندهید. از `viewLifecycleOwner.lifecycleScope.launch` و متدهای `suspend` ریپازیتوری استفاده کنید.

---

## بخش ۲ — معماری

```
UI (Fragment)
   │  فقط نمایش + رویداد
   ▼
AppRepository
   │  منطق کسب‌وکار، سینک، گزارش
   ├──────────────┐
   ▼              ▼
Room (DAO)     JiraService → JiraApi → OkHttp
(SQLite)       (REST سرور شرکت)
```

- Fragment مستقیم به DAO یا Retrofit وصل **نشود**.
- توکن جیرا فقط از `SettingsEntity` خوانده می‌شود و در `JiraClient` به هدر Bearer می‌رود.

---

## بخش ۳ — App.kt

**مسیر:** `com.personal.timetracker.App`  
**نوع:** `class App : Application()`

| عضو / متد | توضیح |
|-----------|--------|
| `repository: AppRepository` | نمونه سراسری؛ بعد از `onCreate` آماده است |
| `onCreate()` | ساخت Repository، کانال نوتیفیکیشن، زمان‌بندی آیکون پویا، geo پس‌زمینه |

دسترسی از Fragment:

```kotlin
val repo = (requireActivity().application as App).repository
```

---

## بخش ۴ — Entity

همه در `data.entity`. هر کلاس = یک جدول (مگر خلافش گفته شود).

### 4.1 AttendanceEntity — تردد

| فیلد | نوع | معنی |
|------|-----|------|
| id | Long | کلید اصلی |
| date | String | yyyy-MM-dd |
| entryTime | String | HH:mm ورود |
| exitTime | String? | HH:mm خروج؛ null = هنوز فعال |
| duration | Int | دقایق کارکرد خالص |
| leaveDuration | Int | دقایق مرخصی محاسبه‌شده |
| overtimeDuration | Int | اضافه‌کار |
| status | String | مثلاً active / completed |

### 4.2 HolidayEntity — تعطیل

| فیلد | معنی |
|------|------|
| date | روز تعطیل |
| title | عنوان |

### 4.3 SettingsEntity — تنظیمات (تک‌ردیفی عملی)

شامل ساعت کاری، انعطاف، تم، بیومتریک، مختصات محل کار، قوانین پنج‌شنبه، بکاپ خودکار، و فیلدهای جیرا:

- `jiraEnabled`, `jiraBaseUrl`, `jiraToken`
- `jiraFilterStatuses`, `jiraFilterProjects`, `jiraProjectCatalog`

### 4.4 TaskEntity / TaskLogEntity — تسک محلی قدیمی

هنوز برای تایمر محلی و fallback گزارش استفاده می‌شود. فیلدهای مهم Task: `jiraNumber`, `requiredMinutes`, `remainingMinutes`, `isRunning`.

### 4.5 JiraIssueCacheEntity — کش Issue

| فیلد مهم | معنی |
|----------|------|
| issueKey | مثلاً DHMS-123 (PK منطقی) |
| summary / description | عنوان و شرح |
| projectKey / projectName | پروژه |
| statusName / statusCategory | وضعیت |
| requiredMinutes / remainingMinutes / timeSpentMinutes | از timetracking |
| isFavorite / isAssignedToMe | پرچم‌های لیست |
| cachedAt | زمان کش |

### 4.6 JiraWorklogCacheEntity — کش Worklog

| فیلد | معنی |
|------|------|
| localId | PK محلی |
| remoteId | شناسه سرور؛ unique؛ null تا سینک |
| issueKey, date, started | کلید Issue و زمان |
| durationMinutes | مدت |
| authorName | نویسنده (برای فیلتر تقویم/گزارش) |
| syncStatus | synced / pending_add / pending_update / pending_delete |

### 4.7 JiraFavoriteEntity / JiraStatusEntity

علاقه‌مندی‌ها و کش وضعیت‌های workflow.

---

## بخش ۵ — DAO

اینترفیس‌های Room در `data.dao`. الگو:

- `observe*` → `Flow` برای UI زنده  
- `get*Once` / `suspend` → یک‌بار خواندن  
- `upsert` / `insert` / `update` / `delete`

### 5.1 AttendanceDao

| متد | کار |
|-----|-----|
| observeByDate / getByDateOnce | تردد یک روز |
| getByRange | بازه گزارش |
| getActive / observeActive | رکورد باز (بدون خروج) |
| insert / update / delete / deleteAll | CRUD |
| sumWorked / sumLeave / countInRange | جمع آماری |

### 5.2 HolidayDao

observeAll, getAllOnce, getInRange, countForDate, insert, insertAll, delete, deleteByDate

### 5.3 SettingsDao

معمولاً observe + upsert تک تنظیمات

### 5.4 TaskDao / TaskLogDao

لیست تسک، جستجو، خلاصه پروژه، لاگ بر اساس taskId یا تاریخ

### 5.5 JiraIssueDao

| متد | کار |
|-----|-----|
| observeAll / observeAssigned / observeFavorites / observeOpen | جریان‌های لیست |
| search(q) | جستجوی کلید/عنوان |
| getByKey / getAllOnce | خواندن یک‌بار |
| upsert / upsertAll | ذخیره کش |
| setFavorite | ستاره |
| delete / deleteByKey | حذف از کش |

### 5.6 JiraWorklogDao

| متد | کار |
|-----|-----|
| observeByIssue / getByIssueOnce | لاگ‌های یک Issue (همه نویسنده‌ها) |
| getByDateOnce / getByRange | برای تقویم و گزارش |
| getPending | صف آفلاین |
| getByRemoteId | جلوگیری از تکرار |
| upsert / update / delete | CRUD |
| deleteSyncedForIssue | پاک‌سازی قبل از رفرش |
| sumMinutesInRange | جمع خام (گزارش بهتر است از فیلتر کاربر استفاده کند) |
| dedupeByRemoteId | حذف تکراری SQL |

### 5.7 JiraFavoriteDao / JiraStatusDao

CRUD و observe استاندارد.

---

## بخش ۶ — AppDatabase

**مسیر:** `data.db.AppDatabase`  
**version فعلی:** 14

- لیست entities در annotation `@Database`
- abstract متد برای هر DAO
- Migrationها هنگام ارتقا نسخه؛ بدون Migration صحیح داده کاربر از بین می‌رود

---

## بخش ۷ — AppRepository

**مسیر:** `data.repository.AppRepository`  
**نقش:** تنها درگاه منطق برای UI.

### 7.1 تنظیمات و تردد

| متد | توضیح |
|-----|--------|
| observeSettings / getSettings / saveSettings | تنظیمات |
| observeActive / observeToday / observeAttendance | مشاهده تردد |
| checkIn / checkOut | ورود/خروج با محاسبه flex |
| addAttendance / updateAttendance / deleteAttendance | CRUD دستی |
| recalculateAllAttendance | محاسبه مجدد همه رکوردها |
| observeHolidays / همگام تعطیلات | از منبع آنلاین در صورت وجود |

### 7.2 تسک محلی

| متد | توضیح |
|-----|--------|
| observeTasks / searchTasks / getTask | لیست |
| saveTask / deleteTask | ذخیره |
| startTimer / stopTimer | تایمر؛ stop لاگ می‌سازد |
| addLog / updateLog / deleteLog | لاگ زمانی |
| recalculateTask | باقیمانده زمان |

### 7.3 جیرا — Issue و وضعیت

| متد | توضیح |
|-----|--------|
| jiraServiceOrNull() | اگر URL/توکن نباشد null |
| refreshJiraIssues | جستجو/بارگذاری Issueها در کش |
| refreshJiraStatuses | کش وضعیت‌ها |
| refreshJiraProjectsCatalog | کاتالوگ پروژه |
| observeJiraIssues / searchJiraIssues | UI لیست |
| getJiraProjectCatalog / saveJiraListFilters | فیلتر لیست |
| fetchJiraTransitions / transitionJiraIssue | تغییر وضعیت workflow |
| addJiraComment / getJiraComments | کامنت |

### 7.4 جیرا — Worklog

| متد | توضیح |
|-----|--------|
| syncWorklogsForDateRange / syncWorklogsForDate | JQL worklogAuthor=currentUser + ذخیره کش |
| getJiraWorklogsForDate | **فقط Worklog خود کاربر** برای تقویم |
| getJiraWorklogsOnce / observeJiraWorklogs | همه لاگ‌های یک Issue (کارت جزئیات) |
| addJiraTaskLog / updateJiraTaskLog / deleteJiraTaskLog | ثبت/ویرایش/حذف با صف pending |
| flushPendingJiraWorklogs | ارسال آفلاین‌ها |
| filterOwnWorklogs (خصوصی) | فیلتر author برای گزارش/تقویم |

### 7.5 گزارش

| متد | خروجی تقریبی |
|-----|----------------|
| report(start,end) | کارکرد، مرخصی، اضافه‌کار، کسری، دقایق لاگ |
| projectSummaryRange | جمع بر اساس پروژه |
| jiraSummaryRange | جمع بر اساس Issue (فقط خود کاربر) |
| dayBreakdown | جزئیات روزبه‌روز |

### 7.6 علاقه‌مندی

`toggleJiraFavorite` — روشن/خاموش ستاره؛ true اگر بعد از عمل favorite باشد.

---

## بخش ۸ — jira

### 8.1 JiraClient (object)

| متد | کار |
|-----|-----|
| create(baseUrl, token): JiraApi | Retrofit + OkHttp + Bearer |
| parseError(body): String | تبدیل JSON خطا به پیام فارسی قابل‌فهم |

### 8.2 JiraApi (interface Retrofit)

گروه endpointها:

- **کاربر/سیستم:** getStatuses, getProjects, myself, serverInfo  
- **جستجو/Issue:** search, getIssue  
- **Worklog:** add / get / update / delete  
- **Comment:** get / add  
- **Create/Update:** createIssue, createIssueRaw, updateIssue, deleteIssue  
- **Workflow:** getTransitions, doTransition  
- **Meta:** getCreateMeta, getProject, getCreateMetaIssueTypes, getCreateMetaFields (+ Raw)  
- **کاربر:** searchAssignableUsers (username), searchAssignableUsersQuery, searchUsers  
- **Issue picker:** issuePicker  
- **ScriptRunner:** scriptRunnerPickerSearch  

### 8.3 JiraModels

DTOهای Gson: `JiraUser`, `JiraIssue`, `JiraWorklog`, `JiraMetaField`, `JiraAllowedValue`, صفحات createmeta، `ScriptRunnerPickerItem`, `DemiscoScriptRunnerFields` (نگاشت fcsId شرکت).

### 8.4 JiraService

کلاس با baseUrl+token؛ متدهای سطح اپ:

| گروه | متدهای نمونه |
|------|----------------|
| هویت | myself |
| جستجو | search, searchJql, getIssue |
| Worklog | getWorklogs, addWorklog, updateWorklog, deleteWorklog |
| Issue | createIssue, createIssueWithFields, updateIssueFields, deleteIssue |
| Meta | fetchCreateMeta (فقط نوع‌ها), fetchFieldsForIssueType, fetchEditFields |
| کاربر | searchAssignableUsers |
| ScriptRunner | searchScriptRunnerPicker, resolveProjectId |
| زمان | companion toJiraStarted, nowIranIso, formatSeconds |

**ایجاد Issue:** همیشه `createIssueWithFields` با Map خام از فرم؛ خطا با `parseError` پرتاب می‌شود.

### 8.5 JiraIssueFormHelper

ساخت UI پویا از Map فیلدهای meta.

| مفهوم | معنی |
|--------|------|
| Kind | TEXT, NUMBER, SINGLE, MULTI, USER, DATE, ISSUE_LINK, TIME, SCRIPT_RUNNER |
| build(container, fields, existing) | رندر فرم؛ **بدون تزریق اجباری** فیلد ScriptRunner برای همه نوع‌ها |
| collect() | Map برای ارسال API؛ null اگر اعتبارسنجی شکست بخورد |
| dateFieldSortKey | ترتیب شروع قبل از پایان؛ 10815 شروع، 10816 پایان |
| DATE در collect | date خالص یا datetime با ISO تهران |

---

## بخش ۹ — ui

### 9.1 MainActivity

- بارگذاری تم از تنظیمات  
- bottom navigation → باز کردن Fragment  
- قفل بیومتریک اختیاری  
- سینک Worklog هفته در ورود  

### 9.2 TasksFragment (مرکز جیرا)

مسئولیت‌ها (متدهای مهم منطقی):

| حوزه | رفتار |
|------|--------|
| لیست | فیلتر پروژه/وضعیت، جستجو، صفحه‌بندی، ستاره |
| همگام‌سازی | refresh مسائل + flush pending + sync worklog بازه |
| جزئیات Issue | کامنت، لاگ، ویرایش، حذف، transition |
| ایجاد Issue | مرحله ۱ پروژه/نوع → مرحله ۲ فیلدها؛ دیالوگ با خطا **بسته نمی‌شود** |
| ویرایش | editmeta + همان FormHelper |

### 9.3 CalendarFragment

- تقویم شمسی ماه  
- loadDay: تردد + `syncWorklogsForDate` + `getJiraWorklogsForDate` (فقط خود کاربر)  

### 9.4 ReportsFragment

- انتخاب بازه  
- `report` / `jiraSummaryRange` / `dayBreakdown`  
- نمودار میله‌ای ساده  

### 9.5 AttendanceFragment / DashboardFragment / SettingsFragment

- تردد دستی و لیست  
- خلاصه داشبورد  
- فرم تنظیمات شامل بلوک جیرا و بکاپ  

### 9.6 JiraFragment

مسیر قدیمی‌تر؛ مسیر اصلی محصول **TasksFragment** است.

---

## بخش ۱۰ — util

### TimeUtils (object)

امروز، ساعت، اختلاف دقیقه، بازه تاریخ، تبدیل شمسی/میلادی نمایشی، شروع هفته/ماه، weekend، ارقام فارسی.

### TimeCalc (object)

| متد | کار |
|-----|-----|
| requiredMinutesForDate | دقیقه موظف با در نظر تعطیل و پنج‌شنبه |
| applyFlex | ورود با شناوری |
| suggestedEnd / earlyLeave / exitOutcome / midDayLeave | محاسبات خروج و مرخصی |

`FlexOutcome`: نتیجه اعمال شناوری روی ورود.

### ThemeHelper

رنگ متن، کارت، outline، دکمه؛ ساخت دکمه آیکون.

### DialogHelper

`show(...)`: onPositive → `true` بستن، `false` باز ماندن.  
هدر یک‌شکل، دکمه مثبت/منفی.

### سایر

| فایل | نقش |
|------|-----|
| BiometricHelper | احراز زیستی |
| BackupHelper / AutoBackupWorker | بکاپ |
| NotifHelper | یادآوری و geo دوره‌ای |
| GeoHelper | ورود/خروج مکانی |
| DynamicAppIcon | آیکون بر اساس ساعات |
| BootReceiver | بعد از بوت |
| Editors | ویرایشگرهای کمکی attendance |
| Charts / BarChartView | نمودار ساده |
| JalaliDatePickerDialog | انتخاب تاریخ شمسی |

---

## بخش ۱۱ — widget

| کلاس | نقش |
|------|-----|
| WorkWidgetProvider | ویجت میانبر کار/تردد؛ onReceive اکشن‌ها |
| IconHoursWidgetProvider | نمایش مرتبط با ساعت |

هر دو `requestUpdate` استاتیک برای تازه‌سازی از اپ دارند.

---

## بخش ۱۲ — res و Manifest

- `AndroidManifest.xml`: مجوز اینترنت، لوکیشن، نوتیف، بوت، بیومتریک؛ `App`؛ `MainActivity`؛ activity-alias برای آیکون‌های پویا؛ Provider ویجت  
- `res/layout`: activity_main، ویجت‌ها  
- `res/xml/network_security_config`: محدودیت cleartext  
- `values/themes`, `colors`, `strings`  

---

## بخش ۱۳ — توسعه عملی

### 13.1 محیط

1. Android Studio + JDK 17  
2. `local.properties` → `sdk.dir=...`  
3. باز کردن پوشه ریشه پروژه  
4. Run روی `app` (debug)

### 13.2 افزودن فیلد به جدول

1. فیلد در Entity  
2. `version` در AppDatabase + 1  
3. کلاس `Migration(from, to)` با `ALTER TABLE`  
4. ثبت Migration در builder دیتابیس  
5. متد Repository در صورت نیاز  
6. UI  

### 13.3 افزودن API جیرا

1. متد در `JiraApi`  
2. DTO در `JiraModels` اگر لازم  
3. متد در `JiraService` با `Result` و `parseError`  
4. متد در `AppRepository`  
5. صدا از Fragment با Coroutine  

### 13.4 افزودن فیلد به فرم ایجاد Issue

- ترجیحاً از createmeta بیاید (خودکار در FormHelper)  
- اگر ScriptRunner است: به `DemiscoScriptRunnerFields.known` اضافه کنید **فقط با fcsId درست**  
- اگر datetime است: در `collect` مثل 10815/10816 فرمت ISO بدهید  

### 13.5 قانون دیالوگ شبکه

```text
onPositive:
  اعتبارسنجی محلی → false اگر رد شد
  launch {
     API
     موفق → dismiss
     خطا → نشان دادن متن، دکمه را فعال کن
  }
  return false   // تا پایان شبکه بسته نشود
```

### 13.6 بیلد ریلیز برای همکاران

1. `ptt-release.jks` + `keystore.properties`  
2. `gradlew assembleRelease`  
3. APK از `app/build/outputs/apk/release/`  
4. نصب sideload؛ هشدار Play Protect برای اپ داخلی طبیعی است  

---

## بخش ۱۴ — بهبود

### اولویت بالا

1. **ViewModel** برای Tasks/Calendar/Reports — منطق از Fragment خارج شود  
2. **صفحه تمام‌صفحه ایجاد Issue** به‌جای دیالوگ بلند  
3. **تست واحد** برای TimeCalc، dedupe Worklog، parseError  
4. **کش createmeta** روی دیسک با TTL  

### اولویت متوسط

5. Navigation Component  
6. جدا کردن ماژول Gradle `:core` / `:jira` / `:app`  
7. خروجی CSV گزارش  
8. کشف پویای fcsId به‌جای hardcode (اگر API شرکت بدهد)  

### اولویت پایین‌تر / بدهی فنی

9. کاهش اندازه TasksFragment (شکستن به چند کلاس)  
10. یکسان‌سازی Task محلی و JiraIssue در یک مدل دامنه  
11. CI (بیلد + lint در GitHub Actions)  

### استاندارد کدی که حفظ کنید

- Repository تنها نقطه side-effect  
- پیام خطا فارسی و فیلد‌محور  
- Worklog تقویم ≠ همه نویسنده‌ها  
- فیلد اجباری فقط از meta همان issuetype  

---

## بخش ۱۵ — چک‌لیست باگ‌های رایج

| نشانه | علت محتمل | محل بررسی |
|--------|-----------|-----------|
| دیالوگ بسته می‌شود و فقط Toast خطا | return true قبل از API | TasksFragment onPositive |
| Error parsing time | datetime با فقط yyyy-MM-dd | JiraIssueFormHelper collect DATE |
| فیلد ScriptRunner روی Event | تزریق اجباری known fields | نباید برای نوع‌های بدون meta باشد |
| Worklog دیگران در تقویم | نبود filterOwnWorklogs | AppRepository getJiraWorklogsForDate |
| تکرار Worklog | remoteId خالی یا dedupe | JiraWorklogDao |
| createmeta Issue Does Not Exist | endpoint کلاسیک روی بعضی سرورها | مسیر issuetypes/{id} |
| Assignee خالی | پارامتر query به‌جای username | JiraApi searchAssignableUsers |
| کرش بعد از ریلیز | ProGuard مدل‌ها | proguard-rules.pro |
| از دست رفتن داده با آپدیت | Migration جا مانده | AppDatabase version |

---

## بخش ۱۶ — واژه‌نامه

| واژه | معنی |
|------|------|
| Issue | تیکت جیرا |
| Worklog | ثبت زمان روی Issue |
| createmeta | تعریف فیلدهای قابل‌پر کردن هنگام Create |
| editmeta | فیلدهای قابل‌ویرایش |
| PAT | Personal Access Token |
| fcsId | شناسه پیکربندی ScriptRunner picker |
| Room | ORM روی SQLite |
| Flow | جریان داده قابل‌جمع‌آوری |
| pending_* | وضعیت صف آفلاین Worklog |
| R8 | فشرده‌ساز ریلیز |

---

## پیوست — ترتیب مطالعه کد

1. App.kt  
2. MainActivity.kt  
3. Entityها + AppDatabase  
4. یک DAO نمونه (AttendanceDao)  
5. AppRepository از بالا تا checkIn/Out سپس بلوک Jira  
6. JiraClient → JiraApi → JiraService  
7. TasksFragment (ایجاد Issue + لیست)  
8. JiraIssueFormHelper  
9. CalendarFragment + ReportsFragment  
10. TimeUtils + TimeCalc  

---

## پیوست — دستورات

```text
gradlew :app:installDebug
gradlew :app:assembleRelease
```

خروجی ریلیز:

```text
app/build/outputs/apk/release/app-release.apk
```

---

*پایان مرجع کامل. این سند برای کار روزمره روی کد و توسعه/بهبود طراحی شده است.*


---

## پیوست تفصیلی — فهرست متدهای JiraService

| سطح | متد | پارامترها (خلاصه) |
|------|-----|-------------------|
| عمومی | `suspend myself` | `` |
| عمومی | `suspend testConnection` | `` |
| عمومی | `suspend fetchAssigned` | `openOnly: Boolean = true, maxResults: Int = 50, extraJql: St` |
| عمومی | `suspend search` | `query: String, maxResults: Int = 30` |
| عمومی | `suspend getIssue` | `issueKey: String` |
| خصوصی | `suspend searchJql` | `jql: String, maxResults: Int, startAt: Int = 0` |
| عمومی | `suspend fetchIssues` | `projectKeys: List<String> = emptyList(` |
| عمومی | `suspend fetchByProjects` | `projectKeys: List<String>, openOnly: Boolean = false, maxRes` |
| عمومی | `suspend getWorklogs` | `issueKey: String` |
| عمومی | `suspend addWorklog` | `issueKey: String, durationMinutes: Int, startedIso: String? ` |
| عمومی | `suspend updateWorklog` | `issueKey: String, worklogId: String, durationMinutes: Int?, ` |
| عمومی | `suspend deleteWorklog` | `issueKey: String, worklogId: String` |
| عمومی | `suspend getComments` | `issueKey: String` |
| عمومی | `suspend addComment` | `issueKey: String, body: String` |
| عمومی | `suspend createIssue` | `projectKey: String, summary: String, description: String? = ` |
| عمومی | `suspend createIssueWithFields` | `fields: Map<String, Any?>` |
| عمومی | `suspend fetchCreateMeta` | `projectKey: String` |
| عمومی | `suspend fetchFieldsForIssueType` | `projectKey: String, issueTypeId: String?, issueTypeName: Str` |
| خصوصی | `suspend parseCreateMetaFieldsRaw` | `projectKey: String, issueTypeId: String` |
| خصوصی | `suspend parseClassicCreateMeta` | `projectKey: String, issueTypeId: String?, issueTypeName: Str` |
| خصوصی | `parseMetaFieldObject` | `o: com.google.gson.JsonObject` |
| خصوصی | `suspend fetchFieldsForType` | `projectKey: String, issueTypeId: String` |
| خصوصی | `defaultCreateFields` | `` |
| عمومی | `suspend fetchEditMeta` | `issueKey: String` |
| عمومی | `suspend updateIssueFields` | `issueKey: String, fields: Map<String, Any?>` |
| عمومی | `suspend updateSummary` | `issueKey: String, summary: String` |
| عمومی | `suspend deleteIssue` | `issueKey: String, deleteSubtasks: Boolean = true` |
| عمومی | `suspend fetchStatuses` | `` |
| عمومی | `suspend fetchTransitions` | `issueKey: String` |
| عمومی | `suspend transitionIssue` | `issueKey: String, transitionId: String` |
| عمومی | `suspend fetchProjects` | `` |
| عمومی | `suspend searchAssignableUsers` | `projectKey: String, query: String` |
| عمومی | `suspend searchIssuesPicker` | `query: String, projectKey: String? = null` |
| عمومی | `suspend fetchEditFields` | `issueKey: String` |
| عمومی | `suspend searchScriptRunnerPicker` | `fcsId: String, projectId: String, issueTypeId: String, input` |
| خصوصی | `parseScriptRunnerPickerResponse` | `raw: String` |
| عمومی | `suspend resolveProjectId` | `projectKey: String` |
| عمومی | `fromSettings` | `s: SettingsEntity` |
| عمومی | `nowIranIso` | `` |
| عمومی | `toJiraStarted` | `dateIso: String, timeHHmm: String? = null` |
| عمومی | `mapStatus` | `category: String` |
| عمومی | `formatSeconds` | `sec: Int?` |

## پیوست تفصیلی — متدهای TasksFragment

| سطح | متد |
|------|-----|
| خصوصی | `primary` |
| خصوصی | `dark` |
| عمومی/override | `modeChip` |
| خصوصی | `bindList` |
| خصوصی | `issueCard` |
| خصوصی | `worklogRow` |
| خصوصی | `smallBtn` |
| خصوصی | `showAddByKeyDialog` |
| خصوصی | `showAddLog` |
| خصوصی | `showEditLog` |
| خصوصی | `suspend loadPage` |
| خصوصی | `updateStatusFilterButton` |
| خصوصی | `openStatusFilterDialog` |
| خصوصی | `updateProjectFilterButton` |
| خصوصی | `openProjectFilterDialog` |
| خصوصی | `openIssueDetail` |
| خصوصی | `showAddComment` |
| خصوصی | `showCreateIssueDialog` |
| عمومی/override | `suspend loadTypes` |
| خصوصی | `suspend showCreateIssueStep2` |
| خصوصی | `showEditIssueDialog` |
| خصوصی | `confirmDeleteIssue` |
| خصوصی | `fmt` |

### راهنمای نام‌گذاری متدهای TasksFragment

| پیشوند / الگو | معنی |
|---------------|------|
| show*Dialog | باز کردن دیالوگ UI |
| load* / bind* | بارگذاری یا اتصال داده به لیست |
| confirm* | تأیید قبل از حذف |
| open*Filter | فیلتر پروژه/وضعیت |

### نکات توسعه TasksFragment

- برای فیچر جدید لیست: از `bindList` و فیلترهای موجود پیروی کنید.
- برای API جدید: اول Repository، بعد فقط یک `show*` نازک در Fragment.
- از کپی کردن منطق createmeta داخل Fragment خودداری کنید؛ در `JiraIssueFormHelper` / `JiraService` بماند.
