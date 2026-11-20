# دراسة فكرة: الترقية إلى معالج KSP لدعم Kotlin 2.0+ وتسريع البناء
# Architectural Idea Study: KSP Compiler Modernization for Room Database

> **المعرف المقترح:** `IDEA-26`  
> **اسم الفكرة:** الترقية إلى معالج KSP لدعم Kotlin 2.0+ وتسريع البناء (KSP Modernization for Room DB)  
> **تاريخ الطرح:** 2026-11-20  
> **حالة الفكرة:** معتمدة ومطابقة معمارياً (Approved & Fully Compliant)  
> **الوكيل الفاحص:** `thinking-architect`  

---

## 1. التوصيف العام والدافع (Overview & Motivation)

### 1.1 ملخص الفكرة
ترحيل معالجة تعليقات قاعدة بيانات `Room DB` من إضافة `kapt` القديمة إلى تقنية **KSP (Kotlin Symbol Processing)** الحديثة المدعومة رسمياً من Google، وذلك عبر:
1. إزالة `id("kotlin-kapt")` من `app/build.gradle.kts`.
2. إضافة إضافة KSP الرسمية `alias(libs.plugins.ksp)` المتوافقة مع Kotlin 2.0.21.
3. استبدال `kapt(libs.androidx.room.compiler)` بـ `ksp(libs.androidx.room.compiler)`.

### 1.2 الدافع ومبررات الإضافة
* **القضاء على تنبيه التراجع اللغوي:** إزالة تحذير البناء المستمر:
  `w: Kapt currently doesn't support language version 2.0+. Falling back to 1.9.`
* **تسريع وتيرة البناء التزايدي (Build Speed):** يوفر KSP تسريعاً لتوليد كود DAOs و Entities بنسبة تتراوح بين 25% إلى 35% مقارنة بـ Kapt، نظراً لأنه يقرأ كود Kotlin مباشرة دون توليد أطراف جافا وسيطة (Java Stubs).
* **تقليل استهلاك الذاكرة العشوائية:** تقليص الحمل على بيئة JVM في لينكس مما يتوافق تماماً مع إعدادات `-Xmx2048m` المعتمدة للمشروع.

---

## 2. جدول مطابقة القرارات المعمارية (ADR Alignment Matrix)

| القرار المعماري | التوافق مع القرار | ملاحظات التدقيق والتحقق المعماري |
| :--- | :--- | :--- |
| **ADR 0001 (عزل البروفايل في Room DB)** | متوافق بنسبة 100% | يسرع توليد أكواد `UserProfileDao`, `LocalWatchProgressDao`, و `LocalFavoriteDao`. |
| **ADR 0003 (معمارية Clean Architecture)** | متوافق | لا يغير أي واجهات أو نماذج في طبقة الـ Domain؛ التغيير محصور في توليد طبقة الـ Data. |
| **مواصفات الأداء (NFR - Low Spec Strategy)** | متوافق بقوة | يخفض استهلاك الرام ووقت بناء التطبيق على الحواسيب المتواضعة. |

---

## 3. فحص التعارضات المعمارية والبرمجية (Architectural Conflicts Audit)

### 3.1 مطابقة إصدارات KSP مع Kotlin 2.0.21
* **المتطلب التقني:** يتطلب KSP توافقاً صارماً بين إصدار الإضافة وإصدار Kotlin المستخدم (Kotlin 2.0.21 يستلزم KSP `2.0.21-1.0.28` أو ما يقابلها).
* **المعالجة:** تثبيت الإصدار بدقة في `gradle/libs.versions.toml` لضمان عدم حدوث أي تعارض أثناء البناء التزايدي.

---

## 4. المخرجات الهندسية ومخطط التنفيذ (Engineering Deliverables)

* تحديث `gradle/libs.versions.toml` بإضافة كود ومكتبة KSP.
* تعديل `build.gradle.kts` و `app/build.gradle.kts` لتبديل kapt بـ ksp.
* فحص البناء بنجاح عبر `./gradlew compileDebugKotlin`.

---

## 5. القرار المعماري والتوصية النهائية (Architectural Verdict)

* **نتيجة التدقيق:** معتمدة ومطابقة معمارياً بنسبة 100% (Approved).
* **توافق الحوكمة والمعمارية:** تحل تحذير تراجع اللغة إلى 1.9، تتماشى تماماً مع بيئة Kotlin 2.0+ و JDK 17 وسقف الذاكرة 2048m، ولا تخل بنماذج Room DB أو ADR 0001.
* **التوصية التنفيذية:** ترقية ضرورية ومستحقة لأداء بيئة البناء والتطوير.
