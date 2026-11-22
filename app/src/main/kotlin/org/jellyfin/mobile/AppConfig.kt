package org.jellyfin.mobile

/**
 * تهيئة التطبيق الأساسية - مبدأ الاعتماد على السيرفر (Server-Driven Architecture).
 * يتم هنا فقط تحديد نقطة البداية (عنوان السيرفر واسم المستخدم)، بينما تُجلب
 * كافة المعرفات (Server ID, User ID)، الإصدارات، التفضيلات (الترجمة)، والأقسام
 * ديناميكياً من الخادم لحظياً.
 */
object AppConfig {
    // 1. هوية التطبيق (App Identity)
    const val APP_NAME = "مدد TV"
    const val APP_NAME_EN = "mdd TV"
    const val APP_VERSION = "1.2.0"

    // 2. نقطة الاتصال بالسيرفر والمستخدم المعتمد فقط (Server & User Endpoint)
    const val SERVER_URL = "http://192.168.200.200:80"
    const val DEFAULT_USERNAME = "الاستراحه"
    const val DEFAULT_PASSWORD = "" // دخول مباشر مفتوح

    // 3. خيارات مشغل الفيديو العتادي (Hardware Player Engine)
    const val FORCE_HARDWARE_DECODE = true
    const val ENABLE_DIRECT_PLAY = true

    fun buildVideoStreamUrl(mediaId: String, token: String? = null): String {
        val base = "${SERVER_URL.trimEnd('/')}/Videos/$mediaId/stream?static=true"
        return if (!token.isNullOrBlank()) "$base&api_key=$token" else base
    }
}
