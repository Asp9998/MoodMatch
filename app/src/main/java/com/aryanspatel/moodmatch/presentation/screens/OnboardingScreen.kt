package com.aryanspatel.moodmatch.presentation.screens

import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aryanspatel.moodmatch.R
import com.aryanspatel.moodmatch.domain.usecases.OnboardingPresets
import com.aryanspatel.moodmatch.presentation.BottomActionButton
import com.aryanspatel.moodmatch.presentation.MoodSelector
import com.aryanspatel.moodmatch.presentation.NickNameCard
import com.aryanspatel.moodmatch.presentation.SnackBarMessage
import com.aryanspatel.moodmatch.presentation.SubmittingState
import com.aryanspatel.moodmatch.presentation.isKeyboardOpen
import com.aryanspatel.moodmatch.presentation.models.Mood
import com.aryanspatel.moodmatch.presentation.models.OnboardingError
import com.aryanspatel.moodmatch.presentation.viewmodels.OnboardingViewModel
import kotlin.math.sin

@Composable
fun OnboardingScreen(
    onStartMatchClick: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val moods = OnboardingPresets.moods
    val snackbarHostState = remember { SnackbarHostState() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val keyboardOpen = isKeyboardOpen()
    val scrollState = rememberScrollState()

    // floating animation
    val infiniteTransition = rememberInfiniteTransition(label = "float")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "float"
    )
    val floatY = sin(Math.toRadians(floatOffset.toDouble())).toFloat() * 8f

    val buttonEnabled =
        uiState.nickname.isNotEmpty() &&
                uiState.selectedMood != null &&
                !uiState.isSubmitting

    val buttonAlpha by animateFloatAsState(
        targetValue = if (buttonEnabled) 1f else 0.5f,
        label = "buttonAlpha"
    )

    // React to error changes
    LaunchedEffect(uiState.error) {
        val error = uiState.error
        if (error is OnboardingError.Message) {
            snackbarHostState.showSnackbar(error.text)
            viewModel.consumeError()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .background(Color.Transparent)
        ) {
             // a couple of soft blobs like matching screen
            Box(
                modifier = Modifier
                    .size(360.dp)
                    .align(Alignment.TopStart)
                    .offset(x = (-40).dp, y = 40.dp)
                    .background(Color(0xFF7C3AED).copy(alpha = 0.18f), CircleShape)
                    .blur(90.dp)
            )
            Box(
                modifier = Modifier
                    .size(360.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 40.dp, y = 40.dp)
                    .background(Color(0xFFFF7F6B).copy(alpha = 0.18f), CircleShape)
                    .blur(90.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (keyboardOpen) Modifier.verticalScroll(scrollState)
                        else Modifier
                    )
                    .imePadding()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(8.dp))

                HeaderPill()

                Spacer(Modifier.height(12.dp))

                AppLogo(floatY = floatY)

                Spacer(Modifier.height(12.dp))

                // nickname card
                NickNameCard(
                    nickname = uiState.nickname,
                    onDone = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                    },
                    onValueChange = { viewModel.onNicknameChanged(it) }
                )

                Spacer(Modifier.height(18.dp))

                MoodSelector(
                    modifier = Modifier.align(Alignment.Start),
                    selectedMood = uiState.selectedMood?.label ?: "",
                    moods = moods,
                    onValueChange = { viewModel.onMoodSelected(it) }
                )

                Spacer(Modifier.height(18.dp))

                AvatarCard(
                    infiniteTransition = infiniteTransition,
                    selectedAvatar = uiState.selectedAvatar,
                    onAvatarShuffle = { viewModel.onAvatarShuffle() }
                )

                Spacer(Modifier.height(22.dp))

                if (uiState.isSubmitting) {
                    SubmittingState()
                } else {
                    BottomActionButton(
                        onClick = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            viewModel.onContinueClicked(
                                onSuccess = onStartMatchClick,
                            )
                        },
                        buttonEnabled = buttonEnabled,
                        buttonAlpha = buttonAlpha,
                        text = "Continue"
                    )
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "Anonymous. Safe. Fun.",
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280),
                    letterSpacing = 1.2.sp,
                )

                Spacer(Modifier.height(24.dp))
            }

            SnackBarMessage(
                modifier = Modifier.align(Alignment.BottomCenter),
                snackbarHostState = snackbarHostState)
        }
    }
}

@Composable
fun AvatarCard(
    infiniteTransition: InfiniteTransition,
    selectedAvatar: String,
    onAvatarShuffle: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.96f),
        tonalElevation = 0.dp,
        shadowElevation = 10.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Your avatar",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF6B21A8),
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 6.dp)
                )

                val avatarPulse by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 0.6f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "avatarPulse"
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .background(
                                Color(0xFFA08CFF).copy(alpha = avatarPulse),
                                CircleShape
                            )
                            .blur(35.dp)
                    )
                    Text(
                        text = selectedAvatar,
                        fontSize = 70.sp
                    )
                }

            }

            IconButton(
                onClick = onAvatarShuffle,
                Modifier
                    .align(Alignment.TopEnd)
                    .size(40.dp)
            ) {
                Icon(Icons.Default.Refresh,
                    contentDescription = "refresh avatar",
                    tint = Color(0xFF7C3AED),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun AppLogo(
    floatY: Float
) {
    Box(
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .background(
                    Color(0xFF1A1A1A),
                    CircleShape
                )
                .blur(45.dp)
        )

        Image(
            painter = painterResource(R.drawable.app_logo),
            contentDescription = "app logo",
            modifier=  Modifier.size(130.dp).offset(y = floatY.dp)
        )
    }
}

@Composable
fun HeaderPill() {
    Box(
        modifier = Modifier
            .shadow(10.dp, RoundedCornerShape(999.dp))
            .background(Color.White, RoundedCornerShape(999.dp))
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = Color(0xFF7C3AED),
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "MoodMatch",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF111827)
            )
        }
    }
}


@Composable
fun MoodBadge(
    mood: Mood,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        label = "scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onClick)
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(
                            mood.moodColor.copy(alpha = 0.6f),
                            CircleShape
                        )
                        .blur(10.dp)
                )
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(54.dp)
                    .scale(scale)
                    .background(
                        if (isSelected) mood.moodColor else Color(0xFF1A1A20).copy(alpha = 0.8f),
                        CircleShape
                    )
            ) {
                Text(
                    text = mood.emoji,
                    fontSize = 30.sp
                )
            }
        }

        Text(
            text = mood.label,
            fontSize = 12.sp,
            color = Color(0xFF3B3D41),
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingScreenPreview() {
    OnboardingScreen(
        onStartMatchClick = {}
    )
}


