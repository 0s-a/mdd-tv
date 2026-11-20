# تطبيق مدد TV (mdd TV) — Jellyfin Native Client

مشروع تطبيق أندرويد مستقل (Native Android) مبني بأحدث التقنيات الرسمية:
* **لغة البرمجة:** Kotlin 2.0+
* **واجهات المستخدم:** Jetpack Compose + Material 3
* **محرك الوسائط والفيديو:** AndroidX Media3 (ExoPlayer)
* **معمارية المشروع:** Clean Architecture + MVI

---

## وثائق التطبيق المستقلة (App Documentation)

جميع مواصفات وتصاميم ومعمارية التطبيق متوفرة ومحدثة داخل:  
* [`docs/README.md`](./docs/README.md)

| الوثيقة | المسار | الوصف |
| :--- | :--- | :--- |
| **المواصفات الفنية الشاملة** | [`docs/current/spec.md`](./docs/current/spec.md) | رؤية المنتج ومعايير القبول ونطاق الشاشات. |
| **نظام التصميم السينمائي** | [`docs/foundations/design_system.md`](./docs/foundations/design_system.md) | رموز التصميم وألوان الأوبسيديان وميثاق مكافحة نمط الذكاء الاصطناعي. |
| **المعمارية التقنية** | [`docs/foundations/architecture.md`](./docs/foundations/architecture.md) | معمارية MVI ومحرك Media3 وتدفق البيانات. |
| **المتطلبات غير الوظيفية** | [`docs/foundations/non_functional_requirements.md`](./docs/foundations/non_functional_requirements.md) | معايير الأداء ومعدل 120Hz وكفاءة البطارية. |
| **سجل القرارات المعمارية** | [`docs/adr/`](./docs/adr/) | قرارات العزل المحلي، التشغيل العتادي، وتأجيل التنزيلات. |
| **مواصفات الشاشات الحالية** | [`docs/current/screens/`](./docs/current/screens/) | تفاصيل الواجهات والمخططات وحالات الخطأ. |
| **حاضنة التفكير والتخطيط** | [`docs/thinking/`](./docs/thinking/) | مسار دراسة الأفكار الجديدة والخطط المعمارية المعتمدة. |

---

## هيكلية المجلد المستقل (`mdd_tv/`)

يحتوي هذا المجلد على مشروع الأندرويد المستقل بالكامل، بحيث يمكن فتحه مباشرة في Android Studio كنقطة بداية:

```text
mdd_tv/
├── docs/                               # التوثيق الكامل للتطبيق (PRD، التصميم، المعمارية، ADRs)
│   ├── current/                        # المواصفات الفنية المعتمدة والشاشات
│   ├── foundations/                    # ركائز المعمارية ونظام التصميم والأداء
│   ├── adr/                            # سجل القرارات المعمارية (ADRs)
│   └── thinking/                       # حاضنة الأفكار والخطط المعتمدة
├── tasks/                              # خطط التنفيذ وقوائم المهام الحالية (active_task.md)
├── tools/                              # أدوات الأتمتة والفحص التابعة للمشروع
│   ├── release.sh                      # محرك الإصدارات التلقائي
│   ├── verify_docs_integrity.py        # فحص سلامة التوثيق والحوكمة
│   ├── install_hooks.sh                # تثبيت خطافات Git للحوكمة
│   └── server_test_suite/              # فحص اتصال السيرفر الحي
├── app/                                # كود تطبيق الأندرويد
├── gradle/                             # مشغل ومكتبات Gradle Version Catalog
├── build.gradle.kts                    # إعدادات البناء الجذرية
└── settings.gradle.kts                 # تسجيل المشروع باسم: mdd-TV
```

---

## تغيير أو تحديث اسم التطبيق (بشكل مؤقت أو دائم)

الاسم المركزي للتطبيق معزول في مكانين محددين لسهولة تعديله أو استبداله مستقبلاً بضغطة زر دون المساس بمنطق الأكواد:

1. **ملفات النصوص (Resource Strings):**
   * العربية: `app/src/main/res/values/strings.xml` (`<string name="app_name">مدد TV</string>`)
   * الإنجليزية: `app/src/main/res/values-en/strings.xml` (`<string name="app_name">mdd TV</string>`)

2. **كائن التهيئة البرمجي:**
   * الملف: `app/src/main/kotlin/org/jellyfin/mobile/AppConfig.kt`
   * المتغير: `const val APP_NAME = "مدد TV"`
