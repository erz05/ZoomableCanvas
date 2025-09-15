package com.erz.zoomablecanvas.lib

import android.graphics.Canvas

abstract class ZoomableManager {

    private var zoomable: IZoomable? = null

    abstract fun onDraw(
        canvas: Canvas
    )

    fun invalidate() {
        zoomable?.invalidate()
    }

    fun canPan(): Boolean {
        // Todo ERZ -------
        return false
    }

    fun getScale(): Float {
        // Todo ERZ -------
        return 0f
    }

    fun zoomTo() {
        // Todo ERZ -------
    }

    /**
     * Maps the touchX value to the x position on the canvas
     */
    private fun mapTouchXValue(
        touchX: Float
    ): Float? {
        val canvasBounds = zoomable?.getCanvasBounds() ?: return null
        val viewportBounds = zoomable?.getViewportBounds() ?: return null
        return ((touchX * viewportBounds.width()) / canvasBounds.width()) + viewportBounds.left
    }

    /**
     * Maps the touchY value to the y position on the canvas
     */
    private fun mapTouchYValue(
        touchY: Float
    ): Float? {
        val canvasBounds = zoomable?.getCanvasBounds() ?: return null
        val viewportBounds = zoomable?.getViewportBounds() ?: return null
        return ((touchY * viewportBounds.height()) / canvasBounds.height()) + viewportBounds.top
    }

    fun constraintViewPort() {
//        // Constraint the viewportBounds to the imageBounds
//        val maxLeft = imageBounds.centerX() - (viewportBounds.width() / 2f)
//        val maxTop = imageBounds.centerY() - (viewportBounds.height() / 2f)
//        viewportBounds.offsetTo(
//            if (viewportBounds.width() > imageBounds.width()) maxLeft else max(
//                imageBounds.left,
//                min(viewportBounds.left, imageBounds.right - viewportBounds.width())
//            ),
//            if (viewportBounds.height() > imageBounds.height()) maxTop else max(
//                imageBounds.top,
//                min(viewportBounds.top, imageBounds.bottom - viewportBounds.height())
//            )
//        )
    }

    internal fun setZoomable(
        zoomable: IZoomable
    ) {
        this.zoomable = zoomable
    }
}