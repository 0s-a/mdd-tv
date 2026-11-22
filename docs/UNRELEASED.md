# التعديلات المرحلية قيد التطوير (Unreleased Staging Delta)
# Working Delta Buffer for Next Release

> هذا الملف هو الخزان الانتقالي المؤقت؛ يسجل فيه الوكيل والمطور كل ميزة جديدة أو إصلاح أثناء دورة التطوير، ويتم تفريغه تلقائياً إلى التوثيق المستقر وسجل التغييرات عند إطلاق كل إصدار رسمي.

---

## الميزات الجديدة قيد الإعداد (Added)
* **دعم فصول الوسائط السينمائية (Media Chapters Navigation):**
  * استخراج فصول الوسائط وعناوينها من كائنات خادم Jellyfin عبر `ChapterInfoDto` وربطها مع `BaseItemDto`.
  * تحويل الفصول وتمريرها لحظياً إلى `PlayerViewModel` وتحديث حالة الفصل الحالي `currentChapter` متزامناً مع موضع التشغيل والتقديم السريع.
  * رسم فواصل الفصول الدقيقة كعلامات بيضاء نقية على شريط التقديم `CinemaScrubber` وعرض اسم الفصل في كارت عائم أثناء السحب اللمسي.
  * دمج اسم الفصل الحالي في السطر التعريفي العلوي للـ HUD بجانب عنوان العمل.
* **طابور العمليات دون اتصال والمزامنة الذكية (Offline Action Queue & Resilient State Sync):**
  * كيان قاعدة البيانات `OfflineActionEntity` وطابور الأحداث في Room DB مع مؤشرات الفرز السريع O(1).
  * واجهة `OfflineActionDao` واستعلامات دمج الأحداث الذكية (Event Compaction) التي تحذف الإجراءات المتضاربة والقديمة تلقائياً قبل الإرسال لمنع تضخم الجدول.
  * مستودع `OfflineActionRepository` وتطبيقه `OfflineActionRepositoryImpl` لإدارة تسجيل الأحداث وتفريغ الطابور وفق نمط FIFO.
  * منسق المزامنة التلقائي `OfflineSyncCoordinator` للاستماع لحالة الشبكة عبر `ConnectivityManager` وتفريغ الطابور فور عودة اتصال الإنترنت تلقائياً.
  * تمكين الاستجابة الفورية 0ms لعمليات المفضلة والمشاهدة حتى في وضع الطيران دون أي رسائل خطأ، مع حفظها محلياً ومزامنتها لاحقاً وفق مبدأ ADR 0001.

---

## التحسينات والتعديلات (Changed)
* **إعادة هيكلة المشغل السينمائي والتخلص من ازدحام الكبسولات (Streamlined Minimal Cinema HUD):**
  * إزالة شريط الكبسولات العائم من أسفل شاشة المشغل وتفريغ الخط الزمني بالكامل لتجربة مشاهدة نقية.
  * توحيد كافة الإجراءات المتقدمة في قائمة إجراءات موحدة رئيسية `PlayerActionsModal` تنبثق بسلاسة وتتفرع لنوافذ فرعية مركزة للصوت والترجمة والسرعة والمعادل الصوتي.
  * إضافة كارت المعاينة الصوري العائم `ThumbnailPreviewBox` وتكامله المباشر مع شريط التقديم `CinemaScrubber` وصور BIF عبر `TrickplayThumbnailManager`.
* **تحسين ترويسة تفاصيل الوسائط (AdaptiveBackdropHeader):**
  * دعم أيقونة التطبيق الأحادية العائمة في المنتصف مع خلفية سينمائية هادئة كبديل فوري في حال غياب الخلفية العريضة للمحتوى.
* ترقية مستودع `MediaDetailsRepositoryImpl` ليدمج طابور العمليات دون اتصال في عمليات `toggleFavorite` و `markAsWatched` مع فض التعارض الصامت.
* تسجيل `OfflineActionDao` و `OfflineActionRepository` و `OfflineSyncCoordinator` ضمن وحدات حقن التبعيات Koin (`AppModule.kt`).
* ربط تشغيل `OfflineSyncCoordinator` بدورة حياة التطبيق الأساسية في `JellyfinApp.kt`.

---

## الإصلاحات البرمجية (Fixed)
* إصلاح استدعاء جلب صور المعاينة `getThumbnail` كدالة معلقة غير حاجبة داخل `LaunchedEffect` في `PlayerHudOverlay` لمنع تعليق خيط الرسم الرئيسي.
* استئصال `runBlocking` من `SeriesViewModel` وتحويل استعلام Room DB إلى كوروتين غير حاجب `viewModelScope.launch`.
* إضافة معامل التوكن الاختياري لرابط البث المباشر `AppConfig.buildVideoStreamUrl` لمنع أخطاء 401 Unauthorized.
* إضافة عنصر `<queries>` لحزم `video/*` في `AndroidManifest.xml` لضمان توافق تشغيل النوايا مع Android 11+.

---

## التغييرات المعمارية أو المحذوفة (Deprecated / Removed)
* استئصال وحذف الملفات المهجورة للمشغل القديم: `ArabicSubtitleOverlay.kt` و `PlayerGestureLayer.kt`.
