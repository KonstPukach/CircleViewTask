package com.example.circleviewtask.circleview

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.*
import android.graphics.Color.parseColor
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.scale
import com.example.circleviewtask.R
import kotlin.math.*


class CircleView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private val sectorsList: MutableList<Sector> = mutableListOf()

    private var _items = mutableListOf<Item>()
    var items: List<Item>
        get() = _items
        set(value) {
            _items = value.toMutableList()
        }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = BORDER_STROKE_WIDTH
        color = Color.WHITE
    }

    var radius: Float = 0F
        set(value) {
            field = value
            if (sectorsList.isNotEmpty())
                refreshSectors()
            invalidate()
        }

    private val defaultIconSizePx: Int
        get() {
            if (_items.size > 8) {
                // cosine theorem - to calculate the distance between borders of a single sector
                return ((radius / 2) * sqrt(2 * (1 - cos(2 * PI / _items.size)))).toInt()
            }
            return if (radius != 0F) {
                (radius / 3).toInt()
            } else {
                convertToPx(DEFAULT_ICON_SIZE_DP, context.resources).toInt()
            }
        }

    private val defaultIcon: Bitmap

    private val sectPaintWithoutShadow: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    var scaleOnClick: Float // how many times to increase the selected sector
        private set

    var circleColor: Int = DEFAULT_SECTOR_COLOR
        set(value) {
            field = value
            if (sectorsList.isNotEmpty())
                sectorsList.map {
                    it.paint.color =
                        if (it.checked) changeColor(it.scale)
                        else value
                }
            invalidate()
        }

    var shadowRadius: Float = 0F
        set(value) {
            field = value
            if (sectorsList.isNotEmpty())
                sectorsList.mapIndexed { i, it ->
                    val center = getSectorCenter(i)
                    it.paint.setShadowLayer(
                        value,
                        (center.first - pivotX) / SHADOW_DIRECTION_DIV,
                        (center.second - pivotY) / SHADOW_DIRECTION_DIV,
                        SHADOW_COLOR
                    )
                }
            invalidate()
        }


    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.CircleView,
            0,
            0
        ).apply {
            // Receive attributes
            try {
                scaleOnClick = getFloat(
                    R.styleable.CircleView_scale_on_click,
                    MIN_SCALE_ON_CLICK
                )
                if (scaleOnClick < MIN_SCALE_ON_CLICK)
                    scaleOnClick = MIN_SCALE_ON_CLICK
                if (scaleOnClick > MAX_SCALE_ON_CLICK)
                    scaleOnClick = MAX_SCALE_ON_CLICK

                circleColor = getColor(
                    R.styleable.CircleView_circle_color,
                    DEFAULT_SECTOR_COLOR
                )
                shadowRadius = getDimension(
                    R.styleable.CircleView_circle_shadow_radius,
                    DEFAULT_SHADOW_RADIUS
                )
                radius = getDimension(
                    R.styleable.CircleView_circle_radius,
                    0F
                )
            } finally {
                recycle()
            }
        }

        val drawable = AppCompatResources.getDrawable(context, R.drawable.icon_sun)
        defaultIcon = (drawable as BitmapDrawable).bitmap.scale(defaultIconSizePx, defaultIconSizePx)
    }

    fun addItem(item: Item) {
        if (sectorsList.isNotEmpty()) {
            _items.add(item)
            sectorsList.add(createSector(getSectorCenter(_items.size - 1), item))
            refreshSectors()
            invalidate()
        }
    }

    private val myListener =  object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            return true
        }
    }
    private val detector: GestureDetector = GestureDetector(context, myListener)

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return detector.onTouchEvent(event).let { result ->
            if (!result) {
                when (event.action) {
                    MotionEvent.ACTION_UP -> {
                        val index = getSectorClicked(event.x, event.y)
                        if (index >= 0)
                            animateSectorOnClick(index)
                        performClick()
                    }
                    else -> false
                }
            } else true
        }
    }

    private fun animateSectorOnClick(sectorIndex: Int) {
        val shadowDirection = getSectorCenter(sectorIndex)
        val shadowDirectionX = (shadowDirection.first - pivotX) / SHADOW_DIRECTION_DIV
        val shadowDirectionY = (shadowDirection.second - pivotY) / SHADOW_DIRECTION_DIV

        sectorsList[sectorIndex].animatorOnClick?.cancel()  // cancel previous animation

        val destScale: Float =    // destination animation scale
            if (sectorsList[sectorIndex].checked) 1F
            else scaleOnClick

        val anim = ObjectAnimator.ofFloat(
            this,
            null,
            sectorsList[sectorIndex].scale, // current scale state
            destScale
        ).apply {
            addUpdateListener {
                val animValue = it.animatedValue as Float

                // animate shadow
                sectorsList[sectorIndex].paint.setShadowLayer(
                    animValue * shadowRadius,
                    shadowDirectionX,
                    shadowDirectionY,
                    SHADOW_COLOR
                )

                val bmp = sectorsList[sectorIndex].icon.bitmap
                sectorsList[sectorIndex] = sectorsList[sectorIndex].copy(
                    scale = animValue,  // animate a sector figure
                    paint = sectorsList[sectorIndex].paint.apply {
                        color = changeColor(animValue) // animate a color
                    },
                    scaledIcon = sectorsList[sectorIndex].icon.copy( // animate icon
                        bitmap = bmp.scale(
                            (bmp.width * animValue).toInt(),
                            (bmp.height * animValue).toInt()
                        ),
                        x = sectorsList[sectorIndex].icon.x - bmp.width / 2 * (animValue - 1),
                        y = sectorsList[sectorIndex].icon.y - bmp.height / 2 * (animValue - 1),
                    )
                )
                postInvalidate()
            }

            duration = ANIM_ON_CLICK_DURATION
        }

        sectorsList[sectorIndex] = sectorsList[sectorIndex].copy(
            animatorOnClick = anim,
            checked = !sectorsList[sectorIndex].checked
        )

        _items[sectorIndex] = _items[sectorIndex].copy(
            checked = !_items[sectorIndex].checked
        )

        anim.start()
    }

    private fun changeColor(scale: Float): Int {
        val color = circleColor
        val a = Color.alpha(color)
        val r = (Color.red(color) / scale).toInt()
        val g = (Color.green(color) / scale).toInt()
        val b = (Color.blue(color) / scale).toInt()
        return Color.argb(
            a,
            min(r, 255),
            min(g, 255),
            min(b, 255)
        )
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.let {
            var currAngle = 0F
            sectorsList.forEach { sect ->
                // draw a sector with a shadow
                canvas.drawArc(
                    sect.getScaledCircle(radius),
                    currAngle,
                    FULL_CIR_DEGREES / _items.size,
                    true,
                    sect.paint
                )
                currAngle += FULL_CIR_DEGREES / _items.size
            }
            currAngle = 0F
            sectorsList.forEach { sect ->
                // draw a sector without a shadow (to cover (hide) a shadow from the previous sector)
                canvas.drawArc(
                    sect.getScaledCircle(radius),
                    currAngle,
                    FULL_CIR_DEGREES / _items.size,
                    true,
                    sectPaintWithoutShadow.apply { color = sect.paint.color }
                )

                // draw a border around a sector
                canvas.drawArc(
                    sect.getScaledCircle(radius),
                    currAngle,
                    FULL_CIR_DEGREES / _items.size,
                    true,
                    borderPaint
                )

                // draw an icon on a sector
                canvas.drawBitmap(
                    sect.scaledIcon.bitmap,
                    sect.scaledIcon.x,
                    sect.scaledIcon.y,
                    borderPaint
                )

                currAngle += FULL_CIR_DEGREES / _items.size
            }
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        initSectorsList()
        super.onLayout(changed, left, top, right, bottom)
    }

    private fun initSectorsList() {
        sectorsList.clear()
        _items.forEachIndexed { i, it ->
            val center = getSectorCenter(i)
            sectorsList.add(createSector(center, it))
        }
    }

    private fun createSector(center: Pair<Float, Float>, item: Item): Sector {
        val color =
            if (item.checked) changeColor(scaleOnClick)
            else circleColor

        return Sector(
            circle = RectF(
                pivotX - radius,
                pivotY - radius,
                pivotX + radius,
                pivotY + radius
            ),
            paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                style = Paint.Style.FILL
                setShadowLayer(
                    shadowRadius,
                    (center.first - pivotX) / SHADOW_DIRECTION_DIV,
                    (center.second - pivotY) / SHADOW_DIRECTION_DIV,
                    SHADOW_COLOR
                )
            },
            icon = createIcon(center, item, false),
            scaledIcon = createIcon(center, item, item.checked),
            checked = item.checked,
            scale =
                if (item.checked) scaleOnClick
                else 1F
        )
    }

    // Call it if some size parameter is changed (e.g. items amount, radius)
    private fun refreshSectors() {
        _items.forEachIndexed { i, item ->
            val center = getSectorCenter(i)
            sectorsList[i] = sectorsList[i].copy(
                circle = RectF(
                    pivotX - radius,
                    pivotY - radius,
                    pivotX + radius,
                    pivotY + radius
                ),
                paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = sectorsList[i].paint.color
                    style = Paint.Style.FILL
                    setShadowLayer(
                        shadowRadius,
                        (center.first - pivotX) / SHADOW_DIRECTION_DIV,
                        (center.second - pivotY) / SHADOW_DIRECTION_DIV,
                        SHADOW_COLOR
                    )
                },
                icon = createIcon(center, item, false),
                scaledIcon = createIcon(center, item, item.checked)
            )
        }
    }

    private fun createIcon(center: Pair<Float, Float>, item: Item, scaled: Boolean): Icon {
        val iconSize: Int =
            if (scaled) (defaultIconSizePx * scaleOnClick).toInt()
            else defaultIconSizePx
        val bitmap = getBitmap(context, item.icon, iconSize) ?: defaultIcon
        return Icon(
            x = center.first - bitmap.width / 2,
            y = center.second - bitmap.height / 2,
            bitmap = bitmap
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        // calculates width considering that it will be scaled by the click and + shadow radius
        val minWidth = paddingLeft + paddingRight + (radius + shadowRadius) * 2 * scaleOnClick
        if (minWidth > min(w, h)) {
            radius = (min(w, h) - paddingRight - paddingLeft) / 2F / scaleOnClick - shadowRadius
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (radius == 0F)
            radius = convertToPx(DEFAULT_RADIUS_DP, context.resources)

        // min layout width to place a circle with scales, shadows and paddings
        val minWidth = paddingLeft + paddingRight + (radius + shadowRadius) * 2 * scaleOnClick

        val w = resolveSize(minWidth.toInt(), widthMeasureSpec)
        val h = resolveSize(minWidth.toInt(), heightMeasureSpec)

        setMeasuredDimension(w, h)
    }

    // Returns an index of chosen sector
    private fun getSectorClicked(x: Float, y: Float): Int {
        // check the click on the view or past the view
        val distanceToCenter = (pivotX - x).pow(2)  + (pivotY - y).pow(2)
        val isInView: Boolean = distanceToCenter <= radius * radius
        val isInScaledView: Boolean = distanceToCenter <= radius.pow(2) * scaleOnClick.pow(2)

        if (isInScaledView) {
            val tanAlpha: Float = (pivotY - y) / (pivotX - x)
            var degrees = atan(tanAlpha.toDouble()) * HALF_CIR_DEGREES / PI // convert to degrees

            // calculate what the quoter was clicked (for the trigonometrical circle)
            if (x - pivotX < 0 && y - pivotY > 0) {     // 2nd quoter
                degrees += HALF_CIR_DEGREES
            }
            if (x - pivotX < 0 &&  y - pivotY < 0) {    // 3d quoter
                degrees += HALF_CIR_DEGREES
            }
            if (x - pivotX > 0 && y - pivotY < 0) {     // 4th quoter
                degrees += FULL_CIR_DEGREES
            }

            var currDegrees = 0F
            var sectorIndex = -1
            for(sect in sectorsList) {
                sectorIndex++
                currDegrees += FULL_CIR_DEGREES / _items.size
                if (degrees <= currDegrees) {
                    break
                }
            }
            if (isInView || sectorsList[sectorIndex].checked)
                return sectorIndex
        }

        return -1   // nothing
    }

    // Returns (x, y) coordinates of the center of a n-th sector
    private fun getSectorCenter(sectorNum: Int): Pair<Float, Float> {
        val degrees = FULL_CIR_DEGREES / _items.size * sectorNum +
                FULL_CIR_DEGREES / _items.size / 2

        val sinA = sin(degrees * PI / HALF_CIR_DEGREES)
        val cosA = cos(degrees * PI / HALF_CIR_DEGREES)
        val resX = pivotX + (radius * ICON_RELATIVE_START_POS) * cosA
        val resY = pivotY + (radius * ICON_RELATIVE_START_POS) * sinA
        return Pair(resX.toFloat(), resY.toFloat())
    }

    private data class Sector(
        val circle: RectF,
        val paint: Paint,
        val icon: Icon,
        val scaledIcon: Icon = icon,
        val animatorOnClick: ObjectAnimator? = null,
        val checked: Boolean = false,
        val scale: Float = 1F
    ) {
        fun getScaledCircle(radius: Float): RectF {
            return RectF(
                circle.left - radius * (scale - 1),
                circle.top - radius * (scale - 1),
                circle.right + radius * (scale - 1),
                circle.bottom + radius * (scale - 1),
            )
        }
    }

    private data class Icon(
        val x: Float,
        val y: Float,
        val bitmap: Bitmap
    )

    data class Item(
        @DrawableRes val icon: Int,
        val checked: Boolean = false
    )

    companion object {
        private const val FULL_CIR_DEGREES = 360F
        private const val HALF_CIR_DEGREES = 180F
        private const val ICON_RELATIVE_START_POS = 0.7F
        private const val ANIM_ON_CLICK_DURATION = 150L
        private const val DEFAULT_ICON_SIZE_DP = 34F
        private const val DEFAULT_SHADOW_RADIUS = 40F
        private const val SHADOW_COLOR = Color.GRAY
        private const val SHADOW_DIRECTION_DIV = 30
        private const val MIN_SCALE_ON_CLICK = 1.1F
        private const val MAX_SCALE_ON_CLICK = 1.5F
        private val DEFAULT_SECTOR_COLOR = parseColor("#03DAC6")
        private const val DEFAULT_RADIUS_DP = 100F
        private const val BORDER_STROKE_WIDTH = 8F
    }
}