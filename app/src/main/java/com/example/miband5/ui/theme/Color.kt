package com.example.miband5.ui.theme

import androidx.compose.ui.graphics.Color

// ---- Base navy (chart card background) ----
val NavyBase = Color(0xFF14151F)
val NavyMid = Color(0xFF1C2440)
val NavyTop = Color(0xFF282D50)

// ---- Radial bloom accents ----
val MagentaPurple = Color(0xFFB06BFF)
val CyanLight = Color(0xFF5CD8FF)

// ---- Stat pill gradients (from the design brief, exact hex) ----
// Steps: linear-gradient(150deg, #ed91d8, #e57aac 35%, #b3626b 70%, #946857)
val StepsStops = listOf(
    0.00f to Color(0xFFED91D8),
    0.35f to Color(0xFFE57AAC),
    0.70f to Color(0xFFB3626B),
    1.00f to Color(0xFF946857)
)

// Heart Rate: linear-gradient(150deg, #6276a8, #59587b 55%, #3d4166)
val HeartRateStops = listOf(
    0.00f to Color(0xFF6276A8),
    0.55f to Color(0xFF59587B),
    1.00f to Color(0xFF3D4166)
)

// Sleep: linear-gradient(150deg, #ca6a58, #dc6237 30%, #ed823d 55%, #f2a957 80%, #edc68f)
val SleepStops = listOf(
    0.00f to Color(0xFFCA6A58),
    0.30f to Color(0xFFDC6237),
    0.55f to Color(0xFFED823D),
    0.80f to Color(0xFFF2A957),
    1.00f to Color(0xFFEDC68F)
)

// ---- App backgrounds ----
val DashboardBackgroundDark = NavyBase
val DashboardBackgroundLight = Color(0xFFEFF1F7)

val StressStops = listOf(
    0.00f to Color(0xFF7AD7C7),
    0.55f to Color(0xFF3FA89A),
    1.00f to Color(0xFF2B6F6A)
)

val BatteryStops = listOf(
    0.00f to Color(0xFFB08CFF),
    0.55f to Color(0xFF7A5CDB),
    1.00f to Color(0xFF4A3A8C)
)

val DistStops = listOf(
    0.00f to Color(0xFFF2F2F6),
    1.00f to Color(0xFFD7DBE8)
)

