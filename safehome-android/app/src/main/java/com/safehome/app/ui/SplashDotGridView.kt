package com.safehome.app.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.OvershootInterpolator
import kotlin.math.sqrt

class SplashDotGridView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val COLS = 13
    private val ROWS = 13
    private val DOT_MAX_RADIUS = 22f

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF4F7EF8.toInt()
    }

    data class Dot(
        val col: Int,
        val row: Int,
        var scale: Float = 0f,
        var alpha: Float = 0f
    )

    private val dots = mutableListOf<Dot>()
    private val animators = mutableListOf<ValueAnimator>()

    init {
        for (r in 0 until ROWS) {
            for (c in 0 until COLS) {
                dots.add(Dot(c, r))
            }
        }
    }

    private fun dist(row: Int, col: Int): Float {
        val cr = (ROWS - 1) / 2f
        val cc = (COLS - 1) / 2f
        return sqrt((row - cr) * (row - cr) + (col - cc) * (col - cc))
    }

    private val maxDist by lazy { dist(0, 0) }

    fun playRipple(startDelay: Long, onEnd: (() -> Unit)? = null) {
        val SPEED = 850L
        val WAVE_DURATION = 550L

        dots.forEach { dot ->
            val d = dist(dot.row, dot.col)
            val triggerDelay = startDelay + ((d / maxDist) * SPEED).toLong()
            val targetScale = 1.25f - (d / maxDist) * 0.5f
            val targetAlpha = 1f - (d / maxDist) * 0.5f

            val expandAnim = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = WAVE_DURATION
                interpolator = OvershootInterpolator(1.4f)
                this.startDelay = triggerDelay
                addUpdateListener {
                    val v = it.animatedValue as Float
                    dot.scale = v * targetScale
                    dot.alpha = v * targetAlpha
                    invalidate()
                }
            }

            val shrinkAnim = ValueAnimator.ofFloat(1f, 0f).apply {
                duration = WAVE_DURATION
                this.startDelay = triggerDelay + WAVE_DURATION
                addUpdateListener {
                    val v = it.animatedValue as Float
                    dot.scale = (0.2f + v * (targetScale - 0.2f))
                    dot.alpha = v * targetAlpha * 0.15f + 0.15f * (1f - v)
                    invalidate()
                }
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        dot.scale = 0.2f
                        dot.alpha = 0.15f
                    }
                })
            }

            animators.add(expandAnim)
            animators.add(shrinkAnim)
            expandAnim.start()
            shrinkAnim.start()
        }

        val totalDuration = startDelay + SPEED + WAVE_DURATION * 2
        postDelayed({ onEnd?.invoke() }, totalDuration)
    }

    fun fadeOutAll(startDelay: Long, onEnd: (() -> Unit)? = null) {
        val FADE_DURATION = 400L
        val SPEED = 550L

        dots.forEach { dot ->
            val d = dist(dot.row, dot.col)
            val delay = startDelay + (((maxDist - d) / maxDist) * SPEED).toLong()

            val anim = ValueAnimator.ofFloat(dot.scale, 0f).apply {
                duration = FADE_DURATION
                this.startDelay = delay
                addUpdateListener {
                    dot.scale = it.animatedValue as Float
                    dot.alpha = dot.scale * 0.5f
                    invalidate()
                }
            }
            animators.add(anim)
            anim.start()
        }

        postDelayed({ onEnd?.invoke() }, startDelay + SPEED + FADE_DURATION)
    }

    fun stopAll() {
        animators.forEach { it.cancel() }
        animators.clear()
        dots.forEach { it.scale = 0f; it.alpha = 0f }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return

        val cellW = width / COLS.toFloat()
        val cellH = height / ROWS.toFloat()

        dots.forEach { dot ->
            if (dot.scale <= 0f) return@forEach
            val cx = cellW * dot.col + cellW / 2f
            val cy = cellH * dot.row + cellH / 2f
            val radius = DOT_MAX_RADIUS * dot.scale
            paint.alpha = (dot.alpha * 255).toInt().coerceIn(0, 255)
            canvas.drawCircle(cx, cy, radius, paint)
        }
    }
}