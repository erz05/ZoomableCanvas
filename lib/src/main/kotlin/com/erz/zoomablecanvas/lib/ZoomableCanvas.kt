package com.erz.zoomablecanvas.lib

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ZoomableCanvas(
    modifier: Modifier = Modifier,
    zoomableCanvasViewModel: ZoomableCanvasViewModel
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val invalidate by zoomableCanvasViewModel.invalidate.collectAsState()

    val zoomableCanvasGestureDetector = remember {
        ZoomableCanvasGestureDetector(
            context = context,
            listener = object : ZoomableCanvasGestureDetecorListener {

                override fun onTranslate(distanceX: Float, distanceY: Float) {
                    zoomableCanvasViewModel.onTranslate(
                        distanceX = distanceX,
                        distanceY = distanceY
                    )
                }

                override fun onMove(
                    lastX: Float?,
                    lastY: Float?,
                    currentX: Float,
                    currentY: Float
                ) {
                    zoomableCanvasViewModel.onMove(
                        lastX = lastX,
                        lastY = lastY,
                        currentX = currentX,
                        currentY = currentY
                    )
                }

                override fun onUp(
                    currentX: Float,
                    currentY: Float,
                    totalPointerCount: Int
                ) {
                    val result = zoomableCanvasViewModel.onUp(
                        currentX = currentX,
                        currentY = currentY,
                        totalPointerCount = totalPointerCount
                    )

//                    if (result) {
//                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
//                    }
                }

                override fun onLongPress(touchX: Float, touchY: Float) {
                    zoomableCanvasViewModel.onLongPress(
                        touchX = touchX,
                        touchY = touchY
                    )
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }

                override fun onScale(
                    currentX: Float,
                    currentY: Float,
                    scale: Float
                ) {
                    zoomableCanvasViewModel.onScale(
                        currentX = currentX,
                        currentY = currentY,
                        scale = scale
                    )
                }

                override fun onDone() {
                    zoomableCanvasViewModel.onTouchDone()
                }
            }
        )
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .pointerInteropFilter { motionEvent ->
                return@pointerInteropFilter zoomableCanvasGestureDetector.onTouchEvent(motionEvent)
            }
            .background(colorScheme.background)
            .onGloballyPositioned { coordinates ->
                zoomableCanvasViewModel.onGloballyPositioned(
                    coordinates.size.width.toFloat(),
                    coordinates.size.height.toFloat(),
                )
            }
    ) {
        invalidate

        drawIntoCanvas { canvas ->
            zoomableCanvasViewModel.draw(canvas.nativeCanvas)
        }
    }
}