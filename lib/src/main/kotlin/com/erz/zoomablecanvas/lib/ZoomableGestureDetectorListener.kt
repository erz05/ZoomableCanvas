package com.erz.zoomablecanvas.lib

internal interface ZoomableGestureDetectorListener {

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

    fun onFling(
        velocityX: Float,
        velocityY: Float
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