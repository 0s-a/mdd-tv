# نظام التصميم السينمائي الراقي (Refined Cinema & Obsidian Design System)
## دستور رموز التصميم والقواعد البصرية الأساسية (Design Tokens & Visual Foundations)

> **الحالة:** معتمد ونافذ  
> **الإصدار:** 2.2.0  
> **تاريخ الاعتماد:** 20 نوفمبر 2026  
> **المنصة المستهدفة:** 100% Native Android • Jetpack Compose 120Hz • AMOLED / OLED Displays  
> **الفلسفة الأساسية:** **المحتوى هو البطل (Content is the Hero)**.  
> **نطاق الوثيقة:** مقتصر حصراً على القواعد الأساسية، الرموز الدلالية (Tokens)، وشبكات القياس دون الخوض في تفاصيل أو أكواد المكونات المشتقة.

---

## 1. المبادئ التأسيسية وميثاق مكافحة نمط الذكاء الاصطناعي (Anti-AI Manifesto)

تتميز منصات البث السينمائي الاحترافية بالهدوء والوقار البصري. الواجهة البرمجية وسيلة غير مرئية لنقل المشاهد إلى المحتوى الفني، وليست مساحة استعراضية للمؤثرات الصاخبة.

### مصفوفة الحظر والاعتماد:

| الممارسة المحظورة (نمط الـ AI المستبعد) | الممارسة المعتمدة (النمط السينمائي الراقي) | الغرض والهدف |
| :--- | :--- | :--- |
| كبسولات بيضاوية منتفخة (Radius 999dp) | استدارة هندسية معتدلة ومنطقية (4dp, 8dp, 10dp, 16dp) | إرساء طابع معماري رصين ومريح للعين. |
| هالات الوهج البنفسجي والسماوي (Neon Glow Aura) | تباين صلب ونظيف (High Contrast Clean Edges) | منع تشويه ألوان بوسترات الأفلام وحماية العين ليلاً. |
| الأسطح الزجاجية البراقة بحدود ملونة (Glassmorphism) | أسطح جرافيت مدخنة مطفأة (Matte Smoked Surfaces) | بيئة عرض وقورة تماثل صالات العرض السينمائي المظلمة. |
| الشارات الملونة الفاقعة للمواصفات التقنية | أختام مونوكرومية محفورة بخط أحادي المسافة | توثيق بيانات 4K و HDR كأختام احترافية هادئة. |
| وميض الشيمر السريع المتدرج بألوان النيون | شيمر كربوني يتنفس ببطء بين درجات الجرافيت | حالة تحميل ناعمة لا تشتت انتباه المستخدم. |
| حركات الارتداد المفرطة (Cartoon Bouncing) | استجابة فيزيائية حرجة غير مرتدة (No Bouncy Spring) | وقار سينمائي يركز على استقرار المشهد وانسيابيته. |

---

## 2. منظومة رموز الألوان والحالات التفاعلية (Color & State Tokens System)

تعتمد المنظومة على نموذج ثلاثي الطبقات (Primitive -> Semantic -> Component) يفصل بين القيمة اللونية الخام والدلالة الوظيفية، مع دعم أصيل لشاشات الـ OLED وتغطية كاملة للحالات التفاعلية (State Layers) المتوافقة مع معايير Material 3.

### أ. الرموز الأولية المجردة (Primitive Colors)
```kotlin
object JellyfinPrimitives {
    // درجات الكربون والأوبسيديان
    val BlackOled = Color(0xFF000000)
    val Obsidian950 = Color(0xFF0B0C0E)
    val Graphite900 = Color(0xFF14171D)
    val Graphite800 = Color(0xFF1B1E26)
    val Slate700 = Color(0xFF272B35)

    // درجات الحياد والطباعة
    val Ivory50 = Color(0xFFEDEAE5)
    val WhitePure = Color(0xFFFFFFFF)
    val Gray100 = Color(0xFFF3F4F6)
    val Gray400 = Color(0xFF9CA3AF)
    val Gray500 = Color(0xFF6B7280)
    val Gray700 = Color(0xFF374151)

    // درجات التمييز والتنبيه السينمائي
    val AmberWarm = Color(0xFFD4A373)
    val GoldCinema = Color(0xFFE5A93C)
    val CrimsonMuted = Color(0xFF9E4343)
    val EmeraldMuted = Color(0xFF2D8A66)
}
```

---

### ب. طبقات التفاعل وشفافية الحالات (State Layer Tokens)
```kotlin
@Immutable
object JellyfinStates {
    const val DraggedOpacity: Float = 0.16f
    const val FocusOpacity: Float = 0.12f
    const val PressedOpacity: Float = 0.10f
    const val HoverOpacity: Float = 0.08f
    const val DisabledContainerOpacity: Float = 0.12f
    const val DisabledContentOpacity: Float = 0.38f
}
```

---

### ج. الرموز الدلالية (Semantic Color Tokens)
```kotlin
@Immutable
data class JellyfinColors(
    val backgroundRoot: Color,
    val onBackgroundRoot: Color,
    val surfaceDefault: Color,
    val onSurfaceDefault: Color,
    val surfaceElevated: Color,
    val onSurfaceElevated: Color,
    val surfaceOverlay: Color,
    val borderHairline: Color,
    val borderFocus: Color,
    val actionPrimary: Color,
    val onActionPrimary: Color,
    val accentHighlight: Color,
    val ratingBadge: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val statusSuccess: Color,
    val statusAlert: Color
)

fun createJellyfinColorScheme(isOledTrueBlack: Boolean = false): JellyfinColors {
    return JellyfinColors(
        backgroundRoot = if (isOledTrueBlack) JellyfinPrimitives.BlackOled else JellyfinPrimitives.Obsidian950,
        onBackgroundRoot = JellyfinPrimitives.Gray100,
        surfaceDefault = if (isOledTrueBlack) JellyfinPrimitives.BlackOled else JellyfinPrimitives.Graphite900,
        onSurfaceDefault = JellyfinPrimitives.Gray100,
        surfaceElevated = JellyfinPrimitives.Graphite800,
        onSurfaceElevated = JellyfinPrimitives.Gray100,
        surfaceOverlay = Color(0x990B0C0E),
        borderHairline = Color(0x14FFFFFF), // حد شعري رمادي خافت 8%
        borderFocus = Color(0x80FFFFFF),    // إطار تركيز صلب بنسبة 50% أبيض لتحقيق تباين 3:1 فما فوق
        actionPrimary = JellyfinPrimitives.Ivory50,
        onActionPrimary = JellyfinPrimitives.Obsidian950,
        accentHighlight = JellyfinPrimitives.AmberWarm,
        ratingBadge = JellyfinPrimitives.GoldCinema,
        textPrimary = JellyfinPrimitives.Gray100,
        textSecondary = JellyfinPrimitives.Gray400,
        textMuted = JellyfinPrimitives.Gray500,
        statusSuccess = JellyfinPrimitives.EmeraldMuted,
        statusAlert = JellyfinPrimitives.CrimsonMuted
    )
}
```

---

### د. جدول تدقيق نسب التباين وإمكانية الوصول (WCAG 2.1 & 2.2 Audit)

| العنصر الدلالي | لون العنصر | لون السطح الحاضن | نسبة التباين الحسابية | المعيار والتوافق |
| :--- | :--- | :--- | :---: | :---: |
| النص الأساسي (textPrimary) | #F3F4F6 | #0B0C0E (Obsidian) | 16.5:1 | WCAG AAA |
| النص الثانوي (textSecondary) | #9CA3AF | #14171D (Surface) | 6.8:1 | WCAG AAA |
| النصوص الخافتة والميتاداتا (textMuted) | #6B7280 | #14171D (Surface) | 4.6:1 | WCAG AA |
| زر التشغيل الأساسي (actionPrimary) | #EDEAE5 | #0B0C0E (داخل الزر) | 15.2:1 | WCAG AAA |
| إطار التركيز والتحديد (borderFocus) | #80FFFFFF (50% White) | #0B0C0E (Obsidian) | 4.2:1 | WCAG 2.2 SC 2.4.11 (تجاوز 3:1) |
| أختام الدقة والمواصفات (labelSmall) | #9CA3AF | #1B1E26 (Elevated) | 5.8:1 | WCAG AA |

---

## 3. المقاييس المكانية والهندسة (Spatial & Geometry System)

تعتمد كافة القياسات حصراً على شبكة مضاعفات الرقم 4 و 8 (8pt Grid System) لمنع القيم العشوائية:

```kotlin
@Immutable
object JellyfinDimens {
    // 1. شبكة المسافات (Spacing Grid)
    val SpaceNone: Dp = 0.dp
    val SpaceMicro: Dp = 2.dp
    val Space2XS: Dp = 4.dp
    val SpaceXS: Dp = 8.dp
    val SpaceSm: Dp = 12.dp
    val SpaceMd: Dp = 16.dp
    val SpaceLg: Dp = 24.dp
    val SpaceXL: Dp = 32.dp
    val Space2XL: Dp = 48.dp
    val Space3XL: Dp = 64.dp

    // 2. مصفوفة الاستدارة الهندسية (Corner Radii - حظر تام لـ 999dp)
    val RadiusMicro: Dp = 4.dp      // الشارات التقنية والمؤشرات المصغرة
    val RadiusSmall: Dp = 8.dp      // الأزرار، حقول الإدخال، والرقائق
    val RadiusDefault: Dp = 10.dp   // بطاقات الأفلام وبوسترات الحلقات
    val RadiusLarge: Dp = 16.dp     // الحاويات العريضة واللوحات السفلية

    // 3. سُمك الحدود (Borders)
    val BorderHairline: Dp = 1.dp   // الحد الشعري القياسي لتعريف الأسطح
    val BorderFocus: Dp = 2.dp      // سُمك إطار التركيز والتحديد

    // 4. مقياس الأهداف اللمسية الأدنى (Touch Target)
    val MinTouchTarget: Dp = 48.dp  // الالتزام بمعيار WCAG 2.1 AA لمنع الضغط الخاطئ
}
```

---

## 4. منظومة التخطيط وشبكة الأعمدة (Layout Grid & Window Breakpoints)

استناداً إلى معايير Android WindowSizeClass، يتم توزيع المحتوى وفق شبكة أعمدة مرنة تضمن تجربة عرض مثالية:

### مصفوفة نقاط التوقف (Breakpoints Matrix):

| فئة حجم النافذة (WindowSizeClass) | عرض الشاشة (Width) | عدد الأعمدة (Columns) | الهوامش الخارجية (Margins) | الفواصل البينية (Gutters) | نوع الجهاز المستهدف |
| :--- | :---: | :---: | :---: | :---: | :--- |
| `Compact` | أقل من 600dp | 4 | 16dp | 12dp / 16dp | الهواتف الرأسية (Portrait Phones) |
| `Medium` | 600dp إلى 839dp | 8 | 24dp | 16dp | الأجهزة القابلة للطي والأجهزة اللوحية المصغرة |
| `Expanded` | 840dp فما فوق | 12 | 32dp | 24dp | الأجهزة اللوحية الكبيرة، شاشات العرض المكتبي و Android TV |

---

## 5. هرمية الطبقات والتراكب البصري (Layer Stack & Surface Elevation)

في بيئات العرض المظلمة وشاشات OLED، يُحظر الاعتماد على الظلال الساقطة الغائمة (Blurry Drop Shadows) المشوهة لنقاء السواد، ويُستعاض عنها بالارتفاع النغمي (Tonal Elevation) والحدود الشعرية الرقيقة.

### أ. مصفوفة مستويات السطح (Surface Tiers):
1. **المستوى 0 (Root Background):** سواد تام `BlackOled` أو أوبسيديان عميق `Obsidian950`.
2. **المستوى 1 (Default Surface):** أسطح الحاويات الأساسية وصفقات المحتوى `Graphite900` بإطار شعري 1dp.
3. **المستوى 2 (Elevated Surface):** بطاقات الأفلام التفاعلية والقوائم المنبثقة `Graphite800`.
4. **المستوى 3 (Overlay & Sheet):** لوحات التفاصيل المنزلقة (Bottom Sheets) وقوائم السياق.
5. **المستوى 4 (Critical Dialogs):** الحوارات التنبيهية والتأكيدية محاطة بحد شعري واضح.

### ب. سلم الطبقات البرمجي (Z-Index Tokens):
```kotlin
object JellyfinZIndex {
    const val Base: Float = 0f              // الخلفية وبوسترات الواجهة الترويجية
    const val Content: Float = 1f           // شبكات البطاقات والمحتوى التمريري
    const val StickyHeader: Float = 10f     // شريط العناوين والتبويبات المثبتة
    const val PlayerControls: Float = 20f   // أزرار التحكم بمشغل الفيديو ومسارات التقدم
    const val BottomSheet: Float = 30f      // اللوحات السفلية وقوائم الخيارات
    const val ModalDialog: Float = 40f      // مربعات الحوار الحاسمة والتحذيرات
    const val ToastNotification: Float = 50f // رسائل التأكيد المؤقتة ومؤشرات النظام
}
```

---

## 6. المنظومة الطباعية والأرقام الجدولية (Typography & Tabular Figures)

### أ. مصفوفة المقاييس الطباعية (Type Scales):

| النمط (Style) | الحجم (sp) | مسافة السطر (sp) | الوزن (Weight) | الغرض المعتمد |
| :--- | :---: | :---: | :--- | :--- |
| `displayLarge` | 36.sp | 44.sp | Bold | العناوين الترويجية الضخمة في الواجهة الرئيسية |
| `titleLarge` | 22.sp | 28.sp | SemiBold | أسماء الأعمال الفنية في شاشات التفاصيل |
| `titleMedium` | 16.sp | 22.sp | Medium | عناوين صفوف العرض وأقسام المكتبة |
| `bodyLarge` | 14.sp | 22.sp | Normal | القصة وملخص العمل الدرامي (حد أقصى 65 حرفاً للسطر) |
| `bodyMedium` | 13.sp | 18.sp | Normal | أسماء الممثلين والمخرج وتفاصيل الإنتاج |
| `labelSmall` | 11.sp | 14.sp | SemiBold | شارات الدقة التقنية والميتاداتا |

### ب. ميثاق الأرقام الجدولية (Tabular Numbers Policy):
* **القاعدة:** يُلزم استخدام خط أحادي المسافة (`FontFamily.Monospace`) أو تفعيل ميزة الأرقام الجدولية (`FontFeatureSettings("tnum")`) لعرض كافة الأرقام الديناميكية:
  1. توقيت المشغل المنقضي والمتبقي (`01:24:08 / 02:15:30`).
  2. معدلات نقل البيانات والسرعة (`24.5 Mbps`).
  3. الدقة التقنية ومعدل الإطارات (`3840x2160 @ 60fps`).
* **الهدف:** منع الاهتزاز الأفقي للأرقام وأشرطة التقدم أثناء التشغيل السريع للمشاهد.

---

## 7. فيزياء الحركة وإمكانية الوصول الحركي (Motion, Physics & Accessibility)

### أ. فيزياء الحركة الرصينة (120Hz Damped Spring Physics)
* يُحظر استخدام الارتداد المطاطي الطفولي. يتم الاعتماد على استجابة نوابض فيزيائية مخمدة حرجة لضمان الرصانة السينمائية:
  * **معامل التخميد (Damping Ratio):** `Spring.DampingRatioNoBouncy` (1.0f) لمنع أي ارتداد زائد.
  * **معامل الصلابة (Stiffness):** `Spring.StiffnessMediumLow` للحصول على استجابة ناعمة سريعة الاستقرار.
  * **الحد الأقصى لزمن الانتقال:** `250ms` لمنع تباطؤ استجابة التطبيق.

### ب. احترام تقليل الحركة (Reduced Motion Support)
* عند تفعيل إعدادات خفض الحركة في نظام التشغيل (`prefers-reduced-motion` أو تعطيل مقياس الرسوم المتحركة في خيارات المطور):
  1. استبدال كافة حركات الانتقال الفيزيائية بتلاشٍ بسيط ومباشر (Crossfade) بزمن أقصاه `100ms`.
  2. تحويل شيمر التحميل المتحرك إلى حالة ثبات لوني مصمتة بدرجة `Graphite900`.

### ج. نمط التحميل الشبحي الكربوني (Carbon Breathing Shimmer)
* يُحظر استخدام الوميض الأبيض أو المتدرج بألوان النيون.
* يتنفس الشيمر المعتمد برقة فائقة بين درجتين كربونيتين:
  * لون البداية: `#14171D` (Graphite900).
  * لون الذروة: `#1B1E26` (Graphite800).
  * زمن الدورة الكاملة: `1200ms` مع تلاشٍ جيبي ناعم.

### د. معدل الأسطح المدخنة الأساسي (Base Cinema Surface Modifier)
المعدل البرمجي الوحيد المعتمد على مستوى الأساسات لاستبدال دوال الـ Glassmorphism السابقة:

```kotlin
fun Modifier.cinemaSurface(
    backgroundColor: Color = JellyfinPrimitives.Graphite900,
    borderColor: Color = Color(0x14FFFFFF),
    cornerRadius: Dp = JellyfinDimens.RadiusDefault,
    borderWidth: Dp = JellyfinDimens.BorderHairline
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    return this
        .clip(shape)
        .background(backgroundColor)
        .border(borderWidth, borderColor, shape)
}
```

---

## 8. جدول خريطة التحويل البرمجي (Migration Mapping)

| الرمز القديم المستبعد | الرمز الجديد المعتمد | التوجيه البرمجي |
| :--- | :--- | :--- |
| `JellyfinColors.PrimaryIndigo` (`#6366F1`) | `actionPrimary` أو `accentHighlight` | استخدام الأبيض العاجي للأزرار أو الكهرماني للتمييز |
| `JellyfinColors.IndigoGlow` | **ملغي نهائياً** | استبداله بالتباين المصمت النظيف |
| `JellyfinColors.TechSky` (`#38BDF8`) | `textPrimary` أو `borderHairline` | تحويل الشارات إلى أختام مونوكرومية هادئة |
| `JellyfinColors.SurfaceGlass` | `surfaceDefault` (`#14171D`) | أسطح جرافيت مدخنة غير عاكسة |
| `JellyfinColors.BorderGlass` | `borderHairline` (`Color(0x14FFFFFF)`) | حدود شعرية رقيقة بنسبة 8% أبيض |
| `JellyfinDimens.RadiusPill` (`999.dp`) | `RadiusMicro` أو `RadiusSmall` | استبدال الكبسولات بأنصاف أقطار 4dp أو 8dp |
| `Modifier.glassmorphism()` | `Modifier.cinemaSurface()` | اعتماد الأسطح المطفأة بدلاً من الزجاج الشفاف |
| `Spring.DampingRatioLowBouncy` | `Spring.DampingRatioNoBouncy` | إلغاء ارتداد الحركات بالكامل لترسيخ الوقار السينمائي |
