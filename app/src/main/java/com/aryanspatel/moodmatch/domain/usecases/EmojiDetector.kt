package com.aryanspatel.moodmatch.domain.usecases

// Rough but very solid for chat use-cases
fun String.isEmojiOnly(): Boolean {
    val trimmed = trim()
    if (trimmed.isEmpty()) return false

    var i = 0
    var hasEmoji = false

    while (i < trimmed.length) {
        val codePoint = trimmed.codePointAt(i)

        when {
            codePoint.isEmojiCodePoint() -> {
                hasEmoji = true
            }

            // skin tones, variation selectors, joiners -> allowed as part of emoji
            codePoint == 0x200D || // zero-width joiner
                    codePoint in 0xFE00..0xFE0F || // variation selectors
                    codePoint in 0x1F3FB..0x1F3FF -> { // skin tone modifiers
                // ignore, they are valid emoji components
            }

            Character.isWhitespace(codePoint) -> {
                // ignore spaces
            }

            else -> {
                // any normal text / symbol → not emoji-only
                return false
            }
        }

        i += Character.charCount(codePoint)
    }

    return hasEmoji
}

private fun Int.isEmojiCodePoint(): Boolean {
    return this in 0x1F600..0x1F64F || // emoticons 🙂
            this in 0x1F300..0x1F5FF || // misc symbols & pictographs
            this in 0x1F680..0x1F6FF || // transport & map
            this in 0x2600..0x26FF   || // misc symbols
            this in 0x2700..0x27BF   || // dingbats
            this in 0x1F900..0x1F9FF || // supplemental symbols & pictographs
            this in 0x1FA70..0x1FAFF   // symbols & pictographs extended
}