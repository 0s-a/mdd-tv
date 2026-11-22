# مسار الأرشفة والذاكرة التاريخية
# Archival & Historical Knowledge Hub

مرجع الأرشفة الدائمة لحاضنة التفكير، والمستودع التاريخي للأفكار والميزات المنجزة أو المستبعدة في تطبيق "مدد TV".

---

## 1. فلسفة المسار وأهدافه

* **الحفاظ على رشاقة مسار التخطيط:** تفريغ مسار التخطيط (`planning/`) حصراً للميزات الوشيكة للتنفيذ، وتجنب تكدس الوثائق القديمة.
* **توثيق الدروس المستفادة (Post-Mortem):** تسجيل أسباب استبعاد الأفكار غير المجدية معمارياً أو تقنياً لمنع هدر الوقت في إعادة طرحها مستقبلاً.
* **حفظ السياق التاريخي للميزات المنفذة:** توفير مرجع هندسي متكامل يوضح كيف نشأت وتطورت كل ميزة قبل أن تدمج تفاصيلها النهائية في واقع التطبيق (`docs/current/`).

---

## 2. التقسيم الداخلي للمسار

```
docs/thinking/archive/
|-- README.md            # هذا الدليل: ضوابط وبروتوكول وفهرس الأرشفة
|-- completed/           # الميزات والخطط التي تم بناؤها واختبارها وإطلاقها بنجاح
`-- rejected/            # الأفكار والمقترحات المستبعدة مع بيان الأسباب المعمارية
```

---

## 3. سجل الخطط المنجزة المعتمدة (`completed/`)

| الملف المؤرشف | اسم الميزة | تاريخ الإنجاز | الإصدار المنجز | الكوميت المرتبط |
| :--- | :--- | :--- | :--- | :--- |
| [`05_trickplay_thumbnail_preview.md`](./completed/05_trickplay_thumbnail_preview.md) | المعاينة الصورية لشريط التقدم (Trickplay BIF) | 2026-11-22 | v1.1.0 | `93427ab` |
| [`07_external_player_integration.md`](./completed/07_external_player_integration.md) | منظومة دعم المشغل الخارجي وحفظ التقدم محلياً | 2026-11-22 | v1.1.0 | `3c88853` |
| [`08_metadata_and_genre_chips.md`](./completed/08_metadata_and_genre_chips.md) | نصوص البيانات الوصفية وكبسولات التصنيفات | 2026-11-21 | v1.1.0 | `fe33822` |
| [`11_offline_action_queue.md`](./completed/11_offline_action_queue.md) | طابور الأحداث دون اتصال مع فض التعارض | 2026-11-22 | v1.1.0 | `bfb0340` |
| [`12_dev_environment_automation_suite.md`](./completed/12_dev_environment_automation_suite.md) | أتمتة بيئة التطوير وخطافات Git وسلامة التوثيق | 2026-11-21 | v1.1.0 | `765094b` |
| [`13_docked_cinema_bottom_bar_and_navigation.md`](./completed/13_docked_cinema_bottom_bar_and_navigation.md) | شريط التنقل السفلي المدمج والمشغل المصغر | 2026-11-22 | v1.1.0 | `93427ab`, `549abcc` |

---

## 4. سجل الأفكار والمقترحات المستبعدة (`rejected/`)

| الملف المستبعد | اسم الفكرة | سبب الاستبعاد المعماري |
| :--- | :--- | :--- |
| [`idea_07_subtitles_gesture_jump_rejected.md`](./rejected/idea_07_subtitles_gesture_jump_rejected.md) | السحب المباشر على الترجمة للقفز | تضارب حركي مع إيماءات الشاشة وتشتيت بصري بحجب الترجمة باليد. |
| [`idea_13_ab_repeat_rejected.md`](./rejected/idea_13_ab_repeat_rejected.md) | تكرار المقطع السينمائي المستمر (A-B Repeat) | استبعاد بقرار المستخدم لعدم ملاءمتها لتطبيق ترفيهي سينمائي وازدحام الواجهة. |
| [`idea_14_smart_sleep_timer_rejected.md`](./rejected/idea_14_smart_sleep_timer_rejected.md) | مؤقت النوم الذكي مع الخفوت الصوتي | استبعاد بقرار المستخدم لتوفر الميزة في نظام أندرويد وتجنب حشو القوائم. |
| [`idea_16_tv_remote_acceleration_rejected.md`](./rejected/idea_16_tv_remote_acceleration_rejected.md) | القفز المتسارع لريموت التلفزيون | استبعاد شامل لكافة خصائص التلفزيون بعد إسقاط دعم Android TV في الإصدار 1.1.0. |
| [`10_auto_frame_rate_matching_tv_rejected.md`](./rejected/10_auto_frame_rate_matching_tv_rejected.md) | تبديل معدل التحديث HDMI للتلفزيون | انتفاء الحاجة بعد إسقاط دعم التلفزيون واقتصار التطبيق على الأجهزة اللمسية. |

---

## 5. بروتوكول الانتقال إلى الأرشفة (Archival Protocol)

### أولاً: أرشفة الميزات المكتملة (`archive/completed/`)
1. **شرط الانتقال:** اكتمال تنفيذ الميزة برمجياً، واجتياز اختبارات الجودة بنسبة 100%، ودمج مواصفاتها وواجهاتها النهائية في واقع التطبيق (`docs/current/`).
2. **إجراء الأرشفة:**
   * نقل وثيقة الخطة من `docs/thinking/planning/XX_feature_name.md` إلى `docs/thinking/archive/completed/XX_feature_name.md`.
   * إضافة ترويسة في بداية الوثيقة المؤرشفة توضح: تاريخ الإنجاز، رقم الإصدار الذي تضمنها (Release Tag)، ورابط الشاشة في `docs/current/screens/`.

### ثانياً: أرشفة الأفكار المستبعدة (`archive/rejected/`)
1. **شرط الانتقال:** صدور قرار معماري من الوكيل المخصص (`thinking-architect`) أو الإدارة الفنية باستبعاد الفكرة لتعارضها مع الدستور المعماري (مثل انتهاك العزل المحلي، أو عبء الذاكرة، أو ضعف الجدوى).
2. **إجراء الأرشفة:**
   * نقل وثيقة الفكرة من `docs/thinking/ideas/` إلى `docs/thinking/archive/rejected/`.
   * تدوين قسم إلزامي في نهاية الوثيقة بعنوان `أسباب الاستبعاد والدروس المستفادة (Rejection Rationale & Lessons Learned)` يتضمن تحليلاً معمارياً وافياً يمنع تكرار الطرح الخاطئ.
