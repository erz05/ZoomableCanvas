package com.erz.zoomablecanvas.lib

const val DEFAULT_MIN_SCALE = 1f
const val DEFAULT_MAX_SCALE = 13f
const val DEFAULT_DOUBLE_TAP_ZOOM_DURATION = 300L

data class ZoomableConfiguration(
    val canPan: Boolean,
    val canFling: Boolean,
    val canDoubleTapToZoom: Boolean,
    val doubleTapZoomDuration: Long = DEFAULT_DOUBLE_TAP_ZOOM_DURATION,
    val canLongPress: Boolean,
    val minScale: Float = DEFAULT_MIN_SCALE,
    val maxScale: Float = DEFAULT_MAX_SCALE,
    val enableMiniMap: Boolean
)