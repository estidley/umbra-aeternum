package com.estidley.umbra

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.delay

@Composable
fun RollAskOverlay(prompt: RollPrompt, onPick: (String) -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xB3000000))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {},
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .padding(24.dp)
                .widthIn(max = 360.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF171B24))
                .border(1.dp, Color(0xFF2A2F3D), RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(prompt.name, color = Color(0xFFE8EAF0), fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
            if (prompt.purpose.isNotBlank()) {
                Text(prompt.purpose, color = Color(0xFFA3A9BA), fontSize = 14.sp)
            }
            RollChoice("Normal") { onPick("normal") }
            RollChoice("Advantage") { onPick("advantage") }
            RollChoice("Disadvantage") { onPick("disadvantage") }
        }
    }
}

@Composable
private fun RollChoice(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF2A2F3D))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color(0xFFE8EAF0), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun EmberDieFade(token: Long, caption: String) {
    val fade = remember { Animatable(0f) }
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(token) {
        if (token == 0L) {
            shown = false
            fade.snapTo(0f)
            return@LaunchedEffect
        }
        shown = true
        fade.snapTo(1f)
        delay(2000)
        fade.animateTo(0f, tween(durationMillis = 800, easing = LinearEasing))
        shown = false
    }
    if (!shown) return
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer { this.alpha = fade.value },
        ) {
            EmberIcosahedron(Modifier.size(210.dp))
            if (caption.isNotBlank()) {
                Text(
                    caption,
                    color = Color(0xFFE8EAF0),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }
        }
    }
}

@Composable
private fun EmberIcosahedron(modifier: Modifier) {
    val spin = rememberInfiniteTransition(label = "ember-die")
    val yaw by spin.animateFloat(
        initialValue = 0f,
        targetValue = (PI * 2.0).toFloat(),
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = 1800, easing = LinearEasing)),
        label = "yaw",
    )
    Canvas(modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val scale = size.minDimension * 0.36f
        val projected = Icosahedron.vertices.map { projectVertex(it, yaw, yaw * 0.37f) }
        val order = Icosahedron.faces.indices.sortedBy { index ->
            val face = Icosahedron.faces[index]
            projected[face[0]].z + projected[face[1]].z + projected[face[2]].z
        }
        for (index in order) {
            val face = Icosahedron.faces[index]
            val a = projected[face[0]]
            val b = projected[face[1]]
            val c = projected[face[2]]
            val ux = b.x - a.x
            val uy = b.y - a.y
            val uz = b.z - a.z
            val vx = c.x - a.x
            val vy = c.y - a.y
            val vz = c.z - a.z
            val nx = uy * vz - uz * vy
            val ny = uz * vx - ux * vz
            val nz = ux * vy - uy * vx
            val len = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(0.0001f)
            val facing = (nz / len).coerceIn(-1f, 1f)
            val light = (facing * 0.5f + 0.5f).coerceIn(0f, 1f)
            val path = Path()
            path.moveTo(cx + a.x * scale, cy - a.y * scale)
            path.lineTo(cx + b.x * scale, cy - b.y * scale)
            path.lineTo(cx + c.x * scale, cy - c.y * scale)
            path.close()
            drawPath(path, emberShade(light))
        }
    }
}

private fun emberShade(light: Float): Color {
    val body = 0.28f + 0.72f * light
    val highlight = ((light - 0.78f) / 0.22f).coerceIn(0f, 1f)
    fun mix(base: Int, hi: Int): Float {
        val shaded = base / 255f * body
        val hot = hi / 255f
        return (shaded + (hot - shaded) * highlight * 0.85f).coerceIn(0f, 1f)
    }
    return Color(red = mix(0xD9, 0xF6), green = mix(0x7A, 0xB4), blue = mix(0x2B, 0x7E))
}

private data class Vec3(val x: Float, val y: Float, val z: Float)

private fun projectVertex(v: Vec3, yaw: Float, pitch: Float): Vec3 {
    val cy = cos(yaw)
    val sy = sin(yaw)
    val x1 = v.x * cy + v.z * sy
    val z1 = -v.x * sy + v.z * cy
    val cx = cos(pitch)
    val sx = sin(pitch)
    val y2 = v.y * cx - z1 * sx
    val z2 = v.y * sx + z1 * cx
    val distance = 3.4f
    val perspective = distance / (distance + z2)
    return Vec3(x1 * perspective, y2 * perspective, z2)
}

private object Icosahedron {
    private const val PHI = 1.6180339887f

    val vertices: List<Vec3> = listOf(
        Vec3(-1f, PHI, 0f),
        Vec3(1f, PHI, 0f),
        Vec3(-1f, -PHI, 0f),
        Vec3(1f, -PHI, 0f),
        Vec3(0f, -1f, PHI),
        Vec3(0f, 1f, PHI),
        Vec3(0f, -1f, -PHI),
        Vec3(0f, 1f, -PHI),
        Vec3(PHI, 0f, -1f),
        Vec3(PHI, 0f, 1f),
        Vec3(-PHI, 0f, -1f),
        Vec3(-PHI, 0f, 1f),
    ).map { v ->
        val len = sqrt(v.x * v.x + v.y * v.y + v.z * v.z)
        Vec3(v.x / len, v.y / len, v.z / len)
    }

    val faces: List<IntArray> = listOf(
        intArrayOf(0, 11, 5),
        intArrayOf(0, 5, 1),
        intArrayOf(0, 1, 7),
        intArrayOf(0, 7, 10),
        intArrayOf(0, 10, 11),
        intArrayOf(1, 5, 9),
        intArrayOf(5, 11, 4),
        intArrayOf(11, 10, 2),
        intArrayOf(10, 7, 6),
        intArrayOf(7, 1, 8),
        intArrayOf(3, 9, 4),
        intArrayOf(3, 4, 2),
        intArrayOf(3, 2, 6),
        intArrayOf(3, 6, 8),
        intArrayOf(3, 8, 9),
        intArrayOf(4, 9, 5),
        intArrayOf(2, 4, 11),
        intArrayOf(6, 2, 10),
        intArrayOf(8, 6, 7),
        intArrayOf(9, 8, 1),
    )
}
