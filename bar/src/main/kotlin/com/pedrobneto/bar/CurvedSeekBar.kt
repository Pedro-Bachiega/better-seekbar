package com.pedrobneto.bar

import android.content.Context
import android.util.AttributeSet
import kotlin.math.pow

class CurvedSeekBar : BetterSeekBar {

    //region Coordinates
    override val initialY: Float
        get() = graphView.measuredHeight.toFloat() - (lineStrokeSize / 2)

    override val finalY: Float
        get() = 0f

    /**
     * The first x coordinate to anchor the curve to.
     */
    private var _anchorX1: Float = 0f
    private val anchorX1: Float
        get() = _anchorX1.x + paddingStart

    /**
     * The second x coordinate to anchor the curve to.
     */
    private var _anchorX2: Float = 0f
    private val anchorX2: Float
        get() = _anchorX2.x - paddingEnd

    /**
     * The first y coordinate to anchor the curve to.
     */
    private var _anchorY1: Float = 0f
    private val anchorY1: Float
        get() = initialY - _anchorY1.y

    /**
     * The second y coordinate to anchor the curve to.
     */
    private var _anchorY2: Float = 0f
    private val anchorY2: Float
        get() = initialY - _anchorY2.y
    //endregion

    //region Constructors
    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : this(context, attrs, defStyleAttr, R.style.CurvedSeekBar)

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes)
    //endregion

    /**
     * Method to get the values from the xml.
     */
    override fun getValues(
        set: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) {
        super.getValues(set, defStyleAttr, defStyleRes)

        val typedArray =
            context.obtainStyledAttributes(
                set,
                R.styleable.CurvedSeekBar,
                defStyleAttr,
                defStyleRes
            )

        absoluteHeight = typedArray.getFloat(R.styleable.CurvedSeekBar_finalY, finalY)
        absoluteWidth = typedArray.getFloat(R.styleable.CurvedSeekBar_finalX, finalX)

        _anchorX1 = typedArray.getFloat(R.styleable.CurvedSeekBar_anchorX1, initialX)
        _anchorY1 = typedArray.getFloat(R.styleable.CurvedSeekBar_anchorY1, initialY)
        _anchorX2 = typedArray.getFloat(R.styleable.CurvedSeekBar_anchorX2, finalX)
        _anchorY2 = typedArray.getFloat(R.styleable.CurvedSeekBar_anchorY2, finalY)

        typedArray.recycle()
    }

    override fun fixHandlerY() {
        handlerView.y = offset + getYForX(handlerCenterX)
    }

    /**
     * @param progress Progress based on the handler's position on the curve.
     * @return The x coordinate for the progress based on Bezier's cubic equation.
     */
    override fun getXForProgress(progress: Float): Float {
        var newX = (1 - progress).pow(3) * initialX
        newX += 3 * (1 - progress).pow(2) * progress * anchorX1
        newX += 3 * (1 - progress) * progress.pow(2) * anchorX2
        newX += progress.pow(3) * finalX

        return newX
    }

    /**
     * @param progress Progress based on the handler's position on the curve.
     * @return The y coordinate for the progress based on Bezier's cubic equation.
     */
    override fun getYForProgress(progress: Float): Float {
        var newY = (1 - progress).pow(3) * initialY
        newY += 3 * (1 - progress).pow(2) * progress * anchorY1
        newY += 3 * (1 - progress) * progress.pow(2) * anchorY2
        newY += progress.pow(3) * finalY

        return newY
    }
}