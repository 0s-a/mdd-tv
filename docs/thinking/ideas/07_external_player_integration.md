# منظومة دعم المشغل الخارجي ومزامنة البث
# External Player Integration & Local-First Progress Specification

> **المرجع والامتثال المعماري:**
> * يتوافق بنسبة 100% مع الدستور المعماري **[`ADR 0001: عزل البروفايل وتقدم المشاهدة محلياً عبر Room Database`](../../adr/0001_local_room_profiles_isolation.md)**.
> * خادم Jellyfin هو للقراءة وبث الوسائط فقط (Read-Only) نظراً لمشاركة حساب "الاستراحه" بين كافة الأجهزة، ولا يتم إرسال أي جلسات تقدم أو إيقاف للسيرفر لمنع تضارب المشاهدين.
> **الحالة:** فكرة وميزة مقترحة قيد التخطيط والدراسة (Planning Backlog).
> **النطاق:** تطبيق مدد TV لنظام أندرويد وأجهزة التلفزيون الذكية (Android TV).

---

## 1. الرؤية والهدف

إتاحة الحرية الكاملة للمستخدم في تشغيل الوسائط عبر مشغلات الفيديو الخارجية المتخصصة المثبتة على جهازه (مثل VLC, Just Player, MX Player, MPV, Nova Player) مع الحفاظ الصارم على معمارية العزل المحلي للبيانات (Local-First Architecture):

* تقديم حل فوري ونهائي لأي ملفات فيديو نادرة قد تواجه صعوبة في فك الترميز أو التوافق العتادي.
* تلبية تفضيلات المستخدمين المتقدمين للتحكم في الصوتيات والألوان عبر مشغلاتهم المفضلة.
* الحفاظ على استقلالية تقدم المشاهدة (Resume Point) وحفظ موضع التوقف محلياً داخل قاعدة بيانات التطبيق `Room DB` لكل بروفايل محلي دون المساس بحساب السيرفر المشترك "الاستراحه".

---

## 2. المعمارية العامة ومخطط تدفق البيانات المحلي

```mermaid
flowchart TD
    UserAction["طلب تشغيل بمشغل خارجي (زر التفاصيل / المشغل المدمج / الإعداد الافتراضي)"] --> ReadLocal["استرجاع نقطة الاستئناف المحلية من Room DB (LocalWatchProgressEntity)"]
    ReadLocal --> URLBuilder["محرر روابط البث (Jellyfin Direct Stream URL)"]
    URLBuilder --> IntentFactory["منشئ نوايا أندرويد (ExternalPlayerIntentFactory)"]
    
    IntentFactory -->|تضمين الرابط، العنوان، التوقيت المحلي، والترجمات| AndroidIntent["نية النظام Android Intent (ACTION_VIEW)"]
    
    AndroidIntent --> AppChooser{"تحديد المشغل"}
    AppChooser -->|مشغل محدد مسبقاً| TargetPlayer["المشغل المختار (VLC / MX Player / Just Player)"]
    AppChooser -->|غير محدد| SystemChooser["قائمة اختيار التطبيقات بنظام أندرويد"]
    SystemChooser --> TargetPlayer
    
    TargetPlayer -->|إغلاق المشغل الخارجي والعودة للتطبيق| ResultHandler["معالج النتيجة (ActivityResultLauncher)"]
    
    ResultHandler --> ParseResult{"فحص بيانات النتيجة (Intent Extras)"}
    ParseResult -->|موضع دقيق متاح (Position Extras)| SaveLocal["حفظ التقدم محلياً في Room DB (LocalProfileRepository)"]
    ParseResult -->|الموضع غير متوفر تلقائياً| TimeEstimator["تقدير الوقت المنقضي أو تأكيد المستخدم"]
    TimeEstimator --> SaveLocal
    
    SaveLocal --> CheckCompletion{"هل تجاوزت المشاهدة 90%؟"}
    CheckCompletion -->|نعم| Archive["أرشفة المادة كمشاهدة محلياً (WatchedArchiveEntity)"]
    CheckCompletion -->|لا| UpdateProgress["تحديث شريط متابعة المشاهدة المحلي (LocalWatchProgressEntity)"]
    
    Archive --> SyncUI["تحديث شاشات التطبيق محلياً (الرئيسية، التفاصيل، البروفايل)"]
    UpdateProgress --> SyncUI
```

---

## 3. عقد نية أندرويد وتكامل المشغلات (Android Intent Contract)

### أ. إعداد النية الأساسية (Base Intent Construction)
* **نوع الإجراء (Action):** `Intent.ACTION_VIEW`
* **المورد ونوع المحتوى (Data & MIME):**
  * `setDataAndType(streamUri, mimeType)` بتمرير رابط البث المباشر المرفق بتوكن الوصول `api_key`.
  * تحديد الـ MIME Type بدقة (مثل `video/*`، `video/mp4`، `video/x-matroska`، `application/x-mpegURL`).
* **أعلام النية (Flags):**
  * `Intent.FLAG_ACTIVITY_NEW_TASK` (للشاشات الكبيرة والتلفزيون).
  * `Intent.FLAG_GRANT_READ_URI_PERMISSION` لتمرير ملفات الترجمة المحلية عبر مزود المحتوى.

### ب. تمرير البيانات الوصفية وموضع الاستئناف المحلي
يتم استخراج موضع الاستئناف الأخير المخزن في جدول `LocalWatchProgressEntity` للبروفايل المحلي النشط وتمريره ضمن الحزمة:

| المعامل (Extra Key) | النوع البرمجي | الوظيفة والمصدر | المشغلات المتوافقة |
| :--- | :--- | :--- | :--- |
| `position` | `Int / Long` | نقطة الاستئناف بالمللي ثانية (مأخوذة من `LocalWatchProgressEntity`) | MX Player, Just Player, MPV |
| `title` | `String` | اسم الفيلم أو الحلقة من بيانات المادة | MX Player, VLC, Just Player, MPV |
| `android.intent.extra.TITLE` | `String` | الترويسة القياسية لنظام أندرويد لعنوان الفيديو | مشغلات أندرويد العامة |
| `return_result` | `Boolean = true` | إلزام المشغل الخارجي بإرجاع النتيجة عند إغلاقه | MX Player, Just Player |
| `subs` | `ParcelableArray (Uri[])` | روابط ومسارات ملفات الترجمة المرافقة | MX Player |
| `subs.name` | `String[]` | أسماء ولغات مسارات الترجمة | MX Player |
| `subs.enable` | `Parcelable (Uri)` | المسار الموصى بتفعيله افتراضياً للترجمة | MX Player |
| `headers` | `Array<String>` | ترويسات التوثيق الإضافية للاتصال بالسيرفر | VLC, MX Player |

---

## 4. استرداد موضع التوقف وتخزينه محلياً (Local-First Persistence via Room DB)

تطبيقاً لميثاق **ADR 0001**، يُحظر تماماً إرسال تقارير جلسات التشغيل أو استدعاءات `/Sessions/Playing` و `/Sessions/Playing/Stopped` إلى خادم Jellyfin، ويتم حصر كافة عمليات التتبع محلياً عبر مستودع `LocalProfileRepository`:

### المسار الأول: الاسترداد المباشر عبر نتيجة النوايا (Direct Intent Result Extraction)
عند انتهاء المشغل الخارجي والعودة لتطبيق مدد TV عبر `ActivityResultLauncher`:
1. فحص الحزمة المرتجعة واستخراج موضع التوقف:
   * **MX Player:** يرجع `position` (بالمللي ثانية)، و `duration`، وحالة الإنهاء `end_by`.
   * **Just Player:** يرجع `position` بالمللي ثانية في الحزمة المرتجعة.
2. حفظ الموضع مباشرة في جدول `LocalWatchProgressEntity` للبروفايل النشط:
   ```kotlin
   localProfileRepository.saveWatchProgress(
       itemId = mediaId,
       title = mediaTitle,
       positionTicks = positionMs * 10_000L,
       durationTicks = durationMs * 10_000L,
       isPlayed = (durationMs > 0 && positionMs.toDouble() / durationMs >= 0.90)
   )
   ```
3. في حال تجاوز نسبة 90%، يتم تلقائياً:
   * إضافة المادة إلى جدول الأرشيف المحلي `WatchedArchiveEntity`.
   * تمييزها كمشاهدة محلياً عبر `localProfileRepository.markWatched(mediaId)`.

### المسار الثاني: التقدير التكيفي والتأكيد اللطيف (Fallback Time Estimation)
في حال استخدام مشغل لا يرجع موضع التوقف في نتيجة النية (مثل VLC):
1. يسجل التطبيق لحظة إطلاق المشغل الخارجي عبر `SystemClock.elapsedRealtime()`.
2. عند عودة المستخدم للتطبيق، يتم احتساب الفارق الزمني المنقضي (`elapsedDuration`).
3. احتساب الموضع المقدر: `estimatedPosition = startPosition + elapsedDuration`.
4. عرض شريط إشعاري خفيف (Snackbar) بأسفل الشاشة لتأكيد الحفظ المحلي:
   * *"تم إيقاف المشاهدة عند [التوقيت المقدر]. حفظ نقطة التوقف محلياً؟"* مع زري: `حفظ` و `تجاهل`.
   * عند الضغط على `حفظ`، تُخزن النقطة في `LocalWatchProgressEntity` دون أي اتصال بالسيرفر.

---

## 5. مواضع التكامل في واجهة المستخدم (UI / UX Touchpoints)

1. **شاشة تفاصيل المادة (Media Details Screen):**
   * زر إجراء إضافي بجانب زر التشغيل: أيقونة شاشة لانطلاق المشغل الخارجي.
   * خيار صريح في القائمة الثلاثية: *"تشغيل عبر مشغل خارجي..."*.
2. **شاشة الإعدادات العامة (Settings Screen):**
   * قسم: **مشغل الوسائط (Media Player)**.
   * مفتاح تفعيل: **استخدام مشغل خارجي دائماً (Always use external player)**.
   * قائمة اختيار: **المشغل المفضل (Preferred Player)** (سؤال دائم / VLC / MX Player / Just Player).
3. **التحويل اللحظي من المشغل المدمج (Seamless In-Player Handoff):**
   * خيار بالقائمة الجانبية للمشغل الداخلي: *"متابعة المشاهدة في مشغل خارجي"*.
   * يقرأ المشغل المدمج الثانية الحالية، يوقف محرك ExoPlayer، ويطلق المشغل الخارجي بنفس التوقيت دون أي فقدان للتقدم.
