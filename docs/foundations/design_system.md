# نظام التصميم السينمائي الراقي (Refined Cinema & Obsidian Design System)
## مواصفات رموز التصميم ومكونات Jetpack Compose المتقنة (Design Tokens & UI Specs)

> **فلسفة التصميم الأساسية:** **«المحتوى هو البطل» (Content is the Hero).**  
> التخلص الجذري والشامل من بهرجة الذكاء الاصطناعي النمطية (تدرجات النيون البنفسجية والسماوية، الهالات المتوهجة الاصطناعية، الكبسولات العائمة المنتفخة `Pill Badges`).  
> يستبدل ذلك بهدوء وفخامة دور السينما الكلاسيكية وتطبيقات البث المتخصصة للمحترفين (*A24*, *Criterion Channel*, *Apple TV+*)، مع الحفاظ على الأداء الفائق والتباين الكامل لشاشات الـ AMOLED بمعدل تحديث 120Hz.

---

##  1. ميثاق مناهضة نمط الـ AI (Anti-AI Aesthetics Principles)

| عنصر واجهة الـ AI المستبعد | البديل السينمائي الراقي المعتمد | المبرر التصميمي والوظيفي |
| :--- | :--- | :--- |
| **كبسولات بيضاوية كاملة (`radius: 999dp`)** | أشكال هندسية متزنة بزوايا دقيقة (`4dp` للشارات، `8dp` للأزرار، `12dp` للبطاقات) | التخلص من المظهر الطفولي الاستعراضي والاعتماد على بناء هندسي معماري رصين. |
| **هالات الوهج البنفسجي والسماوي (Neon Aura & Glow)** | تباين صلب ونظيف (*High-contrast crisp edges*) وإضاءة خلفية ناعمة محايدة | ألوان النيون تشتت انتباه المشاهد وتشوه ألوان أغلفة وبوسترات الأعمال الفنية. |
| **الزجاج المصقول البراق بحدود سماوية (`Glassmorphism`)** | أسطح جرافيت مدخنة مطفأة (*Matte Smoked Surfaces*) بحدود شعرية ناعمة جداً (`1px`) | توفير أقصى درجات الراحة البصرية في الغرف المعتمة بدون انعكاسات مصطنعة. |
| **شارات التقنية الملونة الفاقعة (Neon Chips)** | أختام تقنية مونوكرومية محفورة بدقة (*Minimalist Monospace Spec Seals*) | إظهار مواصفات 4K و HDR كأختام استوديو احترافية بدلاً من فقاعات ألعاب. |

---

##  2. لوحة الألوان السينمائية الراقية (Color Tokens)

### أ. ألوان السواد التام والأسطح المدخنة (OLED & Matte Surfaces)
```kotlin
// الرموز المعتمدة لأسطح التطبيق وخلفياته
val PureBlack = Color(0xFF000000)        // سواد تام لشاشات AMOLED لإطفاء البكسلات بالكامل
val Background = Color(0xFF0B0C0E)       // أوبسيديان كربوني حيادي عميق بدلاً من الكحلي المتوهج
val SurfaceDark = Color(0xFF14171D)      // جرافيت مدخن للبطاقات غير النشطة
val SurfaceElevated = Color(0xFF1B1E26)  // سطح مرتفع ناعم للقوائم المنبثقة وأشرطة الأدوات
val SurfaceOverlay = Color(0x990B0C0E)   // غطاء مدخن شبه شفاف (60%) لتسهيل قراءة النصوص فوق البوسترات
val BorderSubtle = Color(0x14FFFFFF)     // حد شعري رقيق جداً (8% أبيض) يمنح فصلاً هندسياً نقياً
val BorderHighlight = Color(0x33FFFFFF)  // حد التركيز عند التنقل بالريموت أو اللمس (20% أبيض)
```

| اسم اللون في كود Compose | القيمة (Hex / ARGB) | الاستخدام والغرض |
| :--- | :---: | :--- |
| `JellyfinColors.PureBlack` | `#000000` | شاشات AMOLED، وضع التوفير الأقصى، وحدود الفيديو أثناء التشغيل. |
| `JellyfinColors.Background` | `#0B0C0E` | الخلفية الافتراضية للتطبيق، صامتة ومريحة جداً للعين ليلاً. |
| `JellyfinColors.SurfaceDark` | `#14171D` | خلفيات بطاقات الأفلام، عناصر القوائم، والمربعات المجمعة. |
| `JellyfinColors.SurfaceElevated`| `#1B1E26` | القوائم الجانبية، أشرطة التنقل، وصناديق الحوار. |
| `JellyfinColors.BorderSubtle` | `Color(0x14FFFFFF)` | حدود رقيقة (1dp) لتعريف حواف البطاقات دون استخدام أي نيون. |
| `JellyfinColors.BorderHighlight`| `Color(0x33FFFFFF)` | مؤشر التركيز النشط (*Focus Indicator*) لعناصر التلفاز واللمس. |

---

### ب. عناصر التفاعل والشارات المتقنة (Refined Accents & Metadata)
```kotlin
// ألوان التمييز والتفاعل الراقي
val ActionPrimary = Color(0xFFEDEAE5)    // أبيض عاجي راقٍ لأزرار التشغيل الأساسية (عالي التباين)
val OnActionPrimary = Color(0xFF0B0C0E)  // أيقونة ونص أسود داكن صلب فوق الزر الأبيض العاجي
val AccentAmber = Color(0xFFD4A373)      // كهرماني دافئ سينمائي للتقييمات المميزة ونقاط التقدم النشطة
val RatingGold = Color(0xFFE5A93C)       // ذهبي كلاسيكي مطفأ لتقييمات IMDb المعتمدة
val StatusMuted = Color(0xFF6B7280)      // رمادي أردوازي لشارات "تمت المشاهدة" بدلاً من الزمردي الفاقع
val ErrorMuted = Color(0xFF9E4343)       // أحمر قرميدي وقور للتنبيهات والأخطاء
```

| اسم اللون | القيمة (Hex) | الاستخدام والغرض |
| :--- | :---: | :--- |
| `JellyfinColors.ActionPrimary` | `#EDEAE5` | زر التشغيل الأساسي (*Play Button*) لتوفير تباين فوري دون بهرجة. |
| `JellyfinColors.OnActionPrimary` | `#0B0C0E` | لون أيقونة التشغيل والنص الداخلي للزر الأساسي. |
| `JellyfinColors.AccentAmber` | `#D4A373` | شريط تقدم المشاهدة الفاخر، عناصر التحديد الخاصة. |
| `JellyfinColors.RatingGold` | `#E5A93C` | نجمة التقييم السينمائي وأرقام المراجعات الموثوقة. |
| `JellyfinColors.StatusMuted` | `#6B7280` | مؤشرات الحالة المكتملة دون خطف بصر المستخدم عن المحتوى. |

---

### ج. درجات النصوص والتسلسل الطباعي (Typography Contrast)
تطبيق مبدأ الطباعة التحريرية الفاخرة المريحة للرؤية الليلية:

| الاسم | القيمة | الاستخدام | نسبة التباين على الخلفية |
| :--- | :---: | :--- | :---: |
| `JellyfinColors.TextPrimary` | `#F3F4F6` | عناوين الأعمال، الأسماء الرئيسية، وأرقام المواسم | 16.5:1 (AAA) |
| `JellyfinColors.TextSecondary` | `#9CA3AF` | قصص الأفلام والملخصات، سنوات الإنتاج، والتصنيفات | 7.8:1 (AAA) |
| `JellyfinColors.TextMuted` | `#6B7280` | التفاصيل التقنية الدقيقة، الكوديك، الأحجام، وسرعة البث | 4.6:1 (AA) |

---

##  3. مقاييس الأبعاد والهندسة الرصينة (Geometry & Dimens)

تم استبدال المقاييس البيضاوية النمطية (`RadiusPill = 999.dp`) بمصفوفة هندسية واضحة:

```kotlin
@Immutable
object JellyfinDimens {
    // Spacing Scale
    val SpaceNone: Dp = 0.dp
    val SpaceMicro: Dp = 2.dp
    val SpaceExtraSmall: Dp = 4.dp
    val SpaceSmall: Dp = 8.dp
    val SpaceMedium: Dp = 12.dp
    val SpaceLarge: Dp = 16.dp
    val SpaceExtraLarge: Dp = 24.dp
    val Space2XL: Dp = 32.dp
    val Space3XL: Dp = 48.dp

    // Geometric Radii (إلغاء تام لنصف قطر الكبسولات 999.dp)
    val RadiusMicro: Dp = 4.dp      // شارات المواصفات التقنية (4K, HDR, Atmos)
    val RadiusSmall: Dp = 8.dp      // الأزرار الفرعية والرقائق المربعة
    val RadiusDefault: Dp = 10.dp   // بطاقات الأفلام وبوسترات الحلقات
    val RadiusLarge: Dp = 16.dp     // البطاقات العريضة واللوحات المنبثقة

    // Borders
    val BorderHairline: Dp = 1.dp   // الحد الشعري القياسي لكافة البطاقات
    val BorderFocus: Dp = 2.dp      // حد التركيز عند تحريك مؤشر أجهزة التلفاز D-Pad

    // Touch Targets
    val MinTouchTarget: Dp = 48.dp  // الالتزام التام بمعايير WCAG 2.1 AA
}
```

---

## ️ 4. مكونات Jetpack Compose الراقية (Refined Components)

### أ. السطح المدخن السينمائي (Matte Smoked Surface Modifier)
بديل دالة `glassmorphism` السابقة ذات الحدود الزجاجية والسماوية:

```kotlin
/**
 * Modifier يطبق سطحاً مدخناً مطفأً مع حد شعري ناعم جداً، متخلصاً تماماً من بهرجة الزجاج والنيون.
 */
fun Modifier.cinemaSurface(
    backgroundColor: Color = JellyfinColors.SurfaceDark,
    borderColor: Color = JellyfinColors.BorderSubtle,
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

### ب. شارة المواصفات التقنية الاحترافية (Cinema Spec Badge)
بديل الكبسولات العائمة الملونة لشارات مثل `4K UHD`, `Dolby Atmos`, `HDR10`:

```kotlin
@Composable
fun CinemaSpecBadge(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(JellyfinDimens.RadiusMicro))
            .background(Color(0x1AFFFFFF)) // خلفية معتمة بنسبة 10% فقط
            .border(
                width = JellyfinDimens.BorderHairline,
                color = Color(0x26FFFFFF),  // حد هندسي رمادي محفور ناعم
                shape = RoundedCornerShape(JellyfinDimens.RadiusMicro)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            ),
            color = JellyfinColors.TextPrimary
        )
    }
}
```

---

### ج. زر التشغيل السينمائي عالي التباين (Hero Play Button)
بديل الكبسولة البنفسجية المتوهجة بالنيون:

```kotlin
@Composable
fun HeroPlayButton(
    title: String = "تشغيل",
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = JellyfinColors.ActionPrimary,
            contentColor = JellyfinColors.OnActionPrimary
        ),
        shape = RoundedCornerShape(JellyfinDimens.RadiusSmall),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
        modifier = modifier.defaultMinSize(minHeight = JellyfinDimens.MinTouchTarget)
    ) {
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = null,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}
```

---

### د. شريط التقدم السينمائي الدقيق (Cinematic Media Scrubber)
بديل الأشرطة السميكة الملونة بألوان الذكاء الاصطناعي:

```kotlin
@Composable
fun CinematicScrubber(
    progress: Float, // من 0.0 إلى 1.0
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(3.dp) // نحيف ودقيق لا يشوش على كادر المشاهدة
            .clip(RoundedCornerShape(1.5.dp))
            .background(Color(0x33FFFFFF))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(JellyfinColors.AccentAmber) // كهرماني سينمائي راقٍ
        )
    }
}
```

---

##  5. التعامل مع تدرجات البوسترات والخلفيات (Vignette & Hero Scrims)

* **منع التدرجات الملونة فوق البوسترات:** يمنع دمج أي لون أرجواني أو أزرق فوق صور الأفلام.
* **التدرج المعتمد (True Obsidian Scrim):** تدرج عمودي أحادي ناعم ينطلق من الشفافية التامة وينتهي بلون الخلفية الأساسي (`#0B0C0E`):
  ```kotlin
  val HeroScrimGradient = Brush.verticalGradient(
      0.0f to Color.Transparent,
      0.5f to Color(0x660B0C0E),
      1.0f to JellyfinColors.Background
  )
  ```

---

##  6. جدول خريطة التحديث (Migration Mapping Table)

لتسهيل تحديث أي شاشة في التطبيق برمجياً:

| العنصر القديم في الكود | البديل الراقي الجديد |
| :--- | :--- |
| `JellyfinColors.PrimaryIndigo` (`#6366F1`) | `JellyfinColors.ActionPrimary` (`#EDEAE5`) للأزرار أو `AccentAmber` (`#D4A373`) للتمييز |
| `JellyfinColors.IndigoGlow` | **ملغي تماماً** — الاعتماد على التباين المصمت |
| `JellyfinColors.TechSky` (`#38BDF8`) | `JellyfinColors.TextPrimary` أو `BorderSubtle` كختم مونوكرومي |
| `JellyfinColors.SurfaceGlass` | `JellyfinColors.SurfaceDark` (`#14171D`) |
| `JellyfinColors.BorderGlass` | `JellyfinColors.BorderSubtle` (`Color(0x14FFFFFF)`) |
| `JellyfinDimens.RadiusPill` (`999.dp`) | `JellyfinDimens.RadiusMicro` (`4.dp`) للشارات أو `RadiusSmall` (`8.dp`) للأزرار |
| `Modifier.glassmorphism()` | `Modifier.cinemaSurface()` |
