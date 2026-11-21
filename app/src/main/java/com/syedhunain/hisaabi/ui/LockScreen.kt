package com.syedhunain.hisaabi.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun LockScreen(check: (String) -> Boolean, onUnlocked: () -> Unit) {
    val s = LocalStr.current
    val scope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    val shake = remember { Animatable(0f) }

    fun press(key: String) {
        if (key == "<") {
            pin = pin.dropLast(1); return
        }
        if (pin.length >= 4) return
        pin += key
        wrong = false
        if (pin.length == 4) {
            if (check(pin)) {
                onUnlocked()
            } else {
                wrong = true
                scope.launch {
                    shake.animateTo(0f, keyframes {
                        durationMillis = 400
                        -24f at 50; 24f at 100; -18f at 150; 18f at 200; -8f at 260; 8f at 320; 0f at 400
                    })
                    pin = ""
                }
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Brand.EmeraldDeep)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Logo(size = 84.dp, corner = 20.dp)
        Spacer(Modifier.height(20.dp))
        Text("Hisaabi", color = Brand.Ivory, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(if (wrong) s.wrongPin else s.enterPin, color = if (wrong) Brand.GoldLight else Brand.Mint, fontSize = 15.sp)
        Spacer(Modifier.height(24.dp))
        Row(
            Modifier.offset { IntOffset(shake.value.roundToInt(), 0) },
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            repeat(4) { i ->
                Box(
                    Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (i < pin.length) Brand.GoldLight else Brand.Ivory.copy(alpha = 0.12f))
                        .border(1.dp, Brand.GoldLight.copy(alpha = 0.6f), CircleShape)
                )
            }
        }
        Spacer(Modifier.height(40.dp))
        val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("", "0", "<"))
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    row.forEach { key ->
                        if (key.isEmpty()) {
                            Spacer(Modifier.size(72.dp))
                        } else {
                            Box(
                                Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(Brand.Ivory.copy(alpha = 0.08f))
                                    .bounceClick { press(key) }
                                    .semantics { if (key == "<") contentDescription = s.deleteDigit },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(if (key == "<") "⌫" else key, color = Brand.Ivory, fontSize = 26.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}
