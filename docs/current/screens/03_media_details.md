# تفاصيل الأعمال الفنية لهواتف أندرويد — المعمارية الموزعة (V3)
## Screen 03 — Mobile Media Details Specialized Screens Suite

> **المرجع المعماري:** `docs/current/screens/media_details/`  
> **المنصة المستهدفة:** هواتف أندرويد الذكية (Android Mobile Phones) • تفاعل لمسي رأسي (Portrait-First Touch UI)  
> **الفلسفة:** شاشات عرض مخصصة لكل نوع وسائط • سواد OLED تام (`#000000`) • منطقة راحة الإبهام (Thumb Zone) • شريط علوي متكيف (Collapsing Toolbar)  
> **حالة الميزة:** معتمدة رسمياً ومقسمة إلى شاشات متخصصة لكل نوع عمل.

---

## الانتقال إلى المعمارية الموزعة لشاشات التفاصيل على الهاتف

تمت ترقية شاشة التفاصيل من نموذج "الشاشة الموحدة" إلى **حزمة شاشات متخصصة موزعة لهواتف أندرويد** تراعي الفروق الجوهرية بين وسائط الفيديو، المسلسلات، الموسيقى، والسلاسل، وتتوزع وثائقها كاملة داخل المجلد المخصص:

* [media_details/README.md](./media_details/README.md): **الفهرس العام لحزمة شاشات تفاصيل الهاتف المحمول**

---

### ملفات الحزمة المعمارية للهاتف:

1. [00_general_layout_architecture.md](./media_details/00_general_layout_architecture.md): الهيكل العام الموحد وفلسفة اللمس ومنطقة الإبهام والشريط المتكيف.
2. [01_shared_components_and_philosophy.md](./media_details/01_shared_components_and_philosophy.md): مكتبة المكونات المشتركة واللوحات السفلية (Bottom Sheets).
3. [02_movie_details_screen.md](./media_details/02_movie_details_screen.md): شاشة تفاصيل الأفلام السينمائية للهاتف (`Movie`).
4. [03_series_details_screen.md](./media_details/03_series_details_screen.md): شاشة تفاصيل المسلسلات والمواسم للهاتف (`Series`).
5. [04_boxset_details_screen.md](./media_details/04_boxset_details_screen.md): شاشة تفاصيل السلاسل والمجموعات للهاتف (`BoxSet`).
6. [05_music_album_details_screen.md](./media_details/05_music_album_details_screen.md): شاشة تفاصيل الألبومات والموسيقى والمشغل المصغر السفلي (`MusicAlbum`).
7. [06_live_tv_details_screen.md](./media_details/06_live_tv_details_screen.md): شاشة تفاصيل البث المباشر مع دعم النافذة العائمة PiP (`LiveTvChannel`).
8. [SERVER_PAYLOADS_AND_DIFFERENCES.md](./media_details/SERVER_PAYLOADS_AND_DIFFERENCES.md): توثيق بيانات السيرفر، الفروق، واستراتيجية معالجة النقص.
