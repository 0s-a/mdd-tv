# تطبيق مدد TV (mdd TV) — Jellyfin Native Client

مشروع تطبيق أندرويد مستقل (Native Android) مبني بأحدث التقنيات الرسمية:
* **لغة البرمجة:** Kotlin 2.0+
* **واجهات المستخدم:** Jetpack Compose + Material 3
* **محرك الوسائط والفيديو:** AndroidX Media3 (ExoPlayer)
* **معمارية المشروع:** Clean Architecture + MVI

---

## 📚 وثائق التطبيق المستقلة (App Documentation)

جميع مواصفات وتصاميم ومعمارية التطبيق متوفرة ومحدثة داخل:  
👉 [`docs/README.md`](./docs/README.md)

| الوثيقة | المسار | الوصف |
| :--- | :--- | :--- |
| **المواصفات الفنية الشاملة** | [`docs/SPEC.md`](./docs/SPEC.md) | رؤية المنتج ومعايير القبول ونطاق الشاشات. |
| **نظام التصميم السينمائي** | [`docs/DESIGN_SYSTEM.md`](./docs/DESIGN_SYSTEM.md) | رموز التصميم وألوان الأوبسيديان وشارات Infuse. |
| **المعمارية التقنية** | [`docs/ARCHITECTURE_AND_TECH_STACK.md`](./docs/ARCHITECTURE_AND_TECH_STACK.md) | معمارية MVI ومحرك Media3 وتدفق البيانات. |
| **المتطلبات غير الوظيفية** | [`docs/NON_FUNCTIONAL_REQUIREMENTS.md`](./docs/NON_FUNCTIONAL_REQUIREMENTS.md) | معايير الأداء ومعدل 120Hz وكفاءة البطارية. |
| **مواصفات الشاشات الست** | [`docs/screens/`](./docs/screens/) | تفاصيل الواجهات والمخططات وحالات الخطأ. |
| **تطوير الإصدار الثاني** | [`docs/v2/`](./docs/v2/) | خطة العمل والملف الشخصي المحلي. |

---

## 📁 هيكلية المجلد المستقل (`mdd_tv/`)

يحتوي هذا المجلد على مشروع الأندرويد المستقل بالكامل، بحيث يمكن فتحه مباشرة في **Android Studio** كنقطة بداية:

```text
mdd_tv/
├── docs/                               # التوثيق الكامل للتطبيق (PRD، التصميم، المعمارية)
│   ├── SPEC.md                         # المواصفات الفنية الشاملة
│   ├── DESIGN_SYSTEM.md                # نظام التصميم والألوان السينمائية
│   ├── ARCHITECTURE_AND_TECH_STACK.md  # معمارية Clean + MVI + Media3
│   ├── NON_FUNCTIONAL_REQUIREMENTS.md  # متطلبات الأداء والـ Direct Play
│   ├── screens/                        # تفاصيل ومخططات الشاشات
│   ├── v2/                             # خطة وتطوير الإصدار الثاني
│   └── ui_references/                  # مراجع وتجارب الواجهات المرئية
├── tasks/                              # خطط التنفيذ وقوائم المهام للتطبيق
│   ├── plan.md                         # خطة التنفيذ الشاملة للشاشات والمراحل
│   └── todo.md                         # قائمة متابعة الإنجاز والمهام
├── tools/                              # أدوات الفحص التابعة للمشروع
│   └── server_test_suite/              # فحص اتصال السيرفر الحي
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml          # إعدادات التطبيق والأذونات (مدد TV)
│   │   ├── kotlin/org/jellyfin/mobile/
│   │   │   ├── AppConfig.kt            # كائن التهيئة المركزي (السيرفر + الهوية)
│   │   │   ├── JellyfinApp.kt          # فئة التطبيق الرئيسية (Application)
│   │   │   └── ui/theme/               # ثيم التطبيق والألوان السينمائية
│   │   └── res/
│   │       ├── values/strings.xml      # اسم التطبيق بالعربية: "مدد TV"
│   │       └── values-en/strings.xml   # اسم التطبيق بالإنجليزية: "mdd TV"
│   └── build.gradle.kts                # تبعيات وإعدادات موديول التطبيق
├── gradle/
│   ├── wrapper/                        # مشغل Gradle Wrapper
│   └── libs.versions.toml              # كتالوج الإصدارات الموحد (Version Catalog)
├── build.gradle.kts                    # إعدادات البناء الجذرية
└── settings.gradle.kts                 # تسجيل المشروع باسم: MadadTV
```

---

## 🏷️ تغيير أو تحديث اسم التطبيق (بشكل مؤقت أو دائم)

الاسم المركزي للتطبيق معزول في مكانين محددين لسهولة تعديله أو استبداله مستقبلاً بضغطة زر دون المساس بمنطق الأكواد:

1. **ملفات النصوص (Resource Strings):**
   * العربية: `app/src/main/res/values/strings.xml` (`<string name="app_name">مدد TV</string>`)
   * الإنجليزية: `app/src/main/res/values-en/strings.xml` (`<string name="app_name">mdd TV</string>`)

2. **كائن التهيئة البرمجي:**
   * الملف: `app/src/main/kotlin/org/jellyfin/mobile/AppConfig.kt`
   * المتغير: `const val APP_NAME = "مدد TV"`
