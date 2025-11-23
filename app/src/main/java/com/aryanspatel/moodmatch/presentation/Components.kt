package com.aryanspatel.moodmatch.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.aryanspatel.moodmatch.presentation.models.Mood
import com.aryanspatel.moodmatch.presentation.screens.MoodBadge
import kotlinx.coroutines.delay

@Composable
fun HorizontalSlidingOverlay(
    modifier: Modifier = Modifier,
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    var visibleInternal by remember { mutableStateOf(false) }
    var triggerDismiss by remember { mutableStateOf(false) }

    // Show animation on entry
    LaunchedEffect(Unit) { visibleInternal = true }

    // Animate exit before removing from composition
    LaunchedEffect(triggerDismiss) {
        if (triggerDismiss) {
            delay(200)
            onDismiss()
            triggerDismiss = false
        }
    }

    val cardOffset by animateDpAsState(
        targetValue = if (visibleInternal) 0.dp else 1000.dp,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "card_slide"
    )

    val overlayAlpha by animateFloatAsState(
        targetValue = if (visibleInternal) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "overlay_alpha"
    )

    fun animateDismiss() {
        visibleInternal = false
        triggerDismiss = true
    }

    // Handle system back button
    BackHandler { animateDismiss() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = overlayAlpha * 0.2f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = visibleInternal && !triggerDismiss
            ) { animateDismiss() }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .background(
                    Brush.linearGradient(listOf(Color(0xFFE8F4F8), Color(0xFFF5E8F8)))
                )
                .align(Alignment.BottomCenter)
                .offset(x = cardOffset)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* Prevent dismiss on card click */ },
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                // Reusable header with back button + title
                HorizontalSlidingOverlayHeader(
                    isFullScreen = true,
                    title = title,
                    onBackClick = { animateDismiss() }
                )
                content()
            }
        }
    }
}

/**
 * Header for overlay Screen
 */
@Composable
fun HorizontalSlidingOverlayHeader(
    title: String,
    isFullScreen: Boolean = false,
    onBackClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = if (isFullScreen) 16.dp else 0.dp)
    ) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(30.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBackIosNew,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onPrimary)
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center),
            color = MaterialTheme.colorScheme.onPrimary)
    }
}

@Composable
fun SnackBarMessage(
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState) {

    SnackbarHost(
        hostState = snackbarHostState,
        modifier = modifier
            .zIndex(5f)
            .padding(16.dp)
            .navigationBarsPadding() // avoid system bars
    ) { data ->
        Snackbar(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = Color.White,
            snackbarData = data,
            shape = RoundedCornerShape(12.dp),
        )
    }
}

@Composable
fun NickNameCard(
    nickname: String,
    onDone: () -> Unit,
    onValueChange: (String) -> Unit
){
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color.White.copy(alpha = 0.96f),
        tonalElevation = 0.dp,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text = "Nickname",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF6B7280),
                modifier = Modifier.padding(bottom = 6.dp)
            )
            OutlinedTextField(
                value = nickname,
                onValueChange = {onValueChange(it)},
                placeholder = {
                    Text(
                        "What should we call you?",
                        color = Color(0xFF9CA3AF)
                    )
                },
                trailingIcon = {
                    Text("✨", fontSize = 22.sp)
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {onDone()}),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Color(0xFF1A1A1A),
                    unfocusedBorderColor = Color(0xFF1A1A1A).copy(alpha = 0.6f),
                    focusedTextColor = Color(0xFF111827),
                    unfocusedTextColor = Color(0xFF111827),
                    cursorColor = Color(0xFF1A1A1A)
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

@Composable
fun MoodSelector(
    modifier: Modifier,
    selectedMood: String,
    moods: List<Mood>,
    onValueChange: (Mood) -> Unit
){
    Text(
        text = "Choose your mood",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF061D46),
        modifier = modifier
            .padding(start = 6.dp, bottom = 8.dp)
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        moods.forEach { mood ->
            MoodBadge(
                mood = mood,
                isSelected = selectedMood.lowercase() == mood.label.lowercase(),
                onClick = { onValueChange(mood) },
                modifier = Modifier.weight(1f)
            )
        }
    }

}

@Composable
fun BottomActionButton(
    onClick: () -> Unit,
    buttonEnabled: Boolean,
    buttonAlpha: Float,
    text: String
){
    Button(
        onClick = onClick,
        enabled = buttonEnabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .graphicsLayer { alpha = buttonAlpha },
        shape = RoundedCornerShape(999.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent
        ),
        contentPadding = PaddingValues()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF7C3AED),
                            Color(0xFFEC4899)
                        )
                    ),
                    RoundedCornerShape(999.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun isKeyboardOpen(): Boolean {
    val ime = WindowInsets.ime
    return ime.getBottom(LocalDensity.current) > 0
}

@Composable
fun ConfirmationDialog(
    title: String,
    text: String,
    confirmButtonLabel: String,
    dismissButtonLabel: String,
    confirmButtonColor: Color = MaterialTheme.colorScheme.primaryFixed,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
){
    AlertDialog(
        containerColor = Color(0xFFF7FBFF),
        onDismissRequest = onDismiss,
        title = {
            Text(text = title,
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )},
        text = { Text(text, color = Color.Black.copy(0.8f)) },
        confirmButton = {
            TextButton(onClick = { onConfirm()}) {
                Text(text = confirmButtonLabel,
                    color = confirmButtonColor,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissButtonLabel,
                    color = Color.Black,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}

@Composable
fun PartnerLeftDialog(
    title: String,
    message: String,
    onOk: () -> Unit,
    confirmButtonText: String,
    confirmButtonColor: Color
) {
    AlertDialog(
        onDismissRequest = { onOk() },
        containerColor = Color(0xFFF7FBFF),
        title = {
            Text(text = title,
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )},
        text = { Text(message, color = Color.Black.copy(0.8f)) },
        confirmButton = {
            TextButton(onClick = { onOk() }) {
                Text(text = confirmButtonText,
                    color = confirmButtonColor,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
    )
}

@Composable
fun SubmittingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val spinTransition = rememberInfiniteTransition(label = "loading")
            val angle by spinTransition.animateFloat(
                initialValue = -10f,
                targetValue = 10f,
                animationSpec = infiniteRepeatable(
                    animation = tween(400, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "angle"
            )

            Text(
                text = "🔄",
                fontSize = 48.sp,
                modifier = Modifier.graphicsLayer {
                    rotationZ = angle
                }
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Saving your vibe...",
                color = Color(0xFF111827),
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(12.dp))

        }
    }
}