package org.jellyfin.mobile.domain.model

data class AvatarItem(
    val key: String,
    val emoji: String,
    val label: String
)

object AvatarCatalog {
    val avatars = listOf(
        AvatarItem("avatar_1", "\uD83C\uDFAD", "ممثل"),
        AvatarItem("avatar_2", "\uD83E\uDD81", "أسد"),
        AvatarItem("avatar_3", "\uD83D\uDE80", "رائد فضاء"),
        AvatarItem("avatar_4", "\uD83D\uDD76", "عميل سري"),
        AvatarItem("avatar_5", "\u2B50", "نجم"),
        AvatarItem("avatar_6", "\uD83C\uDFAC", "مخرج"),
        AvatarItem("avatar_7", "\uD83C\uDF19", "ليلي"),
        AvatarItem("avatar_8", "\uD83D\uDD25", "حماسي")
    )

    fun getEmoji(key: String?): String {
        return avatars.find { it.key == key }?.emoji ?: "\uD83C\uDFAD"
    }
}
