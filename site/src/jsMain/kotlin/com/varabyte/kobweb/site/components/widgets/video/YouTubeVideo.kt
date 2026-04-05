package com.varabyte.kobweb.site.components.widgets.video


import androidx.compose.runtime.*
import com.varabyte.kobweb.compose.foundation.layout.Box
import com.varabyte.kobweb.compose.ui.Alignment
import com.varabyte.kobweb.compose.ui.Modifier
import com.varabyte.kobweb.compose.ui.modifiers.fillMaxSize
import com.varabyte.kobweb.compose.ui.toAttrs
import com.varabyte.kobweb.silk.style.toModifier
import org.jetbrains.compose.web.dom.Iframe

/**
 * Youtube Video Player
 *
 * @param url The full YouTube URL to play (e.g. `https://www.youtube.com/watch?v=...`). If incorrectly formatted, no
 *   element will be composed and an error will be logged to the console.
 */
@Composable
fun YouTubeVideo(url: String) {
    val videoId = remember {
        extractVideoId(url)
            .also {
                if (it == null) {
                    console.error("Could not extract YouTube video ID from URL: $url")
                }
            }
    } ?: return

    Box(
        modifier = VideoPlayerStyle.toModifier(),
        contentAlignment = Alignment.CenterStart,
    ) {
        Iframe(
            attrs = Modifier.fillMaxSize().toAttrs {
                attr("src", "https://www.youtube.com/embed/$videoId")
            }
        )
    }
}

private fun extractVideoId(url: String): String? {
    if (!url.contains(".youtube.com")) return null
    val regex = Regex("v=([A-Za-z0-9_-]{11})")
    val matchResult = regex.find(url)
    return matchResult?.groups?.get(1)?.value
}