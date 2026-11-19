package com.example.coconutautoplayer

import android.graphics.Bitmap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class RectI(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val cx get() = (left + right) / 2f
    val cy get() = (top + bottom) / 2f
    val width get() = right - left
    val height get() = bottom - top
}
data class Detection(val coconut: Pair<Float, Float>?, val bars: List<RectI>, val confidence: Float, val note: String)

object GameAnalyzer {
    // Lightweight color/shape heuristic. This is an experimental detector, not a trained model.
    fun analyze(source: Bitmap): Detection {
        val targetW = 288
        val scale = targetW.toFloat() / source.width
        val targetH = (source.height * scale).toInt().coerceAtLeast(1)
        val bmp = Bitmap.createScaledBitmap(source, targetW, targetH, false)
        val w = bmp.width; val h = bmp.height
        val pixels = IntArray(w * h)
        bmp.getPixels(pixels, 0, w, 0, 0, w, h)

        fun orange(c: Int): Boolean {
            val r = (c shr 16) and 255; val g = (c shr 8) and 255; val b = c and 255
            return r > 155 && g in 42..155 && b < 105 && r > g * 1.35
        }
        fun brown(c: Int): Boolean {
            val r = (c shr 16) and 255; val g = (c shr 8) and 255; val b = c and 255
            return r in 88..195 && g in 32..125 && b in 8..90 && r > g * 1.15 && g > b * 1.05
        }

        // Horizontal orange runs, merged across adjacent rows into bar rectangles.
        val raw = mutableListOf<RectI>()
        for (y in (h * 0.12).toInt() until (h * 0.94).toInt()) {
            var x = 0
            while (x < w) {
                while (x < w && !orange(pixels[y * w + x])) x++
                val start = x
                while (x < w && orange(pixels[y * w + x])) x++
                if (x - start >= 13) raw.add(RectI(start, y, x, y + 1))
            }
        }
        val bars = mutableListOf<RectI>()
        for (run in raw) {
            val idx = bars.indexOfFirst { abs(it.bottom - run.top) <= 3 && run.left <= it.right + 4 && run.right >= it.left - 4 }
            if (idx >= 0) {
                val old = bars[idx]
                bars[idx] = RectI(min(old.left, run.left), old.top, max(old.right, run.right), run.bottom)
            } else bars.add(run)
        }
        val filteredBars = bars.filter { it.width >= 14 && it.height in 2..22 }.map {
            RectI((it.left / scale).toInt(), (it.top / scale).toInt(),
                  (it.right / scale).toInt(), (it.bottom / scale).toInt())
        }

        // Brown connected components; discard long/thin tree-like regions.
        val seen = BooleanArray(w * h)
        var best: RectI? = null
        var bestScore = Float.NEGATIVE_INFINITY
        val qx = IntArray(w * h)
        val qy = IntArray(w * h)
        for (sy in (h * 0.16).toInt() until (h * 0.82).toInt() step 2) {
            for (sx in (w * 0.04).toInt() until (w * 0.96).toInt() step 2) {
                val start = sy * w + sx
                if (seen[start] || !brown(pixels[start])) continue
                var head = 0; var tail = 0
                qx[tail] = sx; qy[tail] = sy; tail++; seen[start] = true
                var minX = sx; var maxX = sx; var minY = sy; var maxY = sy; var area = 0
                while (head < tail && area < 5000) {
                    val x = qx[head]; val y = qy[head]; head++; area++
                    minX = min(minX, x); maxX = max(maxX, x); minY = min(minY, y); maxY = max(maxY, y)
                    val ns = intArrayOf(y * w + x - 1, y * w + x + 1, (y - 1) * w + x, (y + 1) * w + x)
                    for (ni in ns) {
                        if (ni < 0 || ni >= w * h || seen[ni]) continue
                        val xx = ni % w; val yy = ni / w
                        if (abs(xx - x) + abs(yy - y) != 1) continue
                        if (brown(pixels[ni])) { seen[ni] = true; qx[tail] = xx; qy[tail] = yy; tail++ }
                    }
                }
                val bw = maxX - minX + 1; val bh = maxY - minY + 1
                val ratio = bw.toFloat() / bh.coerceAtLeast(1)
                if (area in 70..1400 && ratio in 0.65f..1.45f && bw in 10..52 && bh in 10..52) {
                    val centerX = (minX + maxX) / 2f
                    val centerY = (minY + maxY) / 2f
                    val centrality = 1f - abs(centerX - w / 2f) / (w / 2f)
                    val score = (1f - abs(1f - ratio)) * 2f + centrality + (1f - abs(area - 380) / 1400f)
                    if (score > bestScore) { bestScore = score; best = RectI(minX, minY, maxX, maxY) }
                }
            }
        }
        bmp.recycle()
        val point = best?.let { Pair(it.cx / scale, it.cy / scale) }
        val confidence = if (best == null) 0f else (bestScore / 4f).coerceIn(0f, 1f)
        return Detection(point, filteredBars, confidence, if (point == null) "Coconut not confidently detected" else "Coconut candidate detected")
    }

    fun chooseTarget(d: Detection, screenW: Int, screenH: Int): Pair<Float, Float>? {
        val p = d.coconut ?: return null
        val x = p.first; val y = p.second
        val radius = screenW * 0.045f
        // Only steer when a detected bar is close enough to the coconut's current lane.
        val threats = d.bars.filter { b ->
            val top = b.top * (screenH.toFloat() / (screenH.toFloat())) // coordinates already from capture bitmap
            abs(top - y) < screenH * 0.105f &&
                x >= b.left - radius && x <= b.right + radius
        }
        if (threats.isEmpty()) return Pair(x, y)
        val bar = threats.minByOrNull { abs(it.cy - y) } ?: return Pair(x, y)
        val leftX = (bar.left - radius - screenW * 0.035f).coerceAtLeast(screenW * 0.06f)
        val rightX = (bar.right + radius + screenW * 0.035f).coerceAtMost(screenW * 0.94f)
        val leftClear = d.bars.none { abs(it.cy - y) < screenH * 0.10f && leftX in (it.left - radius)..(it.right + radius) }
        val rightClear = d.bars.none { abs(it.cy - y) < screenH * 0.10f && rightX in (it.left - radius)..(it.right + radius) }
        return when {
            leftClear && rightClear -> if (abs(x - leftX) <= abs(rightX - x)) Pair(leftX, y) else Pair(rightX, y)
            leftClear -> Pair(leftX, y)
            rightClear -> Pair(rightX, y)
            else -> null
        }
    }
}
