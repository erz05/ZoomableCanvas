package com.erz.zoomablecanvas.lib

const val DEFAULT_MIN_SCALE = 1f
const val DEFAULT_MAX_SCALE = 13f

data class ZoomableConfiguration(
    val canPan: Boolean,
    val canFling: Boolean,
    val canDoubleTapToZoom: Boolean,
    val canLongPress: Boolean,
    val minScale: Float = DEFAULT_MIN_SCALE,
    val maxScale: Float = DEFAULT_MAX_SCALE,
    val constraintToComposableBounds: Boolean,
    val enableMiniMap: Boolean
)