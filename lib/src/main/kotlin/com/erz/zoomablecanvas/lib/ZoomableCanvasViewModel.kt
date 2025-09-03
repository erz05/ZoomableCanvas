package com.erz.zoomablecanvas.lib

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.withMatrix
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.max
import kotlin.math.min

class ZoomableCanvasViewModel() : ViewModel() {

    companion object {

        /**
         * Constant float used for the min scale
         * At this scale the viewport will match the view bounds
         */
        private const val DEFAULT_MIN_SCALE = 1f

        /**
         * Constant float used for the max scale
         * At this scale the viewport will the smallest possible size
         * This number was calculated to match our legacy tile view library max zoom
         */
        private const val DEFAULT_MAX_SCALE = 13f
    }

    val invalidate = MutableStateFlow(0L)

    val imageBounds = RectF()

    /**
     * Matrix used to apply transformations to the views canvas
     */
    val canvasMatrix = Matrix()

    /**
     * RectF that represents the canvas bounds
     * Used to calculate the canvasMatrix
     */
    var viewBounds = RectF()

    /**
     * RectF that represent the visible bounds on the canvas
     * Used to calculate the canvasMatrix
     */
    var viewportBounds = RectF()

    /**
     * Float used to constrain the [currentScale] to the min scale
     */
    private val minScale = DEFAULT_MIN_SCALE

    /**
     * Float used to constrain the [currentScale] to the max scale
     */
    private val maxScale = DEFAULT_MAX_SCALE

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
     * Matrix to scale down the viewport and view bounds for the mini map
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
        viewBounds.set(
            /* left = */ 0f,
            /* top = */ 0f,
            /* right = */ composableWidth,
            /* bottom = */ composableHeight
        )

        val min = min(composableWidth, composableHeight).toFloat()
        imageBounds.set(
            /* left = */ (composableWidth / 2f) - (min / 2f),
            /* top = */ (composableHeight / 2f) - (min / 2f),
            /* right = */ (composableWidth / 2f) + (min / 2f),
            /* bottom = */ (composableHeight / 2f) + (min / 2f)
        )

        // Todo ERZ - should we restore the viewport?

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
            viewportBounds.left + ((distanceX * viewportBounds.width()) / viewBounds.width()),
            viewportBounds.top + ((distanceY * viewportBounds.height()) / viewBounds.height())
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
        val newViewportWidth = viewBounds.width() / currentScale
        val newViewportHeight = viewBounds.height() / currentScale
        val newViewportLeft = max(0f, min(left, viewBounds.width() - newViewportWidth))
        val newViewportTop = max(0f, min(top, viewBounds.height() - newViewportHeight))

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

        // Constraint the viewportBounds to the imageBounds
        val maxLeft = imageBounds.centerX() - (viewportBounds.width() / 2f)
        val maxTop = imageBounds.centerY() - (viewportBounds.height() / 2f)
        viewportBounds.offsetTo(
            if (viewportBounds.width() > imageBounds.width()) maxLeft else max(imageBounds.left, min(viewportBounds.left, imageBounds.right - viewportBounds.width())),
            if (viewportBounds.height() > imageBounds.height()) maxTop else max(imageBounds.top, min(viewportBounds.top, imageBounds.bottom - viewportBounds.height()))
        )

        // Update canvas matrix
        canvasMatrix.setRectToRect(viewportBounds, viewBounds, Matrix.ScaleToFit.FILL)

        invalidate()
    }

    private fun invalidate() {
        invalidate.update {
            System.currentTimeMillis()
        }
    }

    /**
     * Zooms to new scale and pans in the direction of x, y
     * Note: Doesn't fully center on x, y
     *  @param x - offset within original viewport at original scale
     *  @param y - offset within original viewport at original scale
     *  @param newScale - scale factor to zoom by
     */
    private fun zoomTo(x: Float, y: Float, newScale: Float) {
        val focusX = viewportBounds.left + ((viewportBounds.width() * x) / viewBounds.width())
        val focusY = viewportBounds.top + ((viewportBounds.height() * y) / viewBounds.height())

        currentScale = newScale

        val newViewportWidth = viewBounds.width() / currentScale
        val newViewportHeight = viewBounds.height() / currentScale

        panTo(
            focusX - ((newViewportWidth * x) / viewBounds.width()),
            focusY - ((newViewportHeight * y) / viewBounds.height())
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
                canvas.drawRect(viewBounds, paint)

                paint.color = Color.Blue.toArgb()
                canvas.drawRect(viewportBounds, paint)

                paint.color = Color.Green.toArgb()
                canvas.drawRect(imageBounds, paint)

                paint.color = Color.Magenta.toArgb()

            }
        }
    }

    private fun mapTouchXValue(
        touchX: Float
    ): Float {
        return ((touchX * viewportBounds.width()) / viewBounds.width()) + viewportBounds.left
    }

    private fun mapTouchYValue(
        touchY: Float
    ): Float {
        return ((touchY * viewportBounds.height()) / viewBounds.height()) + viewportBounds.top
    }
}