package com.erz.zoomablecanvas.lib

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import kotlin.math.max

internal class ZoomableGestureDetector(
    context: Context,
    private val listener: ZoomableGestureDetectorListener
) {

    private enum class ActionType {
        NONE,
        DRAW,
        TRANSFORM,
        LONG_PRESS
    }

    private var totalPointerCount = 0
    private var lastX: Float? = null
    private var lastY: Float? = null
    private var actionType = ActionType.NONE

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {

        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            if (actionType == ActionType.NONE) {
                actionType = if (e2.pointerCount == 1) ActionType.DRAW else ActionType.TRANSFORM
            }

            when(actionType) {
                ActionType.DRAW -> {
                    if (e2.pointerCount == 1) {
                        listener.onMove(
                            lastX = lastX,
                            lastY = lastY,
                            currentX = e2.x,
                            currentY = e2.y
                        )
                    }
                }
                ActionType.TRANSFORM -> {
                    listener.onTranslate(
                        distanceX = distanceX,
                        distanceY = distanceY
                    )
                }
                else -> Unit
            }

            lastX = e2.x
            lastY = e2.y

            return true
        }

        override fun onLongPress(e: MotionEvent) {
            actionType = ActionType.LONG_PRESS

            listener.onLongPress(
                touchX = e.x,
                touchY = e.y
            )
        }
    })

    private val scaleGestureDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            if (actionType == ActionType.TRANSFORM) {
                listener.onScale(
                    currentX = detector.focusX,
                    currentY = detector.focusY,
                    scale = detector.scaleFactor
                )
            }

            return true
        }
    })

    fun onTouchEvent(
        motionEvent: MotionEvent
    ): Boolean {
        scaleGestureDetector.onTouchEvent(motionEvent)
        gestureDetector.onTouchEvent(motionEvent)

        when (motionEvent.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                totalPointerCount = motionEvent.pointerCount
                lastX = null
                lastY = null
                actionType = ActionType.NONE
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                totalPointerCount = max(totalPointerCount, motionEvent.pointerCount)
            }
            MotionEvent.ACTION_UP -> {
                if (actionType == ActionType.NONE) {
                    listener.onUp(
                        currentX = motionEvent.x,
                        currentY = motionEvent.y,
                        totalPointerCount = totalPointerCount
                    )
                }

                listener.onDone()
            }
        }

        return true
    }
}