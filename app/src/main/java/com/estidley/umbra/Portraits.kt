package com.estidley.umbra

import android.content.res.AssetManager
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SpeakerPortrait(speakerId: String, speakerName: String, explicit: String?, umbra: Boolean) {
    val context = LocalContext.current
    val resolved = remember(speakerId, speakerName, explicit, umbra) {
        resolvePortrait(context.assets, speakerId, speakerName, explicit, umbra)
    }
    var bitmap by remember(resolved) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(resolved) {
        bitmap = null
        val path = resolved ?: return@LaunchedEffect
        bitmap = withContext(Dispatchers.IO) { decodePortrait(context.assets, path) }
    }
    val frame = Modifier.size(36.dp).clip(CircleShape).border(1.dp, Color(0xFF3A4154), CircleShape)
    val image = bitmap
    if (image != null) {
        Image(image, contentDescription = speakerName.ifBlank { speakerId.ifBlank { null } }, modifier = frame, contentScale = ContentScale.Crop)
    } else {
        Box(frame)
    }
}

private fun decodePortrait(assets: AssetManager, path: String): ImageBitmap? {
    return try {
        val bitmap = if (path.startsWith("http://") || path.startsWith("https://")) {
            URL(path).openStream().use { stream -> BitmapFactory.decodeStream(stream) }
        } else {
            assets.open(path).use { stream -> BitmapFactory.decodeStream(stream) }
        }
        bitmap?.asImageBitmap()
    } catch (_: Exception) {
        null
    }
}

fun resolvePortrait(assets: AssetManager, speakerId: String, speakerName: String, explicit: String?, umbra: Boolean): String? {
    fun exists(path: String): Boolean {
        return try {
            assets.open(path).close()
            true
        } catch (_: Exception) {
            false
        }
    }
    if (!explicit.isNullOrBlank()) {
        val raw = explicit.trim()
        if (raw.startsWith("http://") || raw.startsWith("https://")) return raw
        val candidates = linkedSetOf<String>()
        val trimmed = raw.removePrefix("/")
        candidates += trimmed
        candidates += "portraits/$trimmed"
        val stem = trimmed.substringAfterLast("/").substringBeforeLast(".")
        if (stem.isNotBlank()) {
            for (ext in listOf("jpg", "jpeg", "png", "webp")) candidates += "portraits/$stem.$ext"
        }
        for (path in candidates) if (exists(path)) return path
    }
    val key = if (umbra) "umbra" else speakerId.trim().lowercase().ifBlank { speakerName.trim().lowercase() }
    if (key.isBlank() || key == "you" || key == "player" || key == "narration" || key == "error") return null
    for (ext in listOf("jpg", "jpeg", "png", "webp")) {
        val path = "portraits/$key.$ext"
        if (exists(path)) return path
    }
    return null
}