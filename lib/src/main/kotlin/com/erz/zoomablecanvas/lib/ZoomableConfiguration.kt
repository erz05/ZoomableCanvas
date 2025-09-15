package com.erz.zoomablecanvas.lib

data class ZoomableConfiguration(
    val canPan: Boolean,
    val canFling: Boolean,
    val canDoubleTapToZoom: Boolean,
    val canLongPress: Boolean,
    val minZoom: Float,
    val maxZoom: Float,
    val constraintToComposableBounds: Boolean,
    val enableMiniMap: Boolean
)