package com.example.miband5.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * True-capsule stat pill (border-radius 999px) with a 150° gradient.
 * Layout: label top-left, big value + unit, rounded %-change chip with arrow.
 */
@Composable
fun StatPill(
    title: String,
    value: String,
    unit: String,
    changePct: Float?,
    colorStops: List<Pair<Float, Color>>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(104.dp)
            .clip(RoundedCornerShape(999.dp))
            .drawBehind {
                drawRect(
                    Brush.linearGradient(
                        colorStops = colorStops.toTypedArray(),
                        start = Offset(size.width, 0f),   // top-right
                        end = Offset(0f, size.height)     // bottom-left  (~150°)
                    )
                )
            }
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Column(verticalArrangement = Arrangement.SpaceBetween) {
            Text(title, color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(4.dp))
                Text(
                    unit,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
            if (changePct != null) {
                val up = changePct >= 0
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.20f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        "${if (up) "▲" else "▼"} ${abs(changePct).roundToInt()}%",
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
