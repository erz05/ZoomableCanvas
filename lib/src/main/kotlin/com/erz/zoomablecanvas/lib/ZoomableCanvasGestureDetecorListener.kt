package com.erz.zoomablecanvas.lib

interface ZoomableCanvasGestureDetecorListener {

    fun onTranslate(
        distanceX: Float,
        distanceY: Float
    )

    fun onMove(
        lastX: Float?,
        lastY: Float?,
        currentX: Float,
        currentY: Float
    )

    fun onUp(
        currentX: Float,
        currentY: Float,
        totalPointerCount: Int
    )

    fun onLongPress(
        touchX: Float,
        touchY: Float
    )

    fun onScale(
        currentX: Float,
        currentY: Float,
        scale: Float
    )

    fun onDone()
}