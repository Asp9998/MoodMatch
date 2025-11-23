package com.aryanspatel.moodmatch.presentation.models


data class ProfileUiState(
    val nickname: String = "",
    val selectedMood: String? = null,
    val selectedAvatar: String = "",
    val isSaving: Boolean = false,
    val error: ProfileError = ProfileError.None
)

sealed class ProfileError {
    data object None : ProfileError()
    data class Message(val text: String) : ProfileError()
}
