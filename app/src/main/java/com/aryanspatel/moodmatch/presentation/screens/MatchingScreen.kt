package com.aryanspatel.moodmatch.presentation.screens

import android.util.Log
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aryanspatel.moodmatch.R
import com.aryanspatel.moodmatch.presentation.SnackBarMessage
import com.aryanspatel.moodmatch.presentation.models.MatchUiState
import com.aryanspatel.moodmatch.presentation.viewmodels.MatchViewModel
import kotlinx.coroutines.delay

@Composable
fun MoodMatchScreen(
    viewModel: MatchViewModel = hiltViewModel(),
    onMatchFound: () -> Unit
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val session by viewModel.sessionState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showProfileOverlay by remember { mutableStateOf(false) }

    if (uiState is MatchUiState.InRoom) {
        onMatchFound()
        return
    }

    // ---- STATE ----
    val isMatching = uiState is MatchUiState.Queueing
    val errorMessage = (uiState as? MatchUiState.Error)?.message
    var afterChat by remember { mutableStateOf(false) }
    var previousState by remember { mutableStateOf<MatchUiState>(MatchUiState.Idle) }

    // Detect "returning from chat"
    LaunchedEffect(uiState) {
        if (previousState is MatchUiState.InRoom && uiState is MatchUiState.Idle) {
            afterChat = true
        }
        previousState = uiState
    }

    var statusText by remember { mutableStateOf("") }
    val statusMessages = remember {
        listOf(
            "🔮 Connecting to MoodSpace...",
            "✨ Scanning for similar vibes...",
            "💫 Checking who's available...",
            "🌈 Finding your vibe twin..."
        )
    }

    // Cycle status messages while matching
    LaunchedEffect(isMatching) {
        if (isMatching) {
            var index = 0
            while (true) {
                statusText = statusMessages[index]
                index = (index + 1) % statusMessages.size
                delay(2000)
            }
        } else {
            statusText = ""
        }
    }

    LaunchedEffect(errorMessage) {
        if (!errorMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(
                message = errorMessage,
                withDismissAction = true,
                duration = SnackbarDuration.Short
            )
            viewModel.onConsumeMessage()
        }
    }

    fun startMatch() {
        afterChat = false
        if(session == null) {
            Log.d("SessionError", "startMatch: session is null")
        }
        session?.let { viewModel.joinQueue(it.profile.mood) }
    }

    fun cancelMatch() {
        viewModel.leaveQueue()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {

        StickersLayer()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            MoodMatchHeader(onProfileClick = { showProfileOverlay = true })

            // Center mascot & bubble
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CenterMascotAndBubble(
                    isMatching = isMatching,
                    afterChat = afterChat,
                    statusText = statusText
                )
            }

            // Bottom actions
            BottomPanel(
                isMatching = isMatching,
                onStart = { startMatch() },
                onCancel = { cancelMatch() },
            )
        }

        SnackBarMessage(
            modifier = Modifier.align(Alignment.BottomCenter),
            snackbarHostState = snackbarHostState)
    }

    if(showProfileOverlay){
        ProfileScreen(
            onDismiss = {
                showProfileOverlay = false
            }
        )
    }
}

@Composable
private fun BottomPanel(
    isMatching: Boolean,
    onStart: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // main action
        if (!isMatching) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(
                                Color(0xFF7C3AED),
                                Color(0xFFEC4899)
                            )
                        )
                    )
                    .clickable { onStart() },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Bolt,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Start Matching! ✨",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(
                                Color(0xFFEF4444),
                                Color(0xFFF97316)
                            )
                        )
                    )
                    .clickable { onCancel() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Stop Searching",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}


@Composable
private fun CenterMascotAndBubble(
    isMatching: Boolean,
    afterChat: Boolean,
    statusText: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CenterEmoji(isMatching = isMatching)

        Spacer(modifier = Modifier.height(12.dp))

        SpeechBubble(
            isMatching = isMatching,
            afterChat = afterChat,
            statusText = statusText
        )
    }
}

@Composable
private fun CenterEmoji(
    isMatching: Boolean
) {
    val transition = rememberInfiniteTransition(label = "centerEmoji")

    val offsetY by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (isMatching) -30f else -15f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isMatching) 1000 else 3000,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emojiOffsetY"
    )

    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (isMatching) 1.1f else 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isMatching) 1000 else 3000,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emojiScale"
    )

    Box(
        modifier = Modifier
            .size(220.dp),
        contentAlignment = Alignment.Center
    ) {
        // Glow
        Box(
            modifier = Modifier
                .size(180.dp)
                .graphicsLayer {
                    alpha = if (isMatching) 0.8f else 0.5f
                }
                .background(
                    Color.Transparent,
                    shape = CircleShape
                )
        )

        // Orbiting mini emojis when matching
        if (isMatching) {
            val orbitEmojis = listOf("💫", "✨", "⭐", "🌟")
            orbitEmojis.forEachIndexed { index, e ->
                OrbitEmoji(
                    emoji = e,
                    radius = 80 + index * 8,
                    baseDuration = 2000 + index * 300,
                    delay = index * 300
                )
            }
        }

        Image(
            painter = painterResource(R.drawable.app_logo),
            contentDescription = "app logo",
            modifier=  Modifier
                .size(150.dp)
                .graphicsLayer {
                    translationY = offsetY
                    scaleX = scale
                    scaleY = scale
                }
        )

    }
}

@Composable
private fun OrbitEmoji(
    emoji: String,
    radius: Int,
    baseDuration: Int,
    delay: Int
) {
    val transition = rememberInfiniteTransition(label = "orbit")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(baseDuration, delayMillis = delay, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbitAngle"
    )

    Box(
        modifier = Modifier
            .size(160.dp)
            .graphicsLayer {
                rotationZ = angle
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emoji,
            fontSize = 20.sp,
            modifier = Modifier.offset(x = radius.dp)
        )
    }
}

@Composable
private fun SpeechBubble(
    isMatching: Boolean,
    afterChat: Boolean,
    statusText: String
) {
    Box(
        modifier = Modifier
            .wrapContentWidth()
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 10.dp,
            shadowElevation = 10.dp,
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!isMatching && !afterChat) {
                    Text(
                        text = "Hey there! 👋",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF1F2933),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = "Ready to find someone who vibes with your mood energy?",
                        fontSize = 16.sp,
                        color = Color(0xFF4B5563),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text("❤️", fontSize = 14.sp)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Match based on mood, not looks",
                            fontSize = 13.sp,
                            color = Color(0xFF6B7280)
                        )
                    }
                }

                if (!isMatching && afterChat) {
                    Text(
                        text = "That was fun! 🎉",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF1F2933),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = "Want to connect with another vibe-matcher?",
                        fontSize = 16.sp,
                        color = Color(0xFF4B5563),
                        textAlign = TextAlign.Center
                    )
                }

                if (isMatching) {
                    Text(
                        text = "Matching you up! 🎯",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF7C3AED),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = "Looking for someone who feels mood vibes too...",
                        fontSize = 16.sp,
                        color = Color(0xFF4B5563),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))

                    // bouncing dots
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(0, 1, 2).forEach { idx ->
                            BouncingDot(delayMillis = idx * 200)
                            if (idx != 2) Spacer(Modifier.width(6.dp))
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Color(0xFFF5F3FF),
                                RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = statusText,
                            fontSize = 13.sp,
                            color = Color(0xFF5B21B6),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BouncingDot(delayMillis: Int) {
    val transition = rememberInfiniteTransition(label = "dot")
    val scale by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, delayMillis = delayMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotScale"
    )
    Box(
        modifier = Modifier
            .size(10.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .background(Color(0xFF8B5CF6), CircleShape)
    )
}

@Composable
private fun MoodMatchHeader(onProfileClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(top = 10.dp, bottom = 8.dp)
    ) {

        Box(
            modifier = Modifier
                .shadow(8.dp, RoundedCornerShape(999.dp))
                .background(Color.White, RoundedCornerShape(999.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .align(Alignment.Center),
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
                    color = Color(0xFF1F2933)
                )
            }
        }

        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.White)
                .clickable { onProfileClick() }
                .align(Alignment.CenterEnd),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = "Profile",
                tint = Color(0xFF7C3AED),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun StickersLayer() {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        // -------- TOP CROWN AREA (~0.03–0.20) --------
        // 1–10
        EmojiSticker("🌈", xFraction = 0.08f, yFraction = 0.04f, rotation = 18f, alpha = 0.9f, sizeSp = 32) // 1
        EmojiSticker("💫", xFraction = 0.50f, yFraction = 0.03f, rotation = -15f, alpha = 0.85f, sizeSp = 30) // 2
        EmojiSticker("🎉", xFraction = 0.90f, yFraction = 0.05f, rotation = 10f, alpha = 0.9f, sizeSp = 30) // 3
        EmojiSticker("😜", xFraction = 0.02f, yFraction = 0.10f, rotation = -25f, alpha = 0.9f, sizeSp = 32) // 4
        EmojiSticker("👯", xFraction = 0.80f, yFraction = 0.11f, rotation = 16f, alpha = 0.9f, sizeSp = 32) // 5
        EmojiSticker("💖", xFraction = 0.30f, yFraction = 0.12f, rotation = 35f, alpha = 0.85f, sizeSp = 30) // 6
        EmojiSticker("🫧", xFraction = 0.63f, yFraction = 0.09f, rotation = -30f, alpha = 0.75f, sizeSp = 26) // 7
        EmojiSticker("✨", xFraction = 0.20f, yFraction = 0.17f, rotation = -12f, alpha = 0.85f, sizeSp = 26) // 8
        EmojiSticker("🎀", xFraction = 0.72f, yFraction = 0.18f, rotation = 18f, alpha = 0.8f, sizeSp = 26) // 9
        EmojiSticker("💌", xFraction = 0.46f, yFraction = 0.17f, rotation = 8f, alpha = 0.8f, sizeSp = 26) // 10

        TagSticker("good vibes only ✨", xFraction = 0.30f, yFraction = 0.20f, rotation = -4f)
        TagSticker("energy > looks 💯", xFraction = 0.62f, yFraction = 0.22f, rotation = 3f)

        // -------- UPPER-MID (~0.24–0.40) --------
        // 11–16
        EmojiSticker("⭐", xFraction = 0.10f, yFraction = 0.26f, rotation = -8f, alpha = 0.8f, sizeSp = 26) // 11
        EmojiSticker("🎵", xFraction = 0.85f, yFraction = 0.25f, rotation = 14f, alpha = 0.8f, sizeSp = 24) // 12
        EmojiSticker("😎", xFraction = 0.32f, yFraction = 0.28f, rotation = -16f, alpha = 0.85f, sizeSp = 28) // 13
        EmojiSticker("🔥", xFraction = 0.60f, yFraction = 0.29f, rotation = 10f, alpha = 0.9f, sizeSp = 28) // 14
        EmojiSticker("☁️", xFraction = 0.18f, yFraction = 0.34f, rotation = 6f, alpha = 0.8f, sizeSp = 28) // 15
        EmojiSticker("🎭", xFraction = 0.78f, yFraction = 0.35f, rotation = -18f, alpha = 0.8f, sizeSp = 28) // 16

        TagSticker("real talks only 🗣️", xFraction = 0.52f, yFraction = 0.32f, rotation = 2f)
        TagSticker("vibe check! 🎯", xFraction = 0.06f, yFraction = 0.28f, rotation = -5f)

        // -------- MID (~0.42–0.58) --------
        // 17–22
        EmojiSticker("☀️", xFraction = 0.50f, yFraction = 0.42f, rotation = -5f, alpha = 0.7f, sizeSp = 30) // 17
        EmojiSticker("🌸", xFraction = 0.12f, yFraction = 0.46f, rotation = -14f, alpha = 0.7f, sizeSp = 30) // 18
        EmojiSticker("🎨", xFraction = 0.88f, yFraction = 0.44f, rotation = -20f, alpha = 0.75f, sizeSp = 30) // 19
        EmojiSticker("🦋", xFraction = 0.40f, yFraction = 0.52f, rotation = 10f, alpha = 0.8f, sizeSp = 30) // 20
        EmojiSticker("🎪", xFraction = 0.16f, yFraction = 0.56f, rotation = 16f, alpha = 0.8f, sizeSp = 28) // 21
        EmojiSticker("💬", xFraction = 0.82f, yFraction = 0.54f, rotation = -12f, alpha = 0.8f, sizeSp = 26) // 22

        TagSticker("stay authentic 🌟", xFraction = 0.20f, yFraction = 0.40f)
        TagSticker("let's vibe! ✌️", xFraction = 0.64f, yFraction = 0.46f)
        TagSticker("you got this! 💪", xFraction = 0.78f, yFraction = 0.50f)
        TagSticker("spread love 💚", xFraction = 0.22f, yFraction = 0.58f)

        // -------- LOWER-MID (~0.60–0.78) --------
        // 23–27
        EmojiSticker("🌙", xFraction = 0.10f, yFraction = 0.62f, rotation = 18f, alpha = 0.8f, sizeSp = 32) // 23
        EmojiSticker("🎈", xFraction = 0.84f, yFraction = 0.60f, rotation = 10f, alpha = 0.85f, sizeSp = 30) // 24
        EmojiSticker("🌺", xFraction = 0.18f, yFraction = 0.68f, rotation = -16f, alpha = 0.8f, sizeSp = 30) // 25
        EmojiSticker("👀", xFraction = 0.88f, yFraction = 0.70f, rotation = -8f, alpha = 0.75f, sizeSp = 30) // 26
        EmojiSticker("🫶", xFraction = 0.32f, yFraction = 0.72f, rotation = 12f, alpha = 0.8f, sizeSp = 28) // 27
        EmojiSticker("✉️", xFraction = 0.60f, yFraction = 0.74f, rotation = -20f, alpha = 0.8f, sizeSp = 26) // 28

        TagSticker("mood twins! 👯", xFraction = 0.68f, yFraction = 0.62f, rotation = 10f)
        TagSticker("same energy 🔋", xFraction = 0.54f, yFraction = 0.66f, rotation = 10f)
        TagSticker("hey there cutie 😊", xFraction = 0.26f, yFraction = 0.76f, rotation = 10f)
        TagSticker("match my freak 😜", xFraction = 0.72f, yFraction = 0.75f, rotation = -30f)

        // -------- BOTTOM (~0.80–0.97) --------
        // 29–30
        EmojiSticker("🎁", xFraction = 0.14f, yFraction = 0.86f, rotation = 10f, alpha = 0.7f, sizeSp = 28) // 29
        EmojiSticker("💘", xFraction = 0.90f, yFraction = 0.90f, rotation = -12f, alpha = 0.8f, sizeSp = 32) // 30
        EmojiSticker("👀", xFraction = 0.80f, yFraction = 0.85f, rotation = -8f, alpha = 0.75f, sizeSp = 30) // 26


        TagSticker("can we? 👉👈", xFraction = 0.50f, yFraction = 0.86f, rotation = 10f)
        TagSticker("slide into my DMs 📱", xFraction = 0.64f, yFraction = 0.81f, rotation = 0f)
        TagSticker("feeling u already 😌", xFraction = 0.20f, yFraction = 0.82f, rotation = 4f)
        TagSticker("you up? 🌃", xFraction = 0.02f, yFraction = 0.73f, rotation = -5f)
    }
}

@Composable
private fun BoxWithConstraintsScope.EmojiSticker(
    emoji: String,
    xFraction: Float,
    yFraction: Float,
    rotation: Float = 0f,
    alpha: Float = 0.8f,
    sizeSp: Int = 32
) {
    val x = maxWidth * xFraction
    val y = maxHeight * yFraction

    Text(
        text = emoji,
        fontSize = sizeSp.sp,
        modifier = Modifier
            .offset(x = x, y = y)
            .graphicsLayer {
                rotationZ = rotation
                this.alpha = alpha
            }
    )
}

@Composable
private fun BoxWithConstraintsScope.TagSticker(
    text: String,
    xFraction: Float,
    yFraction: Float,
    rotation: Float = 0f
) {
    val x = maxWidth * xFraction
    val y = maxHeight * yFraction

    Box(
        modifier = Modifier
            .offset(x = x, y = y)
            .graphicsLayer {
                rotationZ = rotation
            }
            .shadow(6.dp, RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = Color.Black.copy(0.6f),
            fontWeight = FontWeight.Black,
            fontSize = 11.sp
        )
    }
}