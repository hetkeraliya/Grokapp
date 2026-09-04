package com.example.miband5.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.miband5.ui.dashboard.DayData
import java.time.format.DateTimeFormatter

/**
 * Day selector: S M T W T F S circles — selected is a solid white oval,
 * others thin-outlined/transparent. Below: `‹ Jun 6 – Jun 12 ›` navigator.
 */
@Composable
fun DaySelector(
    days: List<DayData>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val labels = listOf("S", "M", "T", "W", "T", "F", "S")
    val fmt = DateTimeFormatter.ofPattern("MMM d")

    Column(modifier) {
        Row(horizontalArrangement = Arrangement.SpaceBetween) {
            days.forEachIndexed { i, _ ->
                val selected = i == selectedIndex
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(if (selected) Color.White else Color.Transparent)
                        .border(
                            width = if (selected) 0.dp else 1.dp,
                            color = Color.White.copy(alpha = 0.35f),
                            shape = CircleShape
                        )
                        .clickable { onSelect(i) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = labels[i],
                        color = if (selected) Color(0xFF14151F) else Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(Modifier.size(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("‹", fontSize = 20.sp, color = Color.White.copy(alpha = 0.6f))
            Spacer(Modifier.width(18.dp))
            Text(
                text = "${days.firstOrNull()?.date?.format(fmt)} – ${days.lastOrNull()?.date?.format(fmt)}",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp
            )
            Spacer(Modifier.width(18.dp))
            Text("›", fontSize = 20.sp, color = Color.White.copy(alpha = 0.6f))
        }
    }
}
