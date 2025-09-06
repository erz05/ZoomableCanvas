package com.erz.zoomablecanvas.lib

data class ZoomableConfiguration(
    val canFling: Boolean,
    val canDoubleTapToZoom: Boolean,
    val minZoom: Float,
    val maxZoom: Float,
    val constraintToComposableBounds: Boolean,
    val enableMiniMap: Boolean
)