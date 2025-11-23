package com.aryanspatel.moodmatch.presentation.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aryanspatel.moodmatch.data.datastore.UserPreference
import com.aryanspatel.moodmatch.data.repositories.MoodMatchRepository
import com.aryanspatel.moodmatch.domain.usecases.OnboardingPresets
import com.aryanspatel.moodmatch.domain.usecases.UserPreferenceValidator
import com.aryanspatel.moodmatch.presentation.models.Mood
import com.aryanspatel.moodmatch.presentation.models.OnboardingError
import com.aryanspatel.moodmatch.presentation.models.OnboardingUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repo: MoodMatchRepository,
    @ApplicationContext private val context: Context
): ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onNicknameChanged(newValue: String) {
        _uiState.update { state ->
            state.copy(
                nickname = newValue,
            )
        }
    }

    fun onMoodSelected(mood: Mood) {
        _uiState.update { state ->
            state.copy(
                selectedMood = mood,
            )
        }
    }

    fun onAvatarShuffle() {
        val currentState = _uiState.value
        val pool = OnboardingPresets.avatarPoolForMood(currentState.selectedMood?.label)
        val nextAvatar = pool.random()

        _uiState.update {
            it.copy(
                selectedAvatar = nextAvatar,
            )
        }
    }

    fun onContinueClicked(
        onSuccess: () -> Unit,
    ){
        val state = _uiState.value

        val nicknameError = UserPreferenceValidator.validateNickname(state.nickname)
        val moodError = UserPreferenceValidator.validateMood(state.selectedMood?.label)
        val avatarError = UserPreferenceValidator.validateAvatar(state.selectedAvatar)
        val firstErrorMessage = nicknameError ?: moodError ?: avatarError
        if (firstErrorMessage != null) {
            _uiState.update {
                it.copy(
                    error = OnboardingError.Message(firstErrorMessage)
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, error = OnboardingError.None) }

            try {
                val moodToSend = state.selectedMood!!.label.lowercase() // or .value
                Log.d("PreferenceNull", "onContinueClicked: ${state.nickname.trim()}, $moodToSend, ${state.selectedAvatar} ")
                repo.register(
                    nickname = state.nickname.trim(),
                    mood = moodToSend,
                    avatar = state.selectedAvatar
                )
                UserPreference.setOnboardingStatusFinished(context = context)
                onSuccess()

            } catch (e: NicknameNotAcceptableException) {
                _uiState.update {
                    it.copy(
                        error = OnboardingError.Message(e.message ?: "Nickname not acceptable")
                    )
                }
            }  catch (e: Exception) {
                Log.d("OnboardingRegisterError", "onContinueClicked: $e")
                _uiState.update {
                    it.copy(
                        error = OnboardingError.Message("Something went wrong. Please try again")
                    )
                }
            } finally {
                _uiState.update { it.copy(isSubmitting = false) }
            }
        }
    }

    fun consumeError() {
        _uiState.update { it.copy(error = OnboardingError.None) }
    }
}

class NicknameNotAcceptableException(
    message: String = "Nickname is not acceptable"
) : Exception(message)