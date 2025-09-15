package com.erz.zoomablecanvas.lib

import android.graphics.RectF

internal interface IZoomable {
    fun onGloballyPositioned(
        composableWidth: Float,
        composableHeight: Float,
        constraintBounds: RectF?
    )
    fun invalidate()
    fun getCanvasBounds(): RectF
    fun getViewportBounds(): RectF
}