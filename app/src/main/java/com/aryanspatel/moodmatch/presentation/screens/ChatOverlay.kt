package com.aryanspatel.moodmatch.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aryanspatel.moodmatch.domain.usecases.isEmojiOnly
import com.aryanspatel.moodmatch.presentation.ConfirmationDialog
import com.aryanspatel.moodmatch.presentation.PartnerLeftDialog
import com.aryanspatel.moodmatch.presentation.models.FloatingEmoji
import com.aryanspatel.moodmatch.presentation.models.MatchUiState
import com.aryanspatel.moodmatch.presentation.models.Message
import com.aryanspatel.moodmatch.presentation.models.MessageType
import com.aryanspatel.moodmatch.presentation.models.MoodData
import com.aryanspatel.moodmatch.presentation.viewmodels.MatchViewModel

@Composable
fun ChatScreen(
    onLeave: () -> Unit,
    viewModel: MatchViewModel = hiltViewModel(),
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val session by viewModel.sessionState.collectAsStateWithLifecycle()
    val inRoom = uiState as? MatchUiState.InRoom
    val errorState = uiState as? MatchUiState.Error
    val isPartnerLeftError = errorState?.code == "PARTNER_LEFT"

    // Confirmation to leave chat
    var showLeaveChatConfirmationDialog by remember { mutableStateOf(false) }
    // Partner Left Informing
    var showPartnerLeftDialog by remember { mutableStateOf(false) }

    if(uiState is MatchUiState.Error && (uiState as MatchUiState.Error).code == "PARTNER_LEFT"){
        showPartnerLeftDialog = true
    }
    // When PARTNER_LEFT error appears, open dialog once
    LaunchedEffect(isPartnerLeftError) {
        if (isPartnerLeftError) {
            showPartnerLeftDialog = true
        }
    }

    // If not in room anymore AND it's not a partner-left case, just leave screen
    if ((inRoom == null || session == null) && !isPartnerLeftError) {
        LaunchedEffect(Unit) {
            onLeave()
        }
        return
    }
    
    val userMood = remember(session) {
        MoodData(
            nickname = session?.profile?.nickname ?: "",
            avatar = session?.profile?.avatar ?: "",
        )
    }
    val matchMood = remember(inRoom) {
        MoodData(
            nickname = inRoom?.partnerName ?: "",
            avatar = inRoom?.partnerAvatar ?: "",
        )
    }

    val uiMessages: List<Message> = remember(inRoom?.messages) { inRoom?.messages ?: emptyList()}

    val isPartnerTyping = inRoom?.partnerTyping ?: false
    var messageText by rememberSaveable { mutableStateOf("") }
    var floatingEmojis by remember { mutableStateOf<List<FloatingEmoji>>(emptyList()) }

    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current


    // auto scroll list according to keyboard state
    val density = LocalDensity.current
    val imeBottomPx = WindowInsets.ime.getBottom(density) // Int, in px
    var lastImeBottomPx by remember { mutableIntStateOf(0) }

    val brush: Brush = Brush.linearGradient(
        listOf(Color(0xFFFF7F6B),
            Color(0xFFFFD700),
            Color(0xFFA08CFF))
    )
    
    LaunchedEffect(imeBottomPx) {
        val diff = imeBottomPx - lastImeBottomPx
        if (diff > 0) { listState.scrollBy(diff.toFloat()) }
        if (diff < 0) listState.scrollBy(diff.toFloat())
        lastImeBottomPx = imeBottomPx
    }

    // Floating emojis (subtle in chat)
    LaunchedEffect(Unit) {
        val emojis = listOf(
            "✨", "💫", "🌟", "⭐", "💖", "🫰", "😈", "🎭", "☠️",
            "🚀", "🫦", "⚡️", "🍑", "😎", "👻", "🥵", "🤖", "👾️"
        )
        while (true) {
            delay(2500)
            val newEmoji = FloatingEmoji(
                id = System.currentTimeMillis(),
                emoji = emojis.random(),
                offsetX = Random.nextFloat(),
                startTime = System.currentTimeMillis()
            )
            floatingEmojis = (floatingEmojis.takeLast(4) + newEmoji)
        }
    }

    // Auto scroll
    LaunchedEffect(uiMessages.size, isPartnerTyping) {
        val hasMessage = uiMessages.isNotEmpty()
        val lastIndex = when {
            isPartnerTyping -> uiMessages.size
            hasMessage -> uiMessages.size -1
            else -> return@LaunchedEffect
        }

        val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
        val totalItems = listState.layoutInfo.totalItemsCount
        val isNearBottom = lastVisibleIndex == null || lastVisibleIndex >= totalItems - 3

        if(isNearBottom){
            listState.animateScrollToItem(lastIndex)
        }
    }

    var sessionTime by remember { mutableIntStateOf(273) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            sessionTime++
        }
    }

    BackHandler(enabled = !showPartnerLeftDialog) {
        showLeaveChatConfirmationDialog = true
    }
    
    if (showPartnerLeftDialog){
        PartnerLeftDialog(
            title = "Your match left the chat!",
            message = "Your buddy left the chat, but this is not the last chapter. Let’s find a new character for your story 📖✨",
            onOk = {
                showPartnerLeftDialog = false
                viewModel.onConsumeMessage()
                viewModel.makeStateIdle()
                onLeave()
            },
            confirmButtonText = "OK",
            confirmButtonColor = Color(0xFF6C63FF)
        )
    }

    if(showLeaveChatConfirmationDialog){
        ConfirmationDialog(
            title = "End the match?",
            text = "Leaving now will end this match and close the chat for good. Stay if the vibe’s still alive.",
            confirmButtonLabel = "Yes",
            dismissButtonLabel = "No",
            confirmButtonColor = Color(0xFF6C63FF),
            onConfirm = {
                showLeaveChatConfirmationDialog = false
                viewModel.leaveChat()
                onLeave()
            },
            onDismiss = {showLeaveChatConfirmationDialog = false}
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Color.Transparent
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {

        // Floating emojis (very subtle)
        floatingEmojis.forEach { emoji ->
            FloatingEmojiView(emoji)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header (brand pill + little mood info)
            ChatHeader(
                userMood = userMood,
                matchMood = matchMood,
                sessionTime = sessionTime
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                focusManager.clearFocus() // this hides the keyboard
                            }
                        )
                    }
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                    items(uiMessages, key = { it.id }) { message ->
                        when (message.type) {
                            MessageType.EVENT -> EventMessageBubble(message)
                            MessageType.SENT -> SentMessageBubble(message)
                            MessageType.RECEIVED -> ReceivedMessageBubble(message)
                        }
                    }

                    if (isPartnerTyping) {
                        item(key = "typing_indicator") {
                            TypingIndicator()
                        }
                    }
                }

                InputArea(
                    messageText = messageText,
                    brush = brush,
                    onMessageChange = { text ->
                        messageText = text
                        viewModel.onMessageInputChanged(text)},
                    onSend = {
                        val trimmed = messageText.trim()
                        if (trimmed.isNotEmpty()) {
                            viewModel.sendMessage(trimmed)
                            viewModel.onMessageSent()
                            messageText = ""
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun InputArea(
    messageText: String,
    brush: Brush,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit
) {

    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Text field
            BasicTextField(
                value = messageText,
                onValueChange = onMessageChange,
                textStyle = LocalTextStyle.current.copy(
                    color = Color.Black,
                    fontSize = 18.sp
                ),
                cursorBrush = SolidColor(Color.Black),
                maxLines = 5,
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 40.dp)
                    .border(
                        brush = brush,
                        width = 1.dp,
                        shape = RoundedCornerShape(24.dp)
                    )
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.95f))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
            Spacer(modifier = Modifier.width(8.dp) )

            // Send button


            Box(
                modifier = Modifier
                    .clickable(enabled = messageText.isNotBlank()) { onSend() }
                    .size(36.dp)
                    .background(
                        brush = brush,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ChatHeader(
    userMood: MoodData,
    matchMood: MoodData,
    sessionTime: Int
) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 10.dp, start = 10.dp, end = 10.dp)

            ,
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfileSection(
                avatar = matchMood.avatar,
                name = matchMood.nickname,
                color = Color(0xFFFF7F6B)
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Surface(
                color = Color.White.copy(alpha = 0.7f),
                shape = RoundedCornerShape(999.dp),
                tonalElevation = 0.dp,
                shadowElevation = 6.dp
                ) {
                    Text(
                        text = "Matched · ${formatTime(sessionTime)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF4B5563),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            ProfileSection(
                avatar = userMood.avatar,
                name = "You",
                color = Color(0xFFA08CFF)
            )
        }
    }
}

@Composable
fun ProfileSection(avatar: String, name: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            // Avatar
            Surface(
                modifier = Modifier
                    .size(34.dp),
                shape = CircleShape,
                color = color,
                shadowElevation = 20.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = avatar, fontSize = 18.sp)
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = name ,
                color = Color.Black,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun FloatingEmojiView(emoji: FloatingEmoji) {
    val infiniteTransition = rememberInfiniteTransition(label = "float")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 400f,
        targetValue = -800f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "floatY"
    )
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Text(
        text = emoji.emoji,
        fontSize = 28.sp,
        modifier = Modifier
            .offset(x = (emoji.offsetX * 280).dp, y = offsetY.dp)
            .rotate(rotation)
            .alpha(0.18f)
    )
}

@Composable
fun EventMessageBubble(message: Message) {
    Box(
        modifier = Modifier
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color.White.copy(alpha = 0.85f),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Text(
                text = message.content,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF7C3AED),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
    }
}


@Composable
fun SentMessageBubble(message: Message) {
    val bg = Color(0xFFA08CFF)

    val tilt = remember(message.id) {
        when ((message.id.hashCode().toLong() % 5).toInt()) {
            0 -> -3f
            1 -> -1.5f
            2 -> 0f
            3 -> 2f
            else -> 3.5f
        }
    }

    val emojiOnly = remember(message.content) {
        message.content.isEmojiOnly()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.End
    ) {
        if (emojiOnly) {
            Text(
                text = message.content,
                fontSize = 36.sp,
                lineHeight = 36.sp,
                modifier = Modifier
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.graphicsLayer {
                    rotationZ = tilt
                }
            ) {
                // main label bubble
                Surface(
                    color = bg,
                    shape = RoundedCornerShape(6.dp),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .widthIn(max = 260.dp)
                        .border(
                            width = 1.dp,
                            color = Color(0x33000000),
                            shape = RoundedCornerShape(6.dp)
                        )
                ) {
                    Text(
                        text = message.content,
                        color = Color(0xFF111827),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                // little flag tail on the right
                TagTail(color = bg)
            }
        }
    }
}

@Composable
fun ReceivedMessageBubble(message: Message) {
    val bg = Color(0xFFFF7F6B).copy(0.4f)
    val tilt = remember(message.id) {
        when ((message.id.hashCode().toLong() % 5).toInt()) {
            0 -> 3.5f
            1 -> 2f
            2 -> 0f
            3 -> -1.5f
            else -> -3f
        }
    }

    val emojiOnly = remember(message.content) {
        message.content.isEmojiOnly()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        if (emojiOnly) {
            Text(
                text = message.content,
                fontSize = 36.sp,
                lineHeight = 36.sp,
                modifier = Modifier
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.graphicsLayer {
                    rotationZ = tilt
                }
            ) {
                TagTailLeft(color = bg)

                Surface(
                    color = bg,
                    shape = RoundedCornerShape(6.dp),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .widthIn(max = 260.dp)
                        .border(
                            width = 1.dp,
                            color = Color(0x33000000),
                            shape = RoundedCornerShape(6.dp)
                        )
                ) {
                    Text(
                        text = message.content,
                        color = Color(0xFF111827),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TypingIndicator() {
    val bgColor = Color(0xFFFF7F6B).copy(0.4f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 2.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TagTailLeft(color = bgColor)

        Surface(
            color = bgColor,
            shape = RoundedCornerShape(6.dp),
            shadowElevation = 6.dp,
            modifier = Modifier
                .wrapContentWidth()
                .border(
                    1.dp,
                    Color(0x33000000),
                    RoundedCornerShape(6.dp)
                )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 15.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    val infiniteTransition =
                        rememberInfiniteTransition(label = "typingDot$index")
                    val offsetY by infiniteTransition.animateFloat(
                        initialValue = 4f,
                        targetValue = -2f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(
                                durationMillis = 450,
                                delayMillis = index * 140
                            ),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "typingOffset$index"
                    )

                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .offset(y = offsetY.dp)
                            .background(Color.White, CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
private fun TagTail(color: Color, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .width(10.dp)
            .height(18.dp)
    ) {
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, size.height / 2f)
            lineTo(0f, size.height)
            close()
        }
        drawPath(path = path, color = color)
        // optional tiny border to match sticker vibe
        val borderPath = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, size.height / 2f)
            lineTo(0f, size.height)
        }
        drawPath(
            path = borderPath,
            color = Color(0x33000000)
        )
    }
}

@Composable
private fun TagTailLeft(color: Color, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .width(10.dp)
            .height(18.dp)
    ) {
        val path = Path().apply {
            moveTo(size.width, 0f)
            lineTo(0f, size.height / 2f)
            lineTo(size.width, size.height)
            close()
        }
        drawPath(path = path, color = color)

        val borderPath = Path().apply {
            moveTo(size.width, 0f)
            lineTo(0f, size.height / 2f)
            lineTo(size.width, size.height)
        }
        drawPath(
            path = borderPath,
            color = Color(0x33000000)
        )
    }
}

private fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "${m}m ${s.toString().padStart(2, '0')}s"
}