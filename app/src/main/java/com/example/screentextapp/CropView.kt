package com.example.screentextapp

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * CropView — Custom View untuk memilih area crop dari screenshot.
 *
 * Cara pakai:
 *  - User menyentuh dan menyeret untuk menggambar crop rectangle
 *  - Daerah di luar crop rectangle di-dim
 *  - Corner handles kuning menandai sudut crop
 *  - Panggil getCropRect() untuk mendapatkan koordinat crop dalam View
 *  - Panggil hasValidSelection() untuk cek apakah area sudah cukup besar
 */
class CropView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    // Koordinat crop
    private var startX = 0f
    private var startY = 0f
    private var endX = 0f
    private var endY = 0f
    private var hasSelection = false

    // Paint untuk border putih putus-putus
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.WHITE
        strokeWidth = 3f
        pathEffect = DashPathEffect(floatArrayOf(20f, 10f), 0f)
    }

    // Paint untuk corner handles (kuning tebal)
    private val cornerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.YELLOW
        strokeWidth = 7f
        strokeCap = Paint.Cap.ROUND
    }

    // Paint untuk dim overlay di luar area crop
    private val dimPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.argb(140, 0, 0, 0)
    }

    // Panjang corner handle (px)
    private val cornerLen = 50f

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.x
                startY = event.y
                endX = event.x
                endY = event.y
                hasSelection = false
            }
            MotionEvent.ACTION_MOVE -> {
                endX = event.x
                endY = event.y
                hasSelection = true
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                endX = event.x
                endY = event.y
                hasSelection = true
                invalidate()
            }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!hasSelection) return

        val left   = minOf(startX, endX)
        val top    = minOf(startY, endY)
        val right  = maxOf(startX, endX)
        val bottom = maxOf(startY, endY)
        val w = width.toFloat()
        val h = height.toFloat()

        // Dim semua area di luar crop rect
        canvas.drawRect(0f, 0f,  w,    top,    dimPaint)
        canvas.drawRect(0f, top, left, bottom, dimPaint)
        canvas.drawRect(right, top, w, bottom, dimPaint)
        canvas.drawRect(0f, bottom, w, h,      dimPaint)

        // Border putus-putus
        canvas.drawRect(left, top, right, bottom, borderPaint)

        // Corner handles — kiri atas
        canvas.drawLine(left,            top,            left + cornerLen, top,            cornerPaint)
        canvas.drawLine(left,            top,            left,            top + cornerLen, cornerPaint)
        // Corner handle — kanan atas
        canvas.drawLine(right - cornerLen, top,            right,           top,            cornerPaint)
        canvas.drawLine(right,             top,            right,           top + cornerLen, cornerPaint)
        // Corner handle — kiri bawah
        canvas.drawLine(left,             bottom - cornerLen, left,          bottom,         cornerPaint)
        canvas.drawLine(left,             bottom,            left + cornerLen, bottom,         cornerPaint)
        // Corner handle — kanan bawah
        canvas.drawLine(right - cornerLen, bottom,            right,          bottom,         cornerPaint)
        canvas.drawLine(right,             bottom - cornerLen, right,          bottom,         cornerPaint)
    }

    /** Kembalikan rect crop dalam koordinat View */
    fun getCropRect(): RectF = RectF(
        minOf(startX, endX), minOf(startY, endY),
        maxOf(startX, endX), maxOf(startY, endY)
    )

    /** Cek apakah area yang dipilih cukup besar untuk di-scan */
    fun hasValidSelection(): Boolean =
        hasSelection &&
        Math.abs(endX - startX) > 60 &&
        Math.abs(endY - startY) > 60
}
