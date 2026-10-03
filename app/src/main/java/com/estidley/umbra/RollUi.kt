package com.estidley.umbra

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RollAskOverlay(prompt: RollPrompt, onPick: (String) -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xB3000000))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
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
        Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF2A2F3D)).clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color(0xFFE8EAF0), fontWeight = FontWeight.SemiBold)
    }
}