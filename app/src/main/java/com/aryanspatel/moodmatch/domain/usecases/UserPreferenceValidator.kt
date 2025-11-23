package com.aryanspatel.moodmatch.domain.usecases

object UserPreferenceValidator {

    private const val MAX_NICKNAME_LENGTH = 24
    private const val MAX_AVATAR_LENGTH = 4

    // Mirror backend allowed moods
    private val allowedMoodValues = setOf("chill", "fire", "silly", "shy", "wild")

    fun validateNickname(nickname: String): String? {
        if (nickname.isBlank()) return "Nickname cannot be empty"
        if (nickname.length > MAX_NICKNAME_LENGTH) {
            return "Nickname too long (max $MAX_NICKNAME_LENGTH characters)"
        }
        return null
    }

    fun validateMood(mood: String?): String? {
        if (mood == null) return "Please select a mood"

        // If Mood has a "value" or "label" field, use the one you send to backend
        val value = mood.lowercase() // or mood.value, depending on your model

        if (!allowedMoodValues.contains(value)) {
            return "Invalid mood selected"
        }
        return null
    }

    fun validateAvatar(avatar: String): String? {
        if (avatar.isBlank()) return "Please select an avatar"
        if (avatar.length > MAX_AVATAR_LENGTH) {
            return "Avatar is invalid"
        }
        return null
    }
}
