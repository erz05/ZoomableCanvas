package com.erz.zoomablecanvas.lib

import android.graphics.Canvas
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
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ZoomableCanvas(
    modifier: Modifier = Modifier,
    onDraw: (Canvas) -> Unit
) {
    val viewModel: ZoomableCanvasViewModel = viewModel()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val invalidate by viewModel.invalidate.collectAsState()

    val zoomableCanvasGestureDetector = remember {
        ZoomableCanvasGestureDetector(
            context = context,
            listener = object : ZoomableCanvasGestureDetecorListener {

                override fun onTranslate(distanceX: Float, distanceY: Float) {
                    viewModel.onTranslate(
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
                    viewModel.onMove(
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
                    val result = viewModel.onUp(
                        currentX = currentX,
                        currentY = currentY,
                        totalPointerCount = totalPointerCount
                    )

//                    if (result) {
//                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
//                    }
                }

                override fun onLongPress(touchX: Float, touchY: Float) {
                    viewModel.onLongPress(
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
                    viewModel.onScale(
                        currentX = currentX,
                        currentY = currentY,
                        scale = scale
                    )
                }

                override fun onDone() {
                    viewModel.onTouchDone()
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
                viewModel.onGloballyPositioned(
                    coordinates.size.width.toFloat(),
                    coordinates.size.height.toFloat(),
                )
            }
    ) {
        invalidate

        drawIntoCanvas { canvas ->
            viewModel.draw(
                canvas = canvas.nativeCanvas,
                externalDraw = { zoomableCanvas ->
                    onDraw(zoomableCanvas)
                }
            )
        }
    }
}