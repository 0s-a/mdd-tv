package org.jellyfin.mobile.domain.model

data class AvatarItem(
    val key: String,
    val emoji: String,
    val label: String
)

object AvatarCatalog {
    val avatars = listOf(
        AvatarItem("avatar_1", "🎭", "ممثل"),
        AvatarItem("avatar_2", "🦁", "أسد"),
        AvatarItem("avatar_3", "🚀", "رائد فضاء"),
        AvatarItem("avatar_4", "🕶️", "عميل سري"),
        AvatarItem("avatar_5", "⭐", "نجم"),
        AvatarItem("avatar_6", "🎬", "مخرج"),
        AvatarItem("avatar_7", "🌙", "ليلي"),
        AvatarItem("avatar_8", "🔥", "حماسي")
    )

    fun getEmoji(key: String?): String {
        return avatars.find { it.key == key }?.emoji ?: "🎭"
    }
}
