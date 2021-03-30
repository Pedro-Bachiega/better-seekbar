package com.pedrobneto.bar

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Space
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.getIntOrThrow
import androidx.core.graphics.ColorUtils
import androidx.core.view.doOnPreDraw
import androidx.core.view.updateLayoutParams
import kotlin.math.pow
import kotlin.math.roundToInt

typealias ProgressListener = (Float) -> Unit
typealias PointSelectionListener = (Int) -> Unit
typealias SegmentSelectionListener = (Int) -> Unit

private const val ANIMATION_DURATION = 150L

class CurvedSeekBar : FrameLayout {

    //region Views
    /**
     * The layout used for the segments.
     */
    private val segmentLayout = LinearLayout(context)

    /**
     * The view used for the Graph.
     */
    private val graphView = GraphView(context)

    /**
     * The view used for the Handler.
     */
    private val handlerView = View(context)
    //endregion

    //region Measurements
    /**
     * The Handler's size.
     */
    private var handlerSize: Int = 0

    /**
     * The SeekBar's line height.
     */
    private var lineStrokeSize: Float = 0f

    /**
     * The absolute width of the canvas the curve was drawn in.
     * If none provided, will be the same as the final x coordinate.
     */
    private var absoluteWidth: Float = 0f

    /**
     * The absolute height of the canvas the curve was drawn in.
     * If none provided, will be the same as the final y coordinate.
     */
    private var absoluteHeight: Float = 0f

    /**
     * The start margin for the handler, segments and highlight.
     */
    private var handlerMarginStart: Float = 0f

    /**
     * The end margin for the handler, segments and highlight.
     */
    private var handlerMarginEnd: Float = 0f

    /**
     * The width of the segment's divider.
     */
    private var segmentDivider: Int = 0

    /**
     * The graph's top margin to the segments.
     */
    private var _graphMarginTop: Float = 0f

    /**
     * The graph's top offset.
     * Will return 0 if no segments are added because we no longer need the offset.
     *
     * @see _graphMarginTop
     */
    private val offset: Float
        get() = if (segmentQuantity == 0) 0f else _graphMarginTop
    //endregion

    //region Coordinates
    /**
     * The x coordinate of the curve's start.
     */
    private val initialX: Float = 0f + paddingStart

    /**
     * The y coordinate of the curve's start.
     */
    private val initialY: Float
        get() = graphView.measuredHeight.toFloat() - (lineStrokeSize / 2)

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

    /**
     * The x coordinate of the curve's end.
     */
    private val finalX: Float
        get() = measuredWidth.toFloat() - paddingEnd

    /**
     * The y coordinate of the curve's end.
     */
    private val finalY: Float = 0f

    /**
     * The handler's center x coordinate.
     */
    private val handlerCenterX: Float
        get() = handlerView.x + (handlerView.measuredWidth / 2)

    /**
     * Minimum value the x coordinate of the handler can achieve after measuring horizontal margins.
     */
    private val minAchievableX: Float
        get() = initialX + handlerMarginStart

    /**
     * Maximum value the x coordinate of the handler can achieve after measuring horizontal margins.
     */
    private val maxAchievableX: Float
        get() = finalX - handlerMarginEnd - handlerView.measuredWidth

    /**
     * Maximum horizontal value the handler can slide on.
     */
    private val maxHorizontalSpace: Float
        get() = maxAchievableX - handlerMarginStart

    /**
     * Extension property used to scale the y coordinate up or down according to the
     * absolute height.
     *
     * @see absoluteHeight
     */
    private val Float.y: Float
        get() = (this * graphView.measuredHeight) / absoluteHeight

    /**
     * Extension property used to scale the x coordinate up or down according to the
     * absolute width.
     *
     * @see absoluteWidth
     */
    private val Float.x: Float
        get() = (this * graphView.measuredWidth) / absoluteWidth
    //endregion

    //region Color
    /**
     * Color to be used on the SeekBar's line.
     */
    @ColorInt
    private var barColor = Color.YELLOW

    /**
     * Color to be used on the handler.
     */
    @ColorInt
    private var handlerColor = Color.BLUE
    //endregion

    //region Points
    /**
     * Property used to tell how many segments we need to create behind the curve.
     */
    var pointQuantity: Int = 0

    /**
     * Property used to calculate the required value of the x coordinate to achieve another point.
     */
    private val xPerPoint: Float
        get() = (maxAchievableX - minAchievableX) / pointQuantity

    /**
     * Last point selected.
     * Will not be set if the current selected point is equal to the last one.
     */
    private var lastPointSelected: Int = 0
        set(value) {
            if (value != field && value > 0 && value <= pointQuantity) {
                field = value
                onPointSelectedUpdated?.invoke(value - 1)
            }
        }

    /**
     * Map used to select the preferred point when the corresponding segment is clicked.
     * If not set, the preferred point will default to the middle of the segment, if possible.
     */
    private var preferredPointBySegment: Map<Int, Int> = mapOf()
    //endregion

    //region Segments
    /**
     * Property used to tell how many segments we need to create behind the curve.
     * Will reset the curve and the segments whenever this is set to another value.
     */
    var segmentQuantity: Int = 0
        set(value) {
            field = value
            setupSegments()
            setupGraph()
        }

    /**
     * Index of the last segment selected.
     */
    private var lastSegmentSelected: Int = 0

    /**
     * List of segment views.
     * Used to change its background whenever the handler is hovering it.
     */
    private val segments = mutableListOf<View>()

    /**
     * List of segment views.
     * Used to determine whether or not we are animating that view already.
     */
    private val segmentsAnimating = mutableListOf<View>()

    /**
     * Resource used on the segments' backgrounds.
     */
    @DrawableRes
    private var segmentResource = -1
    //endregion

    //region Highlight
    /**
     * Minimum value to be achieved with the highlight's alpha.
     */
    private var minHighlightAlpha: Float = 0f

    /**
     * Maximum value to be achieved with the highlight's alpha.
     */
    private var maxHighlightAlpha: Float = 0.87f

    /**
     * Color of the highlight, defaults to the color used to draw the line.
     *
     * @see barColor
     */
    @ColorInt
    private var highlightColor: Int = barColor
    //endregion

    //region Handler
    /**
     * Resource used for the handler's background.
     */
    @DrawableRes
    private var handlerResource = -1
    //endregion

    //region Flags
    /**
     * Flag to determine whether or not we are animating the view's changes
     */
    var animationsEnabled = true
    //endregion

    //region Progress
    /**
     * Extension property to calculate the progress of the handler based on it's x coordinate on
     * the curve.
     */
    private val View.progress: Float
        get() {
            val realX = x - initialX - handlerMarginStart
            return realX / maxHorizontalSpace
        }

    /**
     * Property to store, notify and be used whenever the calculations need the effective progress.
     */
    private var effectiveProgress: Float = 0f
        set(value) {
            field = value
            onProgressUpdated?.invoke(value)

            if (isLaidOut) {
                fixHandlerY()
                updateHighlight()
                updateSelectedSegment()
                updatePointSelected(false)
            }
        }
    //endregion

    //region Listeners
    /**
     * Set the event that will be triggered when ANY change to the progress is made.
     */
    private var onProgressUpdated: ProgressListener? = null

    /**
     * Set the event that will be triggered when the LAST change to the progress is made.
     */
    private var onProgressStopChanging: ProgressListener? = null

    /**
     * Set the event that will be triggered when ANY point is selected.
     */
    private var onPointSelectedUpdated: PointSelectionListener? = null

    /**
     * Set the event that will be triggered when the LAST point is selected.
     */
    private var onPointSelectedStopUpdating: PointSelectionListener? = null

    /**
     * Set the event that will be triggered when a segment is clicked.
     */
    private var onSegmentSelected: SegmentSelectionListener? = null
    //endregion

    //region Constructors
    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : this(context, attrs, defStyleAttr, R.style.BezierSeekBar)

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        getValues(attrs, defStyleAttr, defStyleRes)

        clipChildren = false
        clipToPadding = false
        setBackgroundColor(Color.TRANSPARENT)

        setupSegments()
        setupGraph()
        setupHandler()

        addView(segmentLayout)
        addView(graphView)
        addView(handlerView)
        adjustLayout()
    }
    //endregion

    /**
     * Making sure the vertical padding is never less than the minimum.
     */
    override fun setPaddingRelative(start: Int, top: Int, end: Int, bottom: Int) {
        val defaultVerticalPadding = handlerSize / 2

        val topPadding: Int =
            if (top < defaultVerticalPadding) defaultVerticalPadding else top
        val bottomPadding: Int =
            if (bottom < defaultVerticalPadding) defaultVerticalPadding else bottom

        super.setPaddingRelative(start, topPadding, end, bottomPadding)
    }

    /**
     * Making sure the vertical padding is never less than the minimum.
     */
    override fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {
        val defaultVerticalPadding = handlerSize / 2

        val topPadding: Int =
            if (top < defaultVerticalPadding) defaultVerticalPadding else top
        val bottomPadding: Int =
            if (bottom < defaultVerticalPadding) defaultVerticalPadding else bottom

        super.setPadding(left, topPadding, right, bottomPadding)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        fixHandlerY()
        updateSelectedSegment()
    }

    /**
     * Method to get the values from the xml.
     */
    private fun getValues(
        set: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) {
        val typedArray =
            context.obtainStyledAttributes(
                set,
                R.styleable.CurvedSeekBar,
                defStyleAttr,
                defStyleRes
            )

        handlerMarginStart = typedArray.getDimension(
            R.styleable.CurvedSeekBar_handlerSize,
            resources.getDimension(R.dimen.default_handler_horizontal_margin)
        )
        handlerMarginEnd = typedArray.getDimension(
            R.styleable.CurvedSeekBar_handlerSize,
            resources.getDimension(R.dimen.default_handler_horizontal_margin)
        )

        handlerSize = typedArray.getDimensionPixelSize(
            R.styleable.CurvedSeekBar_handlerSize,
            resources.getDimensionPixelSize(R.dimen.default_handler_size)
        )
        lineStrokeSize = typedArray.getDimension(R.styleable.CurvedSeekBar_barStrokeSize, 10f)

        segmentDivider =
            typedArray.getDimensionPixelSize(R.styleable.CurvedSeekBar_segmentDivider, 0)

        segmentResource =
            typedArray.getResourceId(
                R.styleable.CurvedSeekBar_segmentResource,
                R.drawable.background_segment
            )

        handlerResource =
            typedArray.getResourceId(
                R.styleable.CurvedSeekBar_handlerResource,
                R.drawable.background_handler
            )

        absoluteHeight = typedArray.getFloat(R.styleable.CurvedSeekBar_finalY, finalY)
        absoluteWidth = typedArray.getFloat(R.styleable.CurvedSeekBar_finalX, finalX)

        _graphMarginTop = typedArray.getDimension(R.styleable.CurvedSeekBar_graphMarginTop, 0f)

        _anchorX1 = typedArray.getFloat(R.styleable.CurvedSeekBar_anchorX1, initialX)
        _anchorY1 = typedArray.getFloat(R.styleable.CurvedSeekBar_anchorY1, initialY)
        _anchorX2 = typedArray.getFloat(R.styleable.CurvedSeekBar_anchorX2, finalX)
        _anchorY2 = typedArray.getFloat(R.styleable.CurvedSeekBar_anchorY2, finalY)

        barColor = typedArray.getColor(R.styleable.CurvedSeekBar_barColor, Color.BLACK)
        handlerColor = typedArray.getColor(R.styleable.CurvedSeekBar_handlerColor, Color.MAGENTA)
        highlightColor = typedArray.getColor(R.styleable.CurvedSeekBar_highlightColor, barColor)

        minHighlightAlpha = typedArray.getFloat(R.styleable.CurvedSeekBar_minHighlightAlpha, 0f)
        typedArray.getFloat(R.styleable.CurvedSeekBar_maxHighlightAlpha, 0.87f).let {
            if (it < maxHighlightAlpha) maxHighlightAlpha = it
        }

        if (typedArray.hasValue(R.styleable.CurvedSeekBar_segments) && segmentQuantity == 0) {
            segmentQuantity = typedArray.getIntOrThrow(R.styleable.CurvedSeekBar_segments)
        }

        effectiveProgress = typedArray.getFloat(R.styleable.CurvedSeekBar_initialProgress, 0f)

        typedArray.recycle()
    }

    /**
     * Method to setup the segment's attributes, as well as adding however many segments are needed.
     */
    private fun setupSegments() {
        segments.clear()
        segmentLayout.removeAllViews()
        segmentLayout.orientation = LinearLayout.HORIZONTAL
        segmentLayout.layoutParams =
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT).apply {
                marginStart = (handlerMarginStart + (handlerSize / 2)).roundToInt()
                marginEnd = (handlerMarginEnd + (handlerSize / 2)).roundToInt()
            }

        (0 until segmentQuantity).forEachIndexed { index, _ ->
            if (index > 0) {
                val divider = Space(context)
                divider.layoutParams =
                    LayoutParams(segmentDivider, LinearLayout.LayoutParams.MATCH_PARENT)
                segmentLayout.addView(divider)
            }

            val segmentView = View(context)
            segmentView.setBackgroundResource(segmentResource)
            segmentView.layoutParams =
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            segmentView.alpha = if (index == lastSegmentSelected) 1f else 0.5f

            segmentView.setOnClickListener {
                if (pointQuantity > 0 && preferredPointBySegment.containsKey(index)) {
                    preferredPointBySegment[index]?.run(::setSelectedPoint)
                } else {
                    val newX = it.x + handlerMarginStart + (it.measuredWidth / 2)
                    adjustHandlerPosition(newX, animationsEnabled) {
                        lastSegmentSelected = index
                        onSegmentSelected?.invoke(index)
                    }
                }
            }

            segmentLayout.addView(segmentView)
            segments.add(segmentView)
        }
    }

    /**
     * Method to setup the graph's attributes.
     */
    private fun setupGraph() {
        graphView.layoutParams =
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT).apply {
                setMargins(0, offset.roundToInt(), 0, 0)
            }
        graphView.x = x
    }

    /**
     * Method to setup the handler's attributes.
     */
    private fun setupHandler() {
        val drawable = ContextCompat.getDrawable(context, handlerResource)
        drawable?.setTint(handlerColor)
        handlerView.id = R.id.curved_seekbar_handler
        handlerView.elevation = resources.getDimension(R.dimen.default_handler_elevation)
        handlerView.background = drawable
        handlerView.layoutParams = LayoutParams(handlerSize, handlerSize)

        val onDrag = OnDragTouchListener(
            handlerView,
            this,
            true,
            OnDragTouchListener.MovementType.HORIZONTAL
        )
            .setMarginStart(handlerMarginStart)
            .setMarginEnd(handlerMarginEnd)
            .setOnDragListener { effectiveProgress = it.progress }
            .setOnEndDraggingListener {
                updatePointSelected(true) {
                    onPointSelectedStopUpdating?.invoke(lastPointSelected)
                    onProgressStopChanging?.invoke(effectiveProgress)
                }
            }

        handlerView.setOnTouchListener(onDrag)
    }

    /**
     * Method to fix some layout flaws related to height, position or width that are
     * only acquired after measuring.
     */
    private fun adjustLayout() {
        doOnPreDraw {
            minimumHeight = (measuredWidth * absoluteHeight / absoluteWidth).roundToInt()
            if (height < minimumHeight) updateLayoutParams { height = minimumHeight }

            adjustHandlerPosition(getXForProgress(effectiveProgress), false)
            updateSelectedSegment()
        }
    }

    /**
     * Method that will move the handler from its current position to the desired one and animate it
     * if desired.
     *
     * @see animationsEnabled
     *
     * @param newX The new x coordinate which the handler will be moved to.
     * @param onEnd Callback that will be executed after the handler's position has changed.
     */
    private fun adjustHandlerPosition(newX: Float, animated: Boolean, onEnd: () -> Unit = {}) {
        val x = when {
            newX > maxAchievableX -> maxAchievableX
            newX < minAchievableX -> minAchievableX
            else -> newX
        }

        if (animated) {
            val handlerViewAnimator = handlerView.animate().x(x)
            handlerViewAnimator.duration = ANIMATION_DURATION
            handlerViewAnimator.setUpdateListener { effectiveProgress = handlerView.progress }
            handlerViewAnimator.withEndAction {
                onProgressStopChanging?.invoke(effectiveProgress)
                onPointSelectedStopUpdating?.invoke(lastPointSelected)
                onEnd()
            }

            handlerViewAnimator.start()
        } else {
            handlerView.x = x
            effectiveProgress = handlerView.progress
            onProgressStopChanging?.invoke(effectiveProgress)
            onPointSelectedStopUpdating?.invoke(lastPointSelected)
            onEnd()
        }
    }

    /**
     * Method that will update the handler's y coordinate based on its current x coordinate so that
     * it will stick to the curve.
     */
    private fun fixHandlerY() {
        handlerView.y = offset + getYForX(handlerCenterX)
    }

    /**
     * Method that will update the alpha of the segment which the handler is hovering over and reset
     * the others.
     */
    private fun updateSelectedSegment() {
        if (segments.isEmpty()) return
        var hasSegmentSelected = false

        segments.forEachIndexed { index, view ->
            val isSelected = handlerView.x >= view.x + handlerMarginStart
                    && handlerView.x <= view.x + view.measuredWidth + handlerMarginEnd
            view.isSelected = isSelected

            var newAlpha = 0.5f
            if (isSelected) {
                hasSegmentSelected = true
                lastSegmentSelected = index
                newAlpha = 1f
            }

            view.changeSegmentAlpha(newAlpha)
        }

        if (!hasSegmentSelected) segments[lastSegmentSelected].isSelected = true
    }

    /**
     * Method to change the segment alpha if need and animate the alpha transition if desired.
     *
     * @see animationsEnabled
     *
     * @param newAlpha The new alpha value to be set.
     */
    private fun View.changeSegmentAlpha(newAlpha: Float) {
        if (alpha != newAlpha) {
            if (animationsEnabled && !segmentsAnimating.contains(this)) {
                animate()
                    .alpha(newAlpha)
                    .setDuration(ANIMATION_DURATION)
                    .withEndAction {
                        segmentsAnimating.remove(this)
                        updateSelectedSegment()
                    }
                    .start()
                segmentsAnimating.add(this)
            } else if (!animationsEnabled) {
                alpha = newAlpha
            }
        }
    }

    /**
     * Method that will invalidate the current GraphView to make it draw the highlight again
     * with the new progress.
     */
    private fun updateHighlight() {
        graphView.invalidate()
    }

    /**
     * Method that will update the last selected point
     * based on the handler's current position on the curve and notify if there are any
     * registered listeners.
     *
     * @param adjustX Parameter to tell if the handler's position must be adjusted
     * (in case of locking to point positions)
     */
    private fun updatePointSelected(adjustX: Boolean, onEnd: () -> Unit = {}) {
        if (pointQuantity == 0) {
            onEnd()
            return
        }

        if (pointQuantity in 1 until segmentQuantity) {
            Log.w(
                "BezierSeekBar",
                "Point quantity ($pointQuantity) is less than segment quantity ($segmentQuantity)"
            )
            return
        }

        var point = ((handlerView.x - minAchievableX) / xPerPoint).roundToInt()
        if (adjustX) {
            val newX: Float = when (pointQuantity) {
                segmentQuantity -> {
                    val selectedSegment = segments.find(View::isSelected) ?: return
                    selectedSegment.x + (selectedSegment.measuredWidth / 2) + handlerMarginStart
                }
                else -> {
                    val floorPoint = handlerView.x - (handlerView.x % xPerPoint)
                    val ceilPoint = floorPoint + xPerPoint

                    if (handlerView.x - floorPoint < ceilPoint - handlerView.x) floorPoint
                    else ceilPoint
                }
            }

            adjustHandlerPosition(newX, animationsEnabled) {
                if (pointQuantity == segmentQuantity) {
                    point = segments.indexOfFirst(View::isSelected) + 1
                }

                lastPointSelected = point
                onEnd()
            }
        } else {
            lastPointSelected = point
            onEnd()
        }
    }

    /**
     * @param progress Progress based on the handler's position on the curve.
     * @return The x coordinate for the progress based on Bezier's cubic equation.
     */
    private fun getXForProgress(progress: Float): Float {
        var newX = (1 - progress).pow(3) * initialX
        newX += 3 * (1 - progress).pow(2) * progress * anchorX1
        newX += 3 * (1 - progress) * progress.pow(2) * anchorX2
        newX += progress.pow(3) * finalX

        return newX
    }

    /**
     * @param x The center x coordinate of the handler's position on the curve.
     * @return The y coordinate for the x coordinate based on Bezier's cubic equation.
     */
    private fun getYForX(x: Float): Float = getYForProgress(x / measuredWidth)

    /**
     * @param progress Progress based on the handler's position on the curve.
     * @return The y coordinate for the progress based on Bezier's cubic equation.
     */
    private fun getYForProgress(progress: Float): Float {
        var newY = (1 - progress).pow(3) * initialY
        newY += 3 * (1 - progress).pow(2) * progress * anchorY1
        newY += 3 * (1 - progress) * progress.pow(2) * anchorY2
        newY += progress.pow(3) * finalY

        return newY
    }

    /**
     * Adjusts handler and highlight position as well as the progress to fit the selected point.
     *
     * @param point Point to be selected (between 0 and given pointQuantity).
     */
    fun setSelectedPoint(point: Int) {
        val newX = (point * xPerPoint) + minAchievableX
        adjustHandlerPosition(newX, animationsEnabled)
    }

    /**
     * Set the preferred point to be selected when the corresponding segment is clicked.
     *
     * @param segmentToPoint A map which the keys are the segments indexes and the values are the
     * points to be set when the matching segment is clicked.
     */
    fun setPreferredPointOnClickBySegment(segmentToPoint: Map<Int, Int>) {
        this.preferredPointBySegment = segmentToPoint
    }

    /**
     * Set the event that will be triggered when ANY change to the progress is made.
     *
     * @param onProgressUpdated A function receiving the current progress and returning Unit.
     */
    fun setOnProgressUpdatedListener(onProgressUpdated: ProgressListener) {
        this.onProgressUpdated = onProgressUpdated
    }

    /**
     * Set the event that will be triggered when the LAST change to the progress is made.
     *
     * @param onProgressStopChanging A function receiving the current progress and returning Unit.
     */
    fun setOnProgressStopChangingListener(onProgressStopChanging: ProgressListener) {
        this.onProgressStopChanging = onProgressStopChanging
    }

    /**
     * Set the event that will be triggered when ANY point is selected.
     *
     * @param onPointSelectedUpdated A function receiving the point selected and returning Unit.
     */
    fun setOnPointSelectedUpdated(onPointSelectedUpdated: PointSelectionListener) {
        this.onPointSelectedUpdated = onPointSelectedUpdated
    }

    /**
     * Set the event that will be triggered when the LAST point is selected.
     *
     * @param onPointSelectedStopUpdating A function receiving the point selected and returning Unit.
     */
    fun setOnPointSelectedStopUpdating(onPointSelectedStopUpdating: PointSelectionListener) {
        this.onPointSelectedStopUpdating = {
            if (it in 1..pointQuantity) onPointSelectedStopUpdating(it - 1)
        }
    }

    /**
     * Set the event that will be triggered when a segment is clicked.
     *
     * @param onSegmentSelected A function receiving the selected segment index and returning Unit.
     */
    fun setOnSegmentSelectedListener(onSegmentSelected: SegmentSelectionListener) {
        this.onSegmentSelected = onSegmentSelected
    }

    /**
     * Layer that will be used to draw the line and highlight (if chosen).
     */
    private inner class GraphView(context: Context) : View(context) {

        init {
            clipChildren = false
            clipToPadding = false
            setBackgroundColor(Color.TRANSPARENT)
        }

        override fun onDraw(canvas: Canvas?) {
            super.onDraw(canvas)
            canvas?.drawBar()
        }

        private fun Canvas.drawBar() {
            val highlightStrokeSize = resources.getDimension(R.dimen.default_highlight_stroke_size)

            val linePaint = Paint()
            linePaint.color = barColor
            linePaint.style = Paint.Style.STROKE
            linePaint.strokeWidth = lineStrokeSize

            val highlightPaint = Paint()
            highlightPaint.color = barColor
            highlightPaint.style = Paint.Style.STROKE
            highlightPaint.strokeWidth = highlightStrokeSize

            val linePath = Path()
            linePath.rewind()
            linePath.moveTo(initialX, initialY)

            var x = 0f
            var progress: Float

            val handlerProgressOnLine = (handlerView.x + handlerView.measuredWidth / 2) / finalX

            val yTo = initialY + (lineStrokeSize / 2)

            while (x <= finalX) {
                progress = x / finalX

                val y = getYForX(x)
                linePath.lineTo(x, y)

                if (progress <= handlerProgressOnLine) {
                    val actualProgress = 1f - (y / yTo)

                    val initialHighlightColor =
                        ColorUtils.setAlphaComponent(
                            highlightColor,
                            (maxHighlightAlpha * 255 * actualProgress).roundToInt()
                        )
                    val finalHighlightColor =
                        ColorUtils.setAlphaComponent(
                            highlightColor,
                            (minHighlightAlpha * 255).roundToInt()
                        )

                    highlightPaint.shader = LinearGradient(
                        x,
                        y,
                        x,
                        yTo,
                        initialHighlightColor,
                        finalHighlightColor,
                        Shader.TileMode.CLAMP
                    )

                    val highlightPath = Path()
                    highlightPath.moveTo(x, y)
                    highlightPath.lineTo(x, yTo)
                    drawPath(highlightPath, highlightPaint)
                }

                x += 1f
            }

            linePath.lineTo(finalX, getYForX(finalX))
            drawPath(linePath, linePaint)
        }
    }
}