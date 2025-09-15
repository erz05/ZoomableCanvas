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

    internal fun setZoomable(
        zoomable: IZoomable
    ) {
        this.zoomable = zoomable
    }
}