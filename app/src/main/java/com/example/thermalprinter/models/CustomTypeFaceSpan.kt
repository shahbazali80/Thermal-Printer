package com.example.thermalprinter.models

import android.graphics.Paint
import android.graphics.Typeface
import android.text.style.TypefaceSpan
import android.text.TextPaint
import com.example.thermalprinter.models.CustomTypeFaceSpan

class CustomTypeFaceSpan(family: String?, private val newType: Typeface?) : TypefaceSpan(family) {
    override fun updateDrawState(ds: TextPaint) {
        if (newType != null) {
            applyCustomTypeFace(ds, newType)
        }
    }

    override fun updateMeasureState(paint: TextPaint) {
        if (newType != null) {
            applyCustomTypeFace(paint, newType)
        }
    }

    companion object {
        private fun applyCustomTypeFace(paint: Paint, tf: Typeface) {
            val oldStyle: Int
            val old = paint.typeface
            oldStyle = old?.style ?: 0
            val fake = oldStyle and tf.style.inv()
            if (fake and Typeface.BOLD != 0) {
                paint.isFakeBoldText = true
            }
            if (fake and Typeface.ITALIC != 0) {
                paint.textSkewX = -0.25f
            }
            paint.typeface = tf
        }
    }
}