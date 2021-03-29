package com.pedrobneto.bar

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import androidx.annotation.DimenRes

private typealias OnDragListener = (view: View) -> Unit

class OnDragTouchListener(
    private val view: View,
    private val parent: View,
    private val elevateWhenTouched: Boolean = true,
    private val movementType: MovementType = MovementType.ANY
) : OnTouchListener {

    //region Flags
    private var isDragging = false
    private var isInitialized = false
    //endregion

    //region Values
    private var width = 0
    private var xWhenAttached = 0f
    private var maxLeft = 0f
    private var maxRight = 0f
    private var dX = 0f
    private var height = 0
    private var yWhenAttached = 0f
    private var maxTop = 0f
    private var maxBottom = 0f
    private var dY = 0f

    private val minimalHorizontalMargin = view.resources.getDimension(R.dimen.default_handler_horizontal_margin)

    private var marginStart = minimalHorizontalMargin
    private var marginTop = 0f
    private var marginEnd = minimalHorizontalMargin
    private var marginBottom = 0f

    private var viewElevation = 0f
    //endregion

    //region Listeners
    private var onStartDragging: OnDragListener? = null
    private var onEndDragging: OnDragListener? = null
    private var onDrag: OnDragListener? = null
    //endregion

    private fun updateBounds() {
        updateViewBounds()
        updateParentBounds()
        isInitialized = true
    }

    private fun updateViewBounds() {
        width = view.width
        xWhenAttached = view.x
        dX = 0f
        height = view.height
        yWhenAttached = view.y
        dY = 0f

        viewElevation = view.elevation
    }

    private fun updateParentBounds() {
        maxLeft = marginStart
        maxRight = parent.width - marginEnd
        maxTop = marginTop
        maxBottom = parent.height - marginBottom
    }

    private fun onDragFinish() {
        onEndDragging?.invoke(view)
        if (elevateWhenTouched) view.elevation = viewElevation

        dX = 0f
        dY = 0f
        isDragging = false
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (isDragging) {
            val bounds = FloatArray(4)
            // LEFT
            bounds[0] = event.rawX + dX
            if (bounds[0] < maxLeft) {
                bounds[0] = maxLeft
            }
            // RIGHT
            bounds[2] = bounds[0] + width
            if (bounds[2] > maxRight) {
                bounds[2] = maxRight
                bounds[0] = bounds[2] - width
            }
            // TOP
            bounds[1] = event.rawY + dY
            if (bounds[1] < maxTop) {
                bounds[1] = maxTop
            }
            // BOTTOM
            bounds[3] = bounds[1] + height
            if (bounds[3] > maxBottom) {
                bounds[3] = maxBottom
                bounds[1] = bounds[3] - height
            }
            when (event.action) {
                MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> onDragFinish()
                MotionEvent.ACTION_MOVE -> {
                    val animator = view.animate()
                    if (movementType != MovementType.VERTICAL) animator.x(bounds[0])
                    if (movementType != MovementType.HORIZONTAL) animator.y(bounds[1])
                    animator.setDuration(0).start()

                    onDrag?.invoke(view)
                }
            }

            return true
        } else {
            if (event.action == MotionEvent.ACTION_DOWN) {
                isDragging = true
                if (!isInitialized) updateBounds()

                dX = v.x - event.rawX
                dY = v.y - event.rawY

                if (elevateWhenTouched) {
                    view.elevation = view.resources.getDimension(R.dimen.default_drag_elevation)
                }
                onStartDragging?.invoke(view)

                return true
            }
        }
        return false
    }

    fun setOnStartDraggingListener(func: OnDragListener) = apply {
        this.onStartDragging = func
    }

    fun setOnDragListener(func: OnDragListener) = apply {
        this.onDrag = func
    }

    fun setOnEndDraggingListener(func: OnDragListener) = apply {
        this.onEndDragging = func
    }

    fun setMarginStart(@DimenRes marginStart: Int) = apply {
        setMarginStart(view.resources.getDimension(marginStart))
    }

    fun setMarginTop(@DimenRes marginTop: Int) = apply {
        setMarginTop(view.resources.getDimension(marginTop))
    }

    fun setMarginEnd(@DimenRes marginEnd: Int) = apply {
        setMarginEnd(view.resources.getDimension(marginEnd))
    }

    fun setMarginBottom(@DimenRes marginBottom: Int) = apply {
        setMarginBottom(view.resources.getDimension(marginBottom))
    }

    fun setMarginStart(marginStart: Float) = apply {
        if (marginStart > minimalHorizontalMargin) this.marginStart = marginStart
    }

    fun setMarginTop(marginTop: Float) = apply {
        this.marginTop = marginTop
    }

    fun setMarginEnd(marginEnd: Float) = apply {
        if (marginEnd > minimalHorizontalMargin) this.marginEnd = marginEnd
    }

    fun setMarginBottom(marginBottom: Float) = apply {
        this.marginBottom = marginBottom
    }

    enum class MovementType {
        HORIZONTAL, VERTICAL, ANY
    }
}