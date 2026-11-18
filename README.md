# تطبيق مدد TV (mdd TV) — Jellyfin Native Client

مشروع تطبيق أندرويد مستقل (Native Android) مبني بأحدث التقنيات الرسمية:
* **لغة البرمجة:** Kotlin 2.0+
* **واجهات المستخدم:** Jetpack Compose + Material 3
* **محرك الوسائط والفيديو:** AndroidX Media3 (ExoPlayer)
* **معمارية المشروع:** Clean Architecture + MVI

---

## 📁 هيكلية المجلد المستقل (`mdd_tv/`)

يحتوي هذا المجلد على مشروع الأندرويد المستقل بالكامل، بحيث يمكن فتحه مباشرة في **Android Studio** كنقطة بداية:

```text
mdd_tv/
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
