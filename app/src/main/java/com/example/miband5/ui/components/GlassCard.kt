package com.example.miband5.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.example.miband5.ui.theme.CyanLight
import com.example.miband5.ui.theme.MagentaPurple
import com.example.miband5.ui.theme.NavyBase
import com.example.miband5.ui.theme.NavyMid
import com.example.miband5.ui.theme.NavyTop
import kotlin.random.Random

/**
 * Frosted-glass card with:
 *  - navy base vertical gradient (#14151f → #1c2440 → #282d50)
 *  - magenta radial bloom top-right, cyan radial bloom bottom-left
 *  - visible film grain overlay (deliberately strong, per the brief)
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val grain = rememberNoiseTexture()
    val shape = RoundedCornerShape(28.dp)

    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .clip(shape)
            .drawBehind {
                // Base vertical gradient
                drawRect(Brush.verticalGradient(listOf(NavyBase, NavyMid, NavyTop)))

                // Magenta bloom top-right
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(MagentaPurple.copy(alpha = 0.45f), Color.Transparent),
                        center = Offset(size.width * 0.92f, size.height * 0.05f),
                        radius = size.width * 0.85f
                    )
                )

                // Cyan bloom bottom-left
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(CyanLight.copy(alpha = 0.35f), Color.Transparent),
                        center = Offset(size.width * 0.06f, size.height * 0.95f),
                        radius = size.width * 0.85f
                    )
                )

                // Film grain
                val paint = Paint().apply {
                    shader = ImageShader(grain, TileMode.Repeated, TileMode.Repeated)
                    alpha = 0.10f
                }
                drawContext.canvas.drawRect(Rect(0f, 0f, size.width, size.height), paint)
            }
            .border(1.dp, Color.White.copy(alpha = 0.10f), shape)
    ) {
        Column(modifier = Modifier.padding(20.dp), content = content)
    }
}

/** Deterministic noise bitmap used as the film-grain overlay. */
@Composable
fun rememberNoiseTexture(size: Int = 160, alpha: Float = 0.10f): ImageBitmap =
    remember(size, alpha) {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val rnd = Random(0x5EED)
        val px = IntArray(size * size)
        for (i in px.indices) {
            val v = rnd.nextInt(256)
            px[i] = ((alpha * 255).toInt() shl 24) or (v shl 16) or (v shl 8) or v
        }
        bmp.setPixels(px, 0, size, 0, 0, size, size)
        bmp.asImageBitmap()
    }
