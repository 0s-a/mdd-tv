# خطة العمل المحسنة: تطبيق نظام التصميم الراقي، استبدال التنزيل بالشخصي، وتوحيد الهيدر
# Optimized Active Task: Refined Cinema Tokens, Profile Tab & Unified TopBar

> **الأهداف الاستراتيجية:**
> 1. ترقية نظام التصميم إلى دستور Obsidian 2.2.0 (ألوان الكربون، الحواف عالية التباين، وإلغاء هالات النيون والزجاج المصطنع).
> 2. استئصال ميزة التنزيل بالكامل دون كسر الكود المصدري عبر فك الارتباط المنظم (Decoupling).
> 3. اعتماد شريط التنقل السفلي الثلاثي (الرئيسية • المكتبة • الشخصي) وجعل البروفايل وجهة عليا مستقرة.
> 4. استبدال الهيدر العائم بهيدر سينمائي موحد وثابت مدمج في سواد OLED (`CinemaTopBar`).

---

## مراحل التنفيذ المحسنة (Optimized 5-Phase Execution Plan)

### المرحلة 1: فك ارتباط التنزيلات وتنظيف منطق المشغل والتفاصيل (Decoupling)
- [x] تعديل `PlayerViewModel.kt`: إزالة الاعتماد على `DownloadManagerProvider` والاعتماد حصراً على التشغيل المباشر عبر السيرفر (Direct Play).
- [x] تعديل `MediaDetailsViewModel.kt`: إزالة عمليات الحفظ والتنزيل وحذف معامل `DownloadsRepository`.
- [x] تعديل `JellyfinApp.kt`: إزالة كود تهيئة مزود التنزيل `DownloadManagerProvider`.

### المرحلة 2: استئصال حزم وخدمات وجداول التنزيل نهائياً (Removal)
- [x] حذف حزمة واجهات التنزيل بالكامل `presentation/downloads/`.
- [x] حذف خدمات التنزيل الخلفية ومزوداتها `core/download/`.
- [x] إزالة خدمة `JellyfinDownloadService` من ملف `AndroidManifest.xml` وحذف تصاريح التنزيل الزائدة إن وجدت.
- [x] إزالة كيان `DownloadedMediaEntity` وواجهة `DownloadedMediaDao` ومحولات التنزيل من طبقتي Data و Domain.
- [x] تنظيف `AppDatabase.kt` بإزالة جدول التنزيل، وتنظيف تعريفات وحقن التنزيل في `AppModule.kt`.

### المرحلة 3: إعادة هيكلة شريط التنقل السفلي واعتماد تبويب "الشخصي" (Navigation Restructure)
- [x] تحديث تعداد `NavigationTab` في `presentation/main/BottomNavigationBar.kt` باستبدال التنزيل بـ `PROFILE` ("الشخصي") برمز `Icons.Rounded.Person` / `Icons.Outlined.Person`.
- [x] تحديث `presentation/main/MainScaffold.kt`:
  - إضافة `Screen.Profile.route` إلى الوجهات العليا (`isTopLevelDestination`) ليبقى الشريط السفلي ظاهراً عند استعراض الملف الشخصي والمفضلة.
  - إزالة مسار التنزيل من شروط العرض.
- [x] تحديث `presentation/navigation/NavGraph.kt`: إزالة مسار `downloads` وربط الضغط على تبويب الشخصي بشاشة `ProfileScreen`.

### المرحلة 4: تطبيق نظام التصميم السينمائي الراقي Obsidian 2.2.0
- [x] تحديث `ui/theme/Color.kt`:
  - إرساء كلاسات الرموز الثلاثية: `JellyfinPrimitives` (الأوبسيديان، الجرافيت، العاجي، والأمبر الدافئ)، `JellyfinStates` (نسب الشفافية التفاعلية)، و `JellyfinColors` الدلالية.
  - توفير جسر توافقي (Compatibility Aliases) لتفادي كسر الملفات القائمة أثناء الترحيل.
- [x] تحديث `ui/theme/Dimens.kt`:
  - اعتماد مقاييس التباعد الصارمة (4dp, 8dp, 10dp, 16dp).
  - إزالة كبسولات `RadiusPill = 999.dp` المفرطة واعتماد أنصاف أقطار رصينة ومريحة للعين.
- [x] تحديث `ui/theme/Theme.kt`: تثبيت السواد التام للشاشات OLED (`#000000`) والأسطح الجرافيتية المطفأة.
- [x] تحديث واستبدال `ui/components/GlassmorphicCard.kt` بمكونات كربونية مطفأة عالية التباين (`ObsidianCard`).

### المرحلة 5: بناء وتوحيد الهيدر السينمائي (Unified Cinema TopBar)
- [x] إنشاء مكون `CinemaTopBar.kt` داخل `presentation/core/components/`:
  - نمط رئيسي `CinemaTopBar.Root`: مخصص للشاشة الرئيسية (عنوان "مدد TV" بخط رصين يميناً، وأيقونة البحث يساراً، بدون كبسولة عائمة وبدون أفاتار).
  - نمط فرعي `CinemaTopBar.Child`: للشاشات الداخلية (زر رجوع موحد، عنوان الشاشة، وإجراءات مونوكرومية).
  - دمج كامل في السواد ومراعاة مسافات شريط الحالة `WindowInsets.statusBars`.
- [x] حذف `FloatingFrostedTopBar.kt` وتحديث `HomeScreen.kt` لاعتماد الهيدر الموحد.
- [x] توحيد الهيدر في `LibraryScreen.kt` و `ProfileScreen.kt` و `SearchScreen.kt`.

### المرحلة 6: التحقق، التدقيق المعماري، والتوثيق (Verification & Living Docs)
- [x] التحقق من تكامل الكود البرمجي وخلوه من أي مراجع تالفة أو أخطاء بناء (`assembleDebug` ناجح بالكامل).
- [x] تحديث وثيقة المواصفات الفنية `docs/current/spec.md` وشاشات التوثيق لتعكس الهيكل الجديد رسمياً.
- [x] تسجيل التعديلات في خزان `docs/UNRELEASED.md`.
- [ ] تثبيت التغييرات النظيفة في مستودع Git وفق معايير Conventional Commits.
