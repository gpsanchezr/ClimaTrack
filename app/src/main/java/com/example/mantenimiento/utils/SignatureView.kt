package com.example.mantenimiento.utils

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * SignatureView - Componente personalizado para captura de firma táctil.
 */
class SignatureView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private var path = Path()
    private var paint = Paint().apply {
        isAntiAlias = true
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeWidth = 5f
    }

    private var isCanvasEmpty = true

    override fun onDraw(canvas: Canvas) {
        canvas.drawPath(path, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                path.moveTo(x, y)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                path.lineTo(x, y)
                isCanvasEmpty = false
            }
            else -> return false
        }
        invalidate()
        return true
    }

    fun clear() {
        path.reset()
        isCanvasEmpty = true
        invalidate()
    }

    fun isEmpty(): Boolean = isCanvasEmpty
    
    fun getSignatureBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        draw(canvas)
        return bitmap
    }
}
