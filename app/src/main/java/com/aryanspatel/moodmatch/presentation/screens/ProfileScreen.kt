package com.aryanspatel.moodmatch.presentation.screens

import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.sin
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.aryanspatel.moodmatch.domain.usecases.OnboardingPresets
import com.aryanspatel.moodmatch.presentation.BottomActionButton
import com.aryanspatel.moodmatch.presentation.HorizontalSlidingOverlay
import com.aryanspatel.moodmatch.presentation.MoodSelector
import com.aryanspatel.moodmatch.presentation.NickNameCard
import com.aryanspatel.moodmatch.presentation.SnackBarMessage
import com.aryanspatel.moodmatch.presentation.SubmittingState
import com.aryanspatel.moodmatch.presentation.models.ProfileError
import com.aryanspatel.moodmatch.presentation.viewmodels.ProfileViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onDismiss: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val moods = OnboardingPresets.moods
    val snackbarHostState = remember { SnackbarHostState() }

    var message by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current


    val infiniteTransition = rememberInfiniteTransition(label = "profileHero")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "float"
    )
    val floatY = sin(Math.toRadians(floatOffset.toDouble())).toFloat() * 6f

    // save button
    val buttonEnabled =
        uiState.nickname.isNotEmpty() && uiState.selectedMood != null
    val buttonAlpha by animateFloatAsState(
        targetValue = if (buttonEnabled) 1f else 0.5f,
        label = "profileBtnAlpha"
    )

    // React to error changes
    LaunchedEffect(uiState.error) {
        val error = uiState.error
        if (error is ProfileError.Message) {
            snackbarHostState.showSnackbar(error.text)
            viewModel.consumeError()
        }
    }

    LaunchedEffect(message) {
        if(message.isNotEmpty()){
            snackbarHostState.showSnackbar(message = message,)
        }
        message = ""
    }

    HorizontalSlidingOverlay(
        title = "Profile",
        onDismiss = {
            viewModel.resetPreferences()
            onDismiss()
                    },
    ) {

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {

            // soft blobs
        Box(
            modifier = Modifier
                .size(360.dp)
                .align(Alignment.TopEnd)
                .offset(x = 40.dp, y = 20.dp)
                .background(Color(0xFF7C3AED).copy(alpha = 0.18f), CircleShape)
                .blur(80.dp)
        )
        Box(
            modifier = Modifier
                .size(360.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-30).dp, y = 40.dp)
                .background(Color(0xFFFF7F6B).copy(alpha = 0.18f), CircleShape)
                .blur(90.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(22.dp))

            AvatarSection(
                avatar = uiState.selectedAvatar,
                onAvatarChange = { viewModel.onAvatarShuffle() },
                infiniteTransition = infiniteTransition,
                floatY = floatY
            )

            Spacer(Modifier.height(22.dp))

            NickNameCard(
                nickname = uiState.nickname,
                onDone = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                },
                onValueChange = {
                    viewModel.onNicknameChanged(it)
                }
            )

            Spacer(Modifier.height(18.dp))

            MoodSelector(
                modifier = Modifier.align(Alignment.Start),
                selectedMood = uiState.selectedMood ?: "",
                moods = moods,
                onValueChange = { viewModel.onMoodSelected(it.label) }
            )

            Spacer(Modifier.height(24.dp))


        if (uiState.isSaving) {
            SubmittingState()
        } else {
            BottomActionButton(
                onClick = {
                    viewModel.onSaveClicked(
                        onSuccess = { message = "Profile updated successfully" }
                    )
                },
                buttonEnabled = buttonEnabled,
                buttonAlpha = buttonAlpha,
                text = "Save changes ✨"
            )
        }
        }

        SnackBarMessage(
            modifier = Modifier.align(Alignment.BottomCenter),
            snackbarHostState = snackbarHostState)
        }
    }
}

@Composable
fun AvatarSection(
    avatar: String,
    onAvatarChange: () -> Unit,
    infiniteTransition: InfiniteTransition,
    floatY: Float
) {

    Box(modifier = Modifier.fillMaxWidth()){
        Box(
            modifier = Modifier.align(Alignment.Center),
            contentAlignment = Alignment.Center
        ) {
            val pulseAlpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 0.7f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "profilePulse"
            )

            Box(
                modifier = Modifier
                    .size(150.dp)
                    .background(
                        Color(0xFFA08CFF).copy(alpha = pulseAlpha),
                        CircleShape
                    )
                    .blur(45.dp)
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = avatar,
                    fontSize = 80.sp,
                    modifier = Modifier.offset(y = floatY.dp)
                )
            }
        }

        IconButton(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 25.dp),
            onClick = onAvatarChange
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = "refresh avatar")
        }
    }
}





