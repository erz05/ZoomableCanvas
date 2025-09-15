package com.erz.zoomablecanvas.lib

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.withMatrix
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.max
import kotlin.math.min

internal class ZoomableViewModel(
    private val configuration: ZoomableConfiguration
) : ViewModel(), IZoomable {

    /**
     * Simple state that holds on holds a timestamp
     * used to force a recomposition on the zoomableCanvas
     * We update this everytime we update the viewport bounds
     * This can also be called manually with the invalidate() function through the [ZoomableManager]
     */
    val invalidate = MutableStateFlow(0L)

    /**
     * Matrix used to apply transformations to the canvas
     */
    val canvasMatrix = Matrix()

    /**
     * RectF that represents the canvas bounds
     * Used to calculate the canvasMatrix
     */
    var canvasBounds = RectF()

    /**
     * RectF that represent the visible bounds on the canvas
     * Used to calculate the canvasMatrix
     */
    var viewportBounds = RectF()

    /**
     * Float used to constrain the [currentScale] to the min scale
     */
    private val minScale = configuration.minScale

    /**
     * Float used to constrain the [currentScale] to the max scale
     */
    private val maxScale = configuration.maxScale

    /**
     * Float that holds on to the current scale
     * This is updated by the scaleGestureDetector
     * This float is constrained to the min and max scale values
     */
    private var currentScale = minScale
        set(value) {
            field = max(minScale, min(maxScale, value))
        }

    /**
     * Boolean to enable the mini map
     */
    var miniMapEnabled = true

    /**
     * Matrix to scale down the viewport and canvas bounds for the mini map
     */
    private val miniMapMatrix = Matrix().apply {
        setScale(.3f, .3f)
    }

    /**
     * reusable paint
     */
    private val paint = Paint().apply {
        isAntiAlias = true
        isDither = true
    }

    override fun invalidate() {
        invalidate.update {
            System.currentTimeMillis()
        }
    }

    override fun getCanvasBounds(): RectF {
        return canvasBounds
    }

    override fun getViewportBounds(): RectF {
        return viewportBounds
    }

    // Todo ERZ - should this be done on init?
    fun setup(
    ) {
        // Reset viewPort
        currentScale = minScale
        panTo()
    }

    fun onGloballyPositioned(
        composableWidth: Float,
        composableHeight: Float
    ) {
        canvasBounds.set(
            /* left = */ 0f,
            /* top = */ 0f,
            /* right = */ composableWidth,
            /* bottom = */ composableHeight
        )

        // Todo ERZ - should we restore the viewport?
        // To restore viewPort we need to remember the center position of the last viewport and the last scale
        // we will use that to center the new viewport and to scale

        panTo()
    }

    fun onScale(
        currentX: Float,
        currentY: Float,
        scale: Float
    ) {
        zoomTo(
            currentX,
            currentY,
            scale * currentScale
        )
    }

    // Used to pan viewport inside of bounds
    fun onTranslate(
        distanceX: Float,
        distanceY: Float,
    ) {
        panTo(
            viewportBounds.left + ((distanceX * viewportBounds.width()) / canvasBounds.width()),
            viewportBounds.top + ((distanceY * viewportBounds.height()) / canvasBounds.height())
        )
    }

    // Used to draw
    fun onMove(
        lastX: Float?,
        lastY: Float?,
        currentX: Float,
        currentY: Float,
    ) {

    }

    fun onLongPress(
        touchX: Float,
        touchY: Float,
    ) {

    }

    fun onUp(
        currentX: Float,
        currentY: Float,
        totalPointerCount: Int
    ): Boolean {

        return false
    }

    fun onTouchDone() {

    }

    /**
     * This Function updates the position of the viewport given the new left and top position
     * this function will keep the viewport constrained to the view bounds
     * finally this function updates the canvas matrix and calls invalidate on the view
     * @param left - the new left position of the viewport
     * @param top - the new top position of the viewport
     */
    private fun panTo(
        left: Float = viewportBounds.left,
        top: Float = viewportBounds.top
    ) {
        val newViewportWidth = canvasBounds.width() / currentScale
        val newViewportHeight = canvasBounds.height() / currentScale
        val newViewportLeft = max(0f, min(left, canvasBounds.width() - newViewportWidth))
        val newViewportTop = max(0f, min(top, canvasBounds.height() - newViewportHeight))

        // If the size and position didn't change lets not do anything
        if (newViewportWidth == viewportBounds.width()
            && newViewportHeight == viewportBounds.height()
            && newViewportLeft == viewportBounds.left
            && newViewportTop == viewportBounds.top
        ) return

        viewportBounds.set(
            newViewportLeft,
            newViewportTop,
            newViewportLeft + newViewportWidth,
            newViewportTop + newViewportHeight
        )

        // Update canvas matrix
        canvasMatrix.setRectToRect(viewportBounds, canvasBounds, Matrix.ScaleToFit.FILL)

        invalidate()
    }

    /**
     * Zooms to new scale and pans in the direction of x, y
     * Note: Doesn't fully center on x, y
     *  @param x - offset within original viewport at original scale
     *  @param y - offset within original viewport at original scale
     *  @param newScale - scale factor to zoom by
     */
    private fun zoomTo(x: Float, y: Float, newScale: Float) {
        val focusX = viewportBounds.left + ((viewportBounds.width() * x) / canvasBounds.width())
        val focusY = viewportBounds.top + ((viewportBounds.height() * y) / canvasBounds.height())

        currentScale = newScale

        val newViewportWidth = canvasBounds.width() / currentScale
        val newViewportHeight = canvasBounds.height() / currentScale

        panTo(
            focusX - ((newViewportWidth * x) / canvasBounds.width()),
            focusY - ((newViewportHeight * y) / canvasBounds.height())
        )
    }

    fun draw(
        canvas: Canvas,
        externalDraw: (Canvas) -> Unit
    ) {
        canvas.withMatrix(canvasMatrix) {
            externalDraw(canvas)
        }

        drawMiniMap(
            canvas = canvas,
            externalDraw = externalDraw
        )
    }

    private fun drawMiniMap(
        canvas: Canvas,
        externalDraw: (Canvas) -> Unit
    ) {
        if (miniMapEnabled) {
            canvas.withMatrix(miniMapMatrix) {
                externalDraw(canvas)

                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 2f
                paint.color = Color.Red.toArgb()
                canvas.drawRect(canvasBounds, paint)

                paint.color = Color.Blue.toArgb()
                canvas.drawRect(viewportBounds, paint)

                paint.color = Color.Magenta.toArgb()

            }
        }
    }

    class Factory(
        private val configuration: ZoomableConfiguration
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            modelClass: Class<T>,
            extras: CreationExtras
        ): T {
            return ZoomableViewModel(
                configuration = configuration
            ) as T
        }
    }
}