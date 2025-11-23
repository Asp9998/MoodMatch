package com.aryanspatel.moodmatch.presentation.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aryanspatel.moodmatch.data.repositories.MoodMatchRepository
import com.aryanspatel.moodmatch.domain.usecases.OnboardingPresets
import com.aryanspatel.moodmatch.domain.usecases.UserPreferenceValidator
import com.aryanspatel.moodmatch.presentation.models.ProfileError
import com.aryanspatel.moodmatch.presentation.models.ProfileUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repo: MoodMatchRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repo.ensureSessionLoaded()
            resetPreferences()
        }
    }

    fun onNicknameChanged(newValue: String) {
        _uiState.update { it.copy(nickname = newValue) }
    }

    fun onMoodSelected(mood: String) {
        _uiState.update { it.copy(selectedMood = mood) }
    }

    fun onAvatarShuffle() {
        val currentState = _uiState.value
        val pool = OnboardingPresets.avatarPoolForMood(currentState.selectedMood)
        val nextAvatar = pool.random()

        _uiState.update { it.copy(selectedAvatar = nextAvatar)}
    }

    fun onSaveClicked(
        onSuccess: () -> Unit,
    ) {
        val state = _uiState.value

        val nicknameError = UserPreferenceValidator.validateNickname(state.nickname)
        val moodError = UserPreferenceValidator.validateMood(state.selectedMood)
        val avatarError = UserPreferenceValidator.validateAvatar(state.selectedAvatar)
        val firstErrorMessage = nicknameError ?: moodError ?: avatarError
        if (firstErrorMessage != null) {
            _uiState.update {
                it.copy(
                    error = ProfileError.Message(firstErrorMessage)
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            try {
                val moodToSend = state.selectedMood?.lowercase() // or .value
                repo.updatePreferences(
                    nickname = state.nickname.trim(),
                    mood = moodToSend,
                    avatar = state.selectedAvatar
                )
                _uiState.update { it.copy(isSaving = false) }
                onSuccess()

            } catch (e: NicknameNotAcceptableException) {
                _uiState.update {
                    it.copy(
                        error = ProfileError.Message(e.message ?: "Nickname not acceptable")
                    )
                }
            } catch (e: Exception) {
                Log.d("ProfileUpdateError", "onContinueClicked: $e")
                _uiState.update {
                    it.copy(
                        error = ProfileError.Message("Something went wrong. Please try again")
                    )
                }
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun consumeError() {
        _uiState.update { it.copy(error = ProfileError.None) }
    }

    fun resetPreferences(){
        viewModelScope.launch {
            repo.sessionFlow().firstOrNull()?.let { session ->
                _uiState.update {
                    it.copy(
                        nickname = session.profile.nickname,
                        selectedMood = session.profile.mood, // write helper
                        selectedAvatar = session.profile.avatar
                    )
                }
            }
        }
    }
}
