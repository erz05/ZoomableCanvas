package com.erz.zoomablecanvas.lib

import android.graphics.RectF

internal interface IZoomable {
    fun invalidate()
    fun getCanvasBounds(): RectF
    fun getViewportBounds(): RectF
}