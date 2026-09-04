package com.example.miband5.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Bell-curve chart: Catmull-Rom (cardinal) spline through 7 daily values,
 * white stroke, dotted horizontal baseline, dotted vertical drop-line at the
 * selected day, and a small circle marker with a translucent halo.
 */
@Composable
fun BellCurveChart(
    values: List<Float>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    lineColor: Color = Color.White
) {
    Canvas(modifier = modifier.fillMaxWidth().height(180.dp)) {
        if (values.size < 2) return@Canvas

        val maxV = values.maxOrNull() ?: 1f
        val minV = values.minOrNull() ?: 0f
        val range = (maxV - minV).coerceAtLeast(1f)

        val padX = 12.dp.toPx()
        val padY = 22.dp.toPx()
        val stepX = (size.width - padX * 2) / (values.size - 1)

        val points = values.mapIndexed { i, v ->
            val x = padX + stepX * i
            val y = padY + (1f - (v - minV) / range) * (size.height - padY * 2)
            Offset(x, y)
        }

        val baselineY = padY + (size.height - padY * 2)

        // Dotted horizontal baseline
        drawLine(
            color = Color.White.copy(alpha = 0.25f),
            start = Offset(padX, baselineY),
            end = Offset(size.width - padX, baselineY),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 6.dp.toPx()))
        )

        // Dotted vertical drop-line at the selected day
        val sel = points[selectedIndex.coerceIn(0, points.size - 1)]
        drawLine(
            color = Color.White.copy(alpha = 0.30f),
            start = Offset(sel.x, sel.y),
            end = Offset(sel.x, baselineY),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 4.dp.toPx()))
        )

        // Catmull-Rom spline (converted to cubic Béziers)
        val path = Path()
        path.moveTo(points[0].x, points[0].y)
        for (i in 0 until points.size - 1) {
            val p0 = points.getOrNull(i - 1) ?: points[i]
            val p1 = points[i]
            val p2 = points[i + 1]
            val p3 = points.getOrNull(i + 2) ?: p2
            val c1 = Offset(p1.x + (p2.x - p0.x) / 6f, p1.y + (p2.y - p0.y) / 6f)
            val c2 = Offset(p2.x - (p3.x - p1.x) / 6f, p2.y - (p3.y - p1.y) / 6f)
            path.cubicTo(c1.x, c1.y, c2.x, c2.y, p2.x, p2.y)
        }
        drawPath(path, color = lineColor, style = Stroke(width = 2.5.dp.toPx()))

        // Marker halo + dot
        drawCircle(Color.White.copy(alpha = 0.18f), radius = 12.dp.toPx(), center = sel)
        drawCircle(Color.White, radius = 4.5.dp.toPx(), center = sel)
    }
}
