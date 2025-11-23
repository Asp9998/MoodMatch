package com.aryanspatel.moodmatch.presentation.models

import androidx.compose.ui.graphics.Color

data class Mood(
    val emoji: String,
    val label: String,
    val moodColor: Color,
)

enum class AvailableMoods{
    Chill,
    Fire,
    Silly,
    Shy,
    Wild
}

data class OnboardingUiState(
    val nickname: String = "",
    val selectedMood: Mood? = null,
    val selectedAvatar: String = "😉",
    val isSubmitting: Boolean = false,
    val error: OnboardingError = OnboardingError.None,
)

sealed class OnboardingError {
    data object None : OnboardingError()
    data class Message(val text: String) : OnboardingError()
}
