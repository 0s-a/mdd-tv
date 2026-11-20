# مركز توثيق مشروع مدد TV
# Documentation Hub - mdd TV

مرجع التوثيق الهندسي والمعماري لتطبيق "مدد TV" لنظام أندرويد.

---

## 1. أساسات التطبيق (`docs/foundations/`)
المرجعيات والقواعد الثابتة التي تحكم هوية وبنية التطبيق:
* [`architecture.md`](./foundations/architecture.md): المعمارية البرمجية (`Clean Architecture + MVI`) وتوزيع الحزم.
* [`design_system.md`](./foundations/design_system.md): فلسفة التصميم، السواد التام لشاشات OLED (`#000000`)، ومكافحة نمط الـ AI.
* [`non_functional_requirements.md`](./foundations/non_functional_requirements.md): مؤشرات الأداء، كفاءة استهلاك البطارية والذاكرة، والتوافق.

---

## 2. سجل القرارات المعمارية (`docs/adr/`)
سجل القرارات الفنية الكبرى لحفظ السياق التاريخي:
* [`0000_template.md`](./adr/0000_template.md): القالب الموحد لتوثيق القرارات.
* [`0001_local_room_profiles_isolation.md`](./adr/0001_local_room_profiles_isolation.md): عزل البروفايل محلياً عبر Room DB لحماية حساب السيرفر المشترك.
* [`0002_media3_hardware_direct_play.md`](./adr/0002_media3_hardware_direct_play.md): التشغيل العتادي المباشر Direct Play عبر Media3.
* [`0003_clean_architecture_mvi.md`](./adr/0003_clean_architecture_mvi.md): تطبيق Clean Architecture مع نمط MVI أحادي الاتجاه.
* [`0004_oled_true_black_design_system.md`](./adr/0004_oled_true_black_design_system.md): السواد التام والإضاءة المحيطية الناعمة.

---

## 3. واقع التطبيق الحالي ومواصفات الشاشات (`docs/current/`)
المرجع المعتمد لما هو مفعل ومنجز حالياً في الإصدار المستقر:
* [`spec.md`](./current/spec.md): المواصفات الفنية الشاملة وتدفق المستخدم.
* **الشاشات (`docs/current/screens/`):**
  * `00_onboarding_and_profiles.md`: التهيئة واختيار البروفايل.
  * `01_home_dashboard.md`: الشاشة الرئيسية والبانر واستئناف المشاهدة.
  * `02_library_browser.md`: تصفح المكتبات والفرز الأبجدي السريع.
  * `03_media_details.md`: تفاصيل الأفلام والحلقات وشارات Infuse.
  * `04_advanced_player.md`: المشغل السينمائي، الإيماءات، والترجمة العربية.
  * `05_search_and_explore.md`: الاستعلام الفوري وشبكة التصنيفات.
  * `06_downloads_offline.md`: إدارة التنزيلات والخزنة المحلية دون إنترنت.

---

## 4. مسار الإصدارات وتراكم التعديلات
* [`UNRELEASED.md`](./UNRELEASED.md): الخزان المؤقت لتسجيل التعديلات الجديدة قبل كل إصدار.
* [`CHANGELOG.md`](../CHANGELOG.md): السجل الدائم للإصدارات المنشورة.
