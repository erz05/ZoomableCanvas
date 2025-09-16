package com.erz.zoomablecanvas.lib

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.withMatrix
import androidx.dynamicanimation.animation.FlingAnimation
import androidx.dynamicanimation.animation.FloatValueHolder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

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
    private val canvasMatrix = Matrix()

    /**
     * RectF that represents the canvas bounds
     * Used to calculate the canvasMatrix
     */
    private var _canvasBounds = RectF()

    /**
     * RectF that represent the visible bounds on the canvas
     * Used to calculate the canvasMatrix
     */
    private var _viewportBounds = RectF()

    /**
     * RectF that represents the bounds used to constraint the viewport
     */
    private var constraintBounds: RectF? = null

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
     * FloatValueHolder to hold on the x fling animation value
     */
    private val xFlingFloatValueHolder = FloatValueHolder()

    /**
     * Fling animation to animate pan to in the x direction
     */
    private val xFlingAnimation = FlingAnimation(xFlingFloatValueHolder).apply {
        addUpdateListener { _, value, _ ->
            panTo(value, _viewportBounds.top)
        }
    }

    /**
     * FloatValueHolder to hold on the y fling animation value
     */
    private val yFlingFloatValueHolder = FloatValueHolder()

    /**
     * Fling animation to animate pan to in the y direction
     */
    private val yFlingAnimation = FlingAnimation(yFlingFloatValueHolder).apply {
        addUpdateListener { _, value, _ ->
            panTo(_viewportBounds.left, value)
        }
    }

    /**
     * Value animator to animate the double tap to zoom
     */
    private val doubleTapZoomValueAnimator = object : ValueAnimator() {
        var focusX = 0f
        var focusY = 0f

        init {
            duration = configuration.doubleTapZoomDuration
            addUpdateListener { updatedAnimation ->
                zoomTo(focusX, focusY, updatedAnimation.animatedValue as Float)
            }
        }
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

    override fun onGloballyPositioned(
        composableWidth: Float,
        composableHeight: Float,
        constraintBounds: RectF?
    ) {
        _canvasBounds.set(
            /* left = */ 0f,
            /* top = */ 0f,
            /* right = */ composableWidth,
            /* bottom = */ composableHeight
        )

        this.constraintBounds = constraintBounds

        // Todo ERZ - should we restore the viewport?
        // To restore viewPort we need to remember the center position of the last viewport and the last scale
        // we will use that to center the new viewport and to scale

        panTo()
    }

    override fun invalidate() {
        invalidate.update {
            System.currentTimeMillis()
        }
    }

    override fun getCanvasBounds(): RectF {
        return _canvasBounds
    }

    override fun getViewportBounds(): RectF {
        return _viewportBounds
    }

    // Todo ERZ - should this be done on init?
    fun setup(
    ) {
        // Reset viewPort
        currentScale = minScale
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

    fun onDown(event: MotionEvent) {
        xFlingAnimation.cancel()
        yFlingAnimation.cancel()
    }

    // Used to pan viewport inside of bounds
    fun onTranslate(
        distanceX: Float,
        distanceY: Float,
    ) {
        panTo(
            _viewportBounds.left + ((distanceX * _viewportBounds.width()) / _canvasBounds.width()),
            _viewportBounds.top + ((distanceY * _viewportBounds.height()) / _canvasBounds.height())
        )
    }

    fun onFling(
        velocityX: Float,
        velocityY: Float
    ) {
        if (configuration.canFling.not()
            || canScroll().not()
            || _canvasBounds.isEmpty
            || _viewportBounds.isEmpty) return

        val xStartValue = _viewportBounds.left
        val xMinValue = getXFlingMinValue()
        val xMaxValue = getXFlingMaxValue()
        if (xMaxValue > xMinValue && xStartValue >= xMinValue && xStartValue <= xMaxValue) {
            xFlingFloatValueHolder.value = xStartValue
            xFlingAnimation.setStartVelocity((-velocityX * _viewportBounds.width()) / _canvasBounds.width())
            xFlingAnimation.setMinValue(xMinValue)
            xFlingAnimation.setMaxValue(xMaxValue)
            xFlingAnimation.start()
        }

        val yStartValue = _viewportBounds.top
        val yMinValue = getYFlingMinValue()
        val yMaxValue = getYFlingMaxValue()
        if (yMaxValue > yMinValue && yStartValue >= yMinValue && yStartValue <= yMaxValue) {
            yFlingFloatValueHolder.value = yStartValue
            yFlingAnimation.setStartVelocity((-velocityY * _viewportBounds.height()) / _canvasBounds.height())
            yFlingAnimation.setMinValue(yMinValue)
            yFlingAnimation.setMaxValue(yMaxValue)
            yFlingAnimation.start()
        }
    }

    fun onDoubleTap(
        event: MotionEvent
    ) {
        if (configuration.canDoubleTapToZoom.not()) return
        var newScale = 2f.pow(floor(ln((currentScale * 2f)) / ln(2f)))
        if (newScale >= maxScale) newScale = minScale
        doubleTapZoomValueAnimator.setFloatValues(currentScale, newScale)
        doubleTapZoomValueAnimator.focusX = event.x
        doubleTapZoomValueAnimator.focusY = event.y
        doubleTapZoomValueAnimator.start()
    }

    fun canPan() = currentScale == minScale

    /**
     * Boolean to determine if the viewport bounds can scroll within the view bounds
     * This is used to determine the [canScrollHorizontally] needed for pagers to prevent paging while a user is zoomed in
     */
    private fun canScroll() = currentScale != minScale

    private fun getXFlingMinValue(): Float {
        return constraintBounds?.left ?: Float.MIN_VALUE
    }

    private fun getXFlingMaxValue(): Float {
        return constraintBounds?.let {
            it.width() - _viewportBounds.width()
        } ?: Float.MAX_VALUE
    }

    private fun getYFlingMinValue(): Float {
        return constraintBounds?.top ?: Float.MIN_VALUE
    }

    private fun getYFlingMaxValue(): Float {
        return constraintBounds?.let {
            it.height() - _viewportBounds.height()
        } ?: Float.MAX_VALUE
    }

    /**
     * This Function updates the position of the viewport given the new left and top position
     * this function will keep the viewport constrained to the view bounds
     * finally this function updates the canvas matrix and calls invalidate on the view
     * @param left - the new left position of the viewport
     * @param top - the new top position of the viewport
     */
    private fun panTo(
        left: Float = _viewportBounds.left,
        top: Float = _viewportBounds.top
    ) {
        val newViewportWidth = _canvasBounds.width() / currentScale
        val newViewportHeight = _canvasBounds.height() / currentScale
        val newViewportLeft = max(0f, min(left, _canvasBounds.width() - newViewportWidth))
        val newViewportTop = max(0f, min(top, _canvasBounds.height() - newViewportHeight))

        // If the size and position didn't change lets not do anything
        if (newViewportWidth == _viewportBounds.width()
            && newViewportHeight == _viewportBounds.height()
            && newViewportLeft == _viewportBounds.left
            && newViewportTop == _viewportBounds.top
        ) return

        _viewportBounds.set(
            newViewportLeft,
            newViewportTop,
            newViewportLeft + newViewportWidth,
            newViewportTop + newViewportHeight
        )

        constraintBounds?.let {
            val maxLeft = it.centerX() - (_viewportBounds.width() / 2f)
            val maxTop = it.centerY() - (_viewportBounds.height() / 2f)
            _viewportBounds.offsetTo(
                if (_viewportBounds.width() > it.width()) maxLeft else max(it.left, min(_viewportBounds.left, it.right - _viewportBounds.width())),
                if (_viewportBounds.height() > it.height()) maxTop else max(it.top, min(_viewportBounds.top, it.bottom - _viewportBounds.height()))
            )
        }

        // Update canvas matrix
        canvasMatrix.setRectToRect(_viewportBounds, _canvasBounds, Matrix.ScaleToFit.FILL)

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
        val focusX = _viewportBounds.left + ((_viewportBounds.width() * x) / _canvasBounds.width())
        val focusY = _viewportBounds.top + ((_viewportBounds.height() * y) / _canvasBounds.height())

        currentScale = newScale

        val newViewportWidth = _canvasBounds.width() / currentScale
        val newViewportHeight = _canvasBounds.height() / currentScale

        panTo(
            focusX - ((newViewportWidth * x) / _canvasBounds.width()),
            focusY - ((newViewportHeight * y) / _canvasBounds.height())
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
                canvas.drawRect(_canvasBounds, paint)

                paint.color = Color.Blue.toArgb()
                canvas.drawRect(_viewportBounds, paint)

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