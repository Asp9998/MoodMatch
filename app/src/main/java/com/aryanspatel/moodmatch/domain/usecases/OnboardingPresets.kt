package com.aryanspatel.moodmatch.domain.usecases

import androidx.compose.ui.graphics.Color
import com.aryanspatel.moodmatch.presentation.models.AvailableMoods
import com.aryanspatel.moodmatch.presentation.models.Mood

object OnboardingPresets {

    // Avatar pools
    // Chill = relaxed / comfy / cozy vibes
    val chillAvatars = listOf(
        "👻", "🐼", "😉", "😎", "🌿", "✌️", "☕", "🎧", "🌙", "💤"
    )

    // Fire = competitive / intense / high-energy
    val fireAvatars = listOf(
        "🔥", "⚡️", "💥", "💣", "💪", "☠️", "🚀", "💀", "🐲", "😈"
    )

    // Silly = goofy / chaotic / meme energy
    val sillyAvatars = listOf(
        "🤓", "🤡", "😜", "🥱", "🙃", "🤟", "🙊", "🥳", "😺", "😂"
    )

    // Shy = soft / cute / wholesome
    val shyAvatars = listOf(
        "☺️", "❤️‍🩹", "😻", "🐱", "🐰", "🌸", "💗", "🎭", "🍡", "🫰"
    )

    // 5th mood = Wild – weird / edgy / out-there
    val wildAvatars = listOf(
        "🥵", "😈", "🫦", "🤘", "🍑", "🍆", "🍓", "🍒", "❤️‍🔥", "💦"
    )

    val allAvatars =
        chillAvatars + fireAvatars + sillyAvatars + shyAvatars + wildAvatars

    // What the UI will render
    val moods: List<Mood> = listOf(
        Mood("👻", AvailableMoods.Chill.name, Color(0xFF4FF4C9)),
        Mood("🔥", AvailableMoods.Fire.name, Color(0xFFFF7F6B)),
        Mood("🤓", AvailableMoods.Silly.name, Color(0xFFA08CFF)),
        Mood("☺️", AvailableMoods.Shy.name,  Color(0xFFDADA47)),
        Mood("😈", AvailableMoods.Wild.name, Color(0xFFEA3939))
    )

    fun avatarPoolForMood(mood: String?): List<String> =
        when (mood) {
            AvailableMoods.Chill.name  -> chillAvatars
            AvailableMoods.Fire.name   -> fireAvatars
            AvailableMoods.Silly.name  -> sillyAvatars
            AvailableMoods.Shy.name    -> shyAvatars
            AvailableMoods.Wild.name   -> wildAvatars
            else                       -> allAvatars
        }
}
