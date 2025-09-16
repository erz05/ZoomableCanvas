package com.erz.zoomablecanvas.lib

import android.view.MotionEvent
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
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ZoomableCanvas(
    modifier: Modifier = Modifier,
    configuration: ZoomableConfiguration,
    manager: ZoomableManager
) {
    val viewModel: ZoomableViewModel = viewModel(
        factory = ZoomableViewModel.Factory(
            configuration = configuration
        )
    )

    manager.setZoomable(
        zoomable = viewModel
    )

    val context = LocalContext.current
    val invalidate by viewModel.invalidate.collectAsState()

    val zoomableGestureDetector = remember {
        ZoomableGestureDetector(
            context = context,
            listener = object : ZoomableGestureDetectorListener {

                override fun onDown(motionEvent: MotionEvent) {
                    viewModel.onDown(
                        motionEvent = motionEvent
                    )
                }

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

                }

                override fun onFling(
                    velocityX: Float,
                    velocityY: Float
                ) {
                    viewModel.onFling(
                        velocityX = velocityX,
                        velocityY = velocityY
                    )
                }

                override fun onUp(
                    currentX: Float,
                    currentY: Float,
                    totalPointerCount: Int
                ) {
                    // Todo ERZ - expose long press
                }

                override fun onLongPress(touchX: Float, touchY: Float) {
                    // Todo ERZ - expose long press
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

                override fun onDoubleTap(motionEvent: MotionEvent) {
                    viewModel.onDoubleTap(
                        motionEvent = motionEvent
                    )
                }

                override fun onDone() {
                    // Todo ERZ - expose onDone
                }
            }
        )
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .pointerInteropFilter { motionEvent ->
                return@pointerInteropFilter zoomableGestureDetector.onTouchEvent(motionEvent)
            }
            .background(colorScheme.background)
            .onGloballyPositioned { coordinates ->
                manager.onGloballyPositioned(
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
                    manager.onDraw(zoomableCanvas)
                }
            )
        }
    }
}