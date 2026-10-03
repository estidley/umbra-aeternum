package com.estidley.umbra

import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

data class ShownDie(
    val sides: Int,
    val value: Int,
    val label: String = value.toString(),
    val faceSet: String = "standard",
    val primary: Boolean = true,
)

data class DieShow(
    val id: Long,
    val dice: List<ShownDie>,
    val total: Int,
    val caption: String,
    val checkName: String? = null,
    val mode: String? = null,
    val expression: String? = null,
    val character: String? = null,
)

private val Ember = Color(0xFFD97A2B)
private val EmberInk = Color(0xFF1A120C)

@Composable
fun NumberedDiceOverlay(show: DieShow?, onConfirm: () -> Unit) {
    if (show == null) return
    val progress = remember(show.id) { Animatable(0f) }
    var settled by remember(show.id) { mutableStateOf(false) }
    val tumbles = remember(show.id) {
        val random = Random(show.id)
        show.dice.map { die -> tumbleFor(die, random) }
    }
    LaunchedEffect(show.id) {
        settled = false
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = 1700, easing = FastOutSlowInEasing))
        settled = true
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xB3000000))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .padding(20.dp)
                .widthIn(max = 420.dp)
                .background(Color(0xFF12151C), RoundedCornerShape(18.dp))
                .border(1.dp, Color(0xFF2A2F3D), RoundedCornerShape(18.dp))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val dieSize = if (show.dice.size > 2) 108.dp else 168.dp
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (show.dice.isEmpty()) {
                    Text(show.total.toString(), color = Color(0xFFE8EAF0), fontSize = 42.sp, fontWeight = FontWeight.Bold)
                }
                show.dice.forEachIndexed { index, die ->
                    val tumble = tumbles.getOrNull(index)
                    Box(
                        Modifier
                            .size(dieSize)
                            .then(if (die.primary) Modifier.border(2.dp, Ember, RoundedCornerShape(12.dp)) else Modifier),
                    ) {
                        if (tumble != null) {
                            Canvas(Modifier.fillMaxSize().padding(6.dp)) {
                                drawNumberedDie(die, pose(tumble, progress.value))
                            }
                        }
                    }
                }
            }
            if (settled) {
                Text(show.total.toString(), color = Color(0xFFF6B47E), fontSize = 40.sp, fontWeight = FontWeight.Bold)
                if (show.caption.isNotBlank()) {
                    Text(show.caption, color = Color(0xFFE8EAF0), fontSize = 14.sp, textAlign = TextAlign.Center)
                }
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = Ember, contentColor = EmberInk),
                ) { Text("OK", fontWeight = FontWeight.Bold) }
            } else {
                Text("Rolling", color = Color(0xFFA3A9BA), fontSize = 14.sp)
            }
        }
    }
}
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawNumberedDie(die: ShownDie, rotation: Quat) {
    val mesh = DiceMeshes.mesh(die)
    val cx = size.width / 2f
    val cy = size.height / 2f
    val scale = size.minDimension * 0.34f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(0x1A, 0x12, 0x0C)
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    val drawn = mesh.faces.map { face ->
        val verts = face.verts.map { rotation.rotate(it) }
        FaceDraw(verts, faceNormal(verts), face.label)
    }.sortedBy { centroid(it.verts).z }
    for (face in drawn) {
        val projected = face.verts.map { project(it, cx, cy, scale) }
        val path = Path()
        projected.forEachIndexed { i, point ->
            if (i == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
        }
        path.close()
        val light = (face.normal.z.coerceIn(-1f, 1f) * 0.5f + 0.5f).coerceIn(0f, 1f)
        drawPath(path, emberShade(light))
        drawPath(path, Color(0xFF3A2412), style = Stroke(width = 2f))
        if (face.normal.z > 0.12f) {
            val center = project(centroid(face.verts), cx, cy, scale)
            val depth = (4.2f - centroid(face.verts).z).coerceAtLeast(0.6f)
            paint.textSize = (scale * 0.28f * (4.2f / depth)).coerceIn(16f, 54f)
            drawContext.canvas.nativeCanvas.drawText(
                face.label,
                center.x,
                center.y - (paint.ascent() + paint.descent()) / 2f,
                paint,
            )
        }
    }
}

private data class FaceDraw(val verts: List<Vec3>, val normal: Vec3, val label: String)

private fun project(v: Vec3, cx: Float, cy: Float, scale: Float): Offset {
    val distance = 4.2f
    val perspective = distance / (distance - v.z).coerceAtLeast(0.45f)
    return Offset(cx + v.x * scale * perspective, cy - v.y * scale * perspective)
}

private fun emberShade(light: Float): Color {
    val body = 0.35f + 0.65f * light
    fun channel(base: Int): Float = (base / 255f * body).coerceIn(0f, 1f)
    return Color(red = channel(0xD9), green = channel(0x7A), blue = channel(0x2B))
}

private data class DieTumble(val start: Quat, val axis: Vec3, val end: Quat)

private fun tumbleFor(die: ShownDie, random: Random): DieTumble {
    val mesh = DiceMeshes.mesh(die)
    val face = mesh.faces.firstOrNull { it.id == die.value } ?: mesh.faces.first()
    val end = orientFace(face)
    val start = Quat(random.nextFloat(), random.nextFloat() - 0.5f, random.nextFloat() - 0.5f, random.nextFloat() - 0.5f).unit()
    val axis = Vec3(random.nextFloat() - 0.5f, random.nextFloat() - 0.5f, random.nextFloat() - 0.5f).unit()
    return DieTumble(start, axis, end)
}

private fun pose(tumble: DieTumble, t: Float): Quat {
    val base = slerp(tumble.start, tumble.end, t.coerceIn(0f, 1f))
    val spin = axisAngle(tumble.axis, (1f - t) * 4f * PI.toFloat())
    return (spin * base).unit()
}

private fun orientFace(face: DieFace): Quat {
    val center = centroid(face.verts)
    var normal = faceNormal(face.verts)
    if (normal.dot(center) < 0f) normal = normal * -1f
    val align = rotationFromTo(normal.unit(), Vec3(0f, 0f, 1f))
    val edge = face.verts.first() - center
    val turned = align.rotate(edge)
    val twist = axisAngle(Vec3(0f, 0f, 1f), -atan2(turned.x, turned.y))
    return (twist * align).unit()
}

private data class Vec3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: Vec3) = Vec3(x - o.x, y - o.y, z - o.z)
    operator fun times(s: Float) = Vec3(x * s, y * s, z * s)
    fun dot(o: Vec3) = x * o.x + y * o.y + z * o.z
    fun cross(o: Vec3) = Vec3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x)
    fun length() = sqrt(dot(this)).coerceAtLeast(1e-8f)
    fun unit() = this * (1f / length())
}

private data class Quat(val w: Float, val x: Float, val y: Float, val z: Float) {
    fun unit(): Quat {
        val len = sqrt(w * w + x * x + y * y + z * z).coerceAtLeast(1e-8f)
        return Quat(w / len, x / len, y / len, z / len)
    }
    fun conjugate() = Quat(w, -x, -y, -z)
    operator fun times(o: Quat) = Quat(
        w * o.w - x * o.x - y * o.y - z * o.z,
        w * o.x + x * o.w + y * o.z - z * o.y,
        w * o.y - x * o.z + y * o.w + z * o.x,
        w * o.z + x * o.y - y * o.x + z * o.w,
    )
    fun rotate(v: Vec3): Vec3 {
        val turned = this * Quat(0f, v.x, v.y, v.z) * conjugate()
        return Vec3(turned.x, turned.y, turned.z)
    }
}

private fun axisAngle(axis: Vec3, angle: Float): Quat {
    val u = axis.unit()
    val half = angle / 2f
    val s = sin(half)
    return Quat(cos(half), u.x * s, u.y * s, u.z * s).unit()
}

private fun slerp(a0: Quat, b0: Quat, t: Float): Quat {
    val a = a0.unit()
    var b = b0.unit()
    var dot = a.w * b.w + a.x * b.x + a.y * b.y + a.z * b.z
    if (dot < 0f) {
        b = Quat(-b.w, -b.x, -b.y, -b.z)
        dot = -dot
    }
    if (dot > 0.9995f) {
        return Quat(a.w + (b.w - a.w) * t, a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t, a.z + (b.z - a.z) * t).unit()
    }
    val theta = acos(dot.coerceIn(-1f, 1f))
    val denom = sin(theta).coerceAtLeast(1e-6f)
    val w1 = sin((1f - t) * theta) / denom
    val w2 = sin(t * theta) / denom
    return Quat(a.w * w1 + b.w * w2, a.x * w1 + b.x * w2, a.y * w1 + b.y * w2, a.z * w1 + b.z * w2).unit()
}

private fun rotationFromTo(from: Vec3, to: Vec3): Quat {
    val f = from.unit()
    val t = to.unit()
    val dot = f.dot(t)
    if (dot > 0.9995f) return Quat(1f, 0f, 0f, 0f)
    if (dot < -0.9995f) {
        val axis = if (abs(f.x) < 0.9f) Vec3(1f, 0f, 0f).cross(f) else Vec3(0f, 1f, 0f).cross(f)
        return axisAngle(axis, PI.toFloat())
    }
    val cross = f.cross(t)
    return Quat(1f + dot, cross.x, cross.y, cross.z).unit()
}

private data class DieFace(val verts: List<Vec3>, val id: Int, val label: String)

private fun centroid(verts: List<Vec3>): Vec3 {
    var sum = Vec3(0f, 0f, 0f)
    for (v in verts) sum += v
    return sum * (1f / verts.size.coerceAtLeast(1))
}

private fun faceNormal(verts: List<Vec3>): Vec3 {
    var n = Vec3(0f, 0f, 0f)
    for (i in verts.indices) {
        val a = verts[i]
        val b = verts[(i + 1) % verts.size]
        n += a.cross(b)
    }
    if (n.length() < 1e-4f && verts.size >= 3) n = (verts[1] - verts[0]).cross(verts[2] - verts[0])
    return n.unit()
}

private fun outward(verts: List<Vec3>): List<Vec3> {
    val n = faceNormal(verts)
    return if (n.dot(centroid(verts)) < 0f) verts.asReversed() else verts
}

private object DiceMeshes {
    private val standard: Map<Int, List<DieFace>> = mapOf(
        4 to number(tetra(), 1),
        6 to number(cube(), 1),
        8 to number(octa(), 1),
        10 to number(d10(), 1),
        12 to number(dodeca(), 1),
        20 to number(icosa(), 1),
    )
    private val tens: List<DieFace> = d10().mapIndexed { index, verts ->
        val value = index * 10
        DieFace(outward(verts), value, if (value == 0) "00" else value.toString())
    }
    private val units: List<DieFace> = d10().mapIndexed { index, verts ->
        DieFace(outward(verts), index, index.toString())
    }

    fun mesh(die: ShownDie): ListHolder = when (die.faceSet) {
        "tens" -> ListHolder(tens)
        "units" -> ListHolder(units)
        else -> ListHolder(standard[die.sides] ?: standard.getValue(20))
    }

    private fun number(raw: List<List<Vec3>>, start: Int): List<DieFace> =
        raw.mapIndexed { index, verts ->
            val value = start + index
            DieFace(outward(verts), value, value.toString())
        }
}

private class ListHolder(val faces: List<DieFace>)

private fun cube(): List<List<Vec3>> {
    val s = 1f
    return listOf(
        listOf(Vec3(-s, -s, s), Vec3(s, -s, s), Vec3(s, s, s), Vec3(-s, s, s)),
        listOf(Vec3(-s, -s, -s), Vec3(-s, s, -s), Vec3(s, s, -s), Vec3(s, -s, -s)),
        listOf(Vec3(-s, -s, -s), Vec3(-s, -s, s), Vec3(-s, s, s), Vec3(-s, s, -s)),
        listOf(Vec3(s, -s, -s), Vec3(s, s, -s), Vec3(s, s, s), Vec3(s, -s, s)),
        listOf(Vec3(-s, s, -s), Vec3(-s, s, s), Vec3(s, s, s), Vec3(s, s, -s)),
        listOf(Vec3(-s, -s, -s), Vec3(s, -s, -s), Vec3(s, -s, s), Vec3(-s, -s, s)),
    )
}

private fun tetra(): List<List<Vec3>> {
    val a = Vec3(1f, 1f, 1f)
    val b = Vec3(1f, -1f, -1f)
    val c = Vec3(-1f, 1f, -1f)
    val d = Vec3(-1f, -1f, 1f)
    return listOf(listOf(a, b, c), listOf(a, c, d), listOf(a, d, b), listOf(b, d, c))
}

private fun octa(): List<List<Vec3>> {
    val p = listOf(Vec3(1f, 0f, 0f), Vec3(-1f, 0f, 0f), Vec3(0f, 1f, 0f), Vec3(0f, -1f, 0f), Vec3(0f, 0f, 1f), Vec3(0f, 0f, -1f))
    val idx = listOf(
        intArrayOf(0, 2, 4), intArrayOf(2, 1, 4), intArrayOf(1, 3, 4), intArrayOf(3, 0, 4),
        intArrayOf(2, 0, 5), intArrayOf(1, 2, 5), intArrayOf(3, 1, 5), intArrayOf(0, 3, 5),
    )
    return idx.map { ids -> listOf(p[ids[0]], p[ids[1]], p[ids[2]]) }
}

private fun d10(): List<List<Vec3>> {
    val top = Vec3(0f, 0f, 1.05f)
    val bottom = Vec3(0f, 0f, -1.05f)
    val upper = (0 until 5).map { i ->
        val a = (2.0 * PI * i / 5.0).toFloat()
        Vec3(cos(a), sin(a), 0.32f)
    }
    val lower = (0 until 5).map { i ->
        val a = (2.0 * PI * i / 5.0 + PI / 5.0).toFloat()
        Vec3(cos(a), sin(a), -0.32f)
    }
    val faces = mutableListOf<List<Vec3>>()
    for (i in 0 until 5) {
        val j = (i + 1) % 5
        faces += listOf(top, upper[i], lower[i], upper[j])
        faces += listOf(bottom, lower[j], upper[j], lower[i])
    }
    return faces
}

private fun icosaVerts(): List<Vec3> {
    val phi = 1.618034f
    return listOf(
        Vec3(-1f, phi, 0f), Vec3(1f, phi, 0f), Vec3(-1f, -phi, 0f), Vec3(1f, -phi, 0f),
        Vec3(0f, -1f, phi), Vec3(0f, 1f, phi), Vec3(0f, -1f, -phi), Vec3(0f, 1f, -phi),
        Vec3(phi, 0f, -1f), Vec3(phi, 0f, 1f), Vec3(-phi, 0f, -1f), Vec3(-phi, 0f, 1f),
    ).map { it.unit() }
}

private fun icosaIndex(): List<IntArray> = listOf(
    intArrayOf(0, 11, 5), intArrayOf(0, 5, 1), intArrayOf(0, 1, 7), intArrayOf(0, 7, 10), intArrayOf(0, 10, 11),
    intArrayOf(1, 5, 9), intArrayOf(5, 11, 4), intArrayOf(11, 10, 2), intArrayOf(10, 7, 6), intArrayOf(7, 1, 8),
    intArrayOf(3, 9, 4), intArrayOf(3, 4, 2), intArrayOf(3, 2, 6), intArrayOf(3, 6, 8), intArrayOf(3, 8, 9),
    intArrayOf(4, 9, 5), intArrayOf(2, 4, 11), intArrayOf(6, 2, 10), intArrayOf(8, 6, 7), intArrayOf(9, 8, 1),
)

private fun icosa(): List<List<Vec3>> {
    val verts = icosaVerts()
    return icosaIndex().map { ids -> listOf(verts[ids[0]], verts[ids[1]], verts[ids[2]]) }
}

private fun dodeca(): List<List<Vec3>> {
    val verts = icosaVerts()
    val triangles = icosaIndex()
    val incident = Array(verts.size) { mutableListOf<Int>() }
    triangles.forEachIndexed { face, ids ->
        for (id in ids) incident[id] += face
    }
    return verts.indices.map { vertex ->
        val axis = verts[vertex]
        val tmp = if (abs(axis.x) < 0.9f) Vec3(1f, 0f, 0f) else Vec3(0f, 1f, 0f)
        val tangent = axis.cross(tmp).unit()
        val bitangent = axis.cross(tangent).unit()
        val ordered = incident[vertex].sortedBy { face ->
            val center = centroid(triangles[face].map { verts[it] })
            atan2(center.dot(bitangent), center.dot(tangent))
        }
        ordered.map { face -> centroid(triangles[face].map { verts[it] }) }
    }
}