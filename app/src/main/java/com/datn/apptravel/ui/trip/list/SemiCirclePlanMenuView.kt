package com.datn.apptravel.ui.trip.list
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.GestureDetectorCompat
import com.datn.apptravel.R
import com.datn.apptravel.data.model.PlanType
import kotlin.math.*

class SemiCirclePlanMenuView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var centerX = 0f
    private var centerY = 0f
    private var radius = 0f

    // All 14 plan types (now includes NONE)
    private val allPlanTypes = PlanType.values().toList()

    // Rotation angle in degrees (0-360, continuous circular rotation)
    private var currentRotationAngle = 0f
    private var targetRotationAngle = 0f
    private var startRotationAngle = 0f

    // Animation
    private var isAnimating = false
    private val animationDuration = 300f // milliseconds
    private var animationStartTime = 0L

    private var onPlanSelectedListener: ((PlanType) -> Unit)? = null

    // Gesture detector for swipe/fling
    private val gestureDetector: GestureDetectorCompat

    init {
        paint.style = Paint.Style.FILL
        setLayerType(LAYER_TYPE_SOFTWARE, null) // Enable shadow rendering

        textPaint.style = Paint.Style.FILL
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 12f
        textPaint.typeface = Typeface.DEFAULT_BOLD

        iconPaint.style = Paint.Style.FILL
        iconPaint.textAlign = Paint.Align.CENTER
        iconPaint.textSize = 18f

        arrowPaint.style = Paint.Style.FILL
        arrowPaint.color = Color.WHITE

        // Initialize gesture detector for swipe (now vertical since menu is on left side)
        gestureDetector = GestureDetectorCompat(context, object : GestureDetector.SimpleOnGestureListener() {

            private var totalScrollY = 0f

            override fun onDown(e: MotionEvent): Boolean {
                // Reset scroll counter
                totalScrollY = 0f
                // Return true to indicate we want to handle this gesture
                return true
            }

            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                // Accumulate scroll
                totalScrollY += distanceY

                // For vertical menu, use Y scroll
                // Swipe up = rotate clockwise (next items)
                // Swipe down = rotate counter-clockwise (previous items)
                val anglePerItem = 360f / allPlanTypes.size
                val threshold = 20f // pixels per rotation step

                if (abs(totalScrollY) > threshold) {
                    if (totalScrollY > 0) {
                        rotateNext()
                    } else {
                        rotatePrevious()
                    }
                    totalScrollY = 0f
                    return true
                }
                return true
            }

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 != null && abs(velocityY) > 500) {
                    val diffY = e2.y - e1.y
                    // Fast fling - multiple rotations
                    val numSteps = min(abs(velocityY / 2000f).toInt(), 3)

                    repeat(numSteps.coerceAtLeast(1)) {
                        if (diffY > 0) {
                            rotatePrevious()
                        } else {
                            rotateNext()
                        }
                    }
                    return true
                }
                return false
            }
        })

        // Force initial draw
        post {
            invalidate()
        }
    }

    private fun rotatePrevious() {
        if (isAnimating) return

        val anglePerItem = 360f / allPlanTypes.size
        targetRotationAngle = currentRotationAngle + anglePerItem

        startAnimation()
    }

    private fun rotateNext() {
        if (isAnimating) return

        val anglePerItem = 360f / allPlanTypes.size
        targetRotationAngle = currentRotationAngle - anglePerItem

        startAnimation()
    }

    private fun startAnimation() {
        isAnimating = true
        startRotationAngle = currentRotationAngle
        animationStartTime = System.currentTimeMillis()
        Log.d("SemiCircleMenu", "Animation started: from $startRotationAngle to $targetRotationAngle")
        invalidate()
    }

    private fun updateAnimation() {
        val elapsed = System.currentTimeMillis() - animationStartTime
        val progress = min(elapsed / animationDuration, 1f)

        if (progress >= 1f) {
            currentRotationAngle = targetRotationAngle
            // Normalize angle to 0-360
            currentRotationAngle = ((currentRotationAngle % 360f) + 360f) % 360f
            targetRotationAngle = currentRotationAngle
            isAnimating = false

            Log.d("SemiCircleMenu", "Animation ended: $currentRotationAngle")

            // Notify selection changed
            onPlanSelectedListener?.invoke(getSelectedPlanType())
        } else {
            // Smooth easing function (ease-out)
            val easedProgress = 1f - (1f - progress) * (1f - progress)

            // Interpolate rotation angle (lerp between start and target)
            currentRotationAngle = startRotationAngle + (targetRotationAngle - startRotationAngle) * easedProgress

            invalidate()
        }
    }

    fun getSelectedPlanType(): PlanType {
        val anglePerItem = 360f / allPlanTypes.size

        // Find which item is closest to center (0 degrees = right/center of semi-circle)
        allPlanTypes.forEachIndexed { itemIndex, planType ->
            val baseItemAngle = itemIndex * anglePerItem
            val rotatedAngle = baseItemAngle + currentRotationAngle
            val normalizedAngle = ((rotatedAngle % 360f) + 360f) % 360f

            // Check if this item is in the visible range and at center
            if (normalizedAngle >= 270f || normalizedAngle <= 90f) {
                val semiAngle = if (normalizedAngle > 180f) normalizedAngle - 360f else normalizedAngle

                // Item at center has semiAngle close to 0
                if (abs(semiAngle) < anglePerItem / 2) {
                    Log.d("SemiCircleMenu", "Selected: ${planType.displayName} (index=$itemIndex, angle=$semiAngle)")
                    return planType
                }
            }
        }

        // Fallback to first item if nothing found
        return allPlanTypes[0]
    }

    fun setOnPlanSelectedListener(listener: (PlanType) -> Unit) {
        onPlanSelectedListener = listener
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Position for left side semi-circle (from -90° to 90°)
        centerX = 0f
        centerY = h / 2f
        radius = min(w * 2, h).toFloat() * 0.45f

        // Force redraw when size changes
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Update animation if running
        if (isAnimating) {
            updateAnimation()
        }

        // Draw semi-circle background (left side, from -90° to 90°)
        paint.color = ContextCompat.getColor(context, R.color.white)
        paint.setShadowLayer(20f, 5f, 0f, Color.parseColor("#20000000"))

        val rect = RectF(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )
        // Draw from -90° to 90° (left semi-circle)
        canvas.drawArc(rect, -90f, 180f, true, paint)

        // Calculate angle per item for full circle
        val anglePerItem = 360f / allPlanTypes.size

        // Draw all items with rotation offset
        allPlanTypes.forEachIndexed { itemIndex, planType ->
            // Calculate base angle for this item (distributed around full circle)
            val baseItemAngle = itemIndex * anglePerItem

            // Apply current rotation offset
            val rotatedAngle = baseItemAngle + currentRotationAngle

            // Only draw items that are visible in the semi-circle (-90° to 90°)
            val normalizedAngle = ((rotatedAngle % 360f) + 360f) % 360f
            if (normalizedAngle >= 270f || normalizedAngle <= 90f) {
                // Map to semi-circle coordinates (-90 to 90)
                val semiAngle = if (normalizedAngle > 180f) normalizedAngle - 360f else normalizedAngle

                // Check if this is the selected item (closest to 0 degrees = center)
                val isSelected = abs(semiAngle) < anglePerItem / 2

                // Calculate position for icon and text based on semiAngle
                val iconRadius = radius * 0.55f
                val textRadius = radius * 0.75f

                val iconX = centerX + iconRadius * cos(Math.toRadians(semiAngle.toDouble())).toFloat()
                val iconY = centerY + iconRadius * sin(Math.toRadians(semiAngle.toDouble())).toFloat()

                val textX = centerX + textRadius * cos(Math.toRadians(semiAngle.toDouble())).toFloat()
                val textY = centerY + textRadius * sin(Math.toRadians(semiAngle.toDouble())).toFloat()

                // Draw icon drawable
                try {
                    val drawable = ContextCompat.getDrawable(context, planType.iconRes)
                    drawable?.let {
                        val iconSize = if (isSelected) 56 else 44
                        it.setBounds(
                            (iconX - iconSize / 2).toInt(),
                            (iconY - iconSize / 2).toInt(),
                            (iconX + iconSize / 2).toInt(),
                            (iconY + iconSize / 2).toInt()
                        )

                        // Tint icon color
                        it.setTint(
                            if (isSelected) ContextCompat.getColor(context, R.color.blue_primary)
                            else Color.parseColor("#888888")
                        )
                        it.draw(canvas)
                    }
                } catch (e: Exception) {
                    // Fallback to circle with first letter if icon not found
                    val iconSize = if (isSelected) 44f else 36f
                    paint.color = if (isSelected) ContextCompat.getColor(context, R.color.blue_primary)
                    else Color.parseColor("#888888")
                    canvas.drawCircle(iconX, iconY, iconSize / 2, paint)

                    iconPaint.color = Color.WHITE
                    iconPaint.textSize = if (isSelected) 18f else 14f
                    canvas.drawText(
                        planType.displayName.first().toString(),
                        iconX,
                        iconY + 6f,
                        iconPaint
                    )
                }

                // Draw plan name with background for better visibility
                val displayName = if (planType.displayName.length > 8) {
                    planType.displayName.substring(0, 7) + "."
                } else {
                    planType.displayName
                }

                // Measure text for background
                textPaint.textSize = if (isSelected) 14f else 11f
                textPaint.typeface = Typeface.DEFAULT_BOLD
                val textWidth = textPaint.measureText(displayName)
                val textHeight = textPaint.textSize

                // Draw text background only if selected
                if (isSelected) {
                    val bgPaint = Paint()
                    bgPaint.color = ContextCompat.getColor(context, R.color.blue_primary)
                    bgPaint.style = Paint.Style.FILL

                    val padding = 8f
                    canvas.drawRoundRect(
                        textX - textWidth / 2 - padding,
                        textY - textHeight + 2f,
                        textX + textWidth / 2 + padding,
                        textY + 8f,
                        8f, 8f,
                        bgPaint
                    )

                    // Draw text in white
                    textPaint.color = Color.WHITE
                } else {
                    // Draw text in dark gray
                    textPaint.color = Color.parseColor("#555555")
                }

                // Draw text
                canvas.drawText(displayName, textX, textY + 5f, textPaint)

                // Draw arrow for selected item
                if (isSelected) {
                    drawArrow(canvas, iconX, iconY - 45f)
                }
            }
        }
    }

    private fun drawArrow(canvas: Canvas, x: Float, y: Float) {
        val arrowPath = Path()
        arrowPath.moveTo(x, y)
        arrowPath.lineTo(x - 10f, y + 15f)
        arrowPath.lineTo(x + 10f, y + 15f)
        arrowPath.close()

        arrowPaint.color = ContextCompat.getColor(context, R.color.blue_primary)
        canvas.drawPath(arrowPath, arrowPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Always handle gesture detector first
        val gestureHandled = gestureDetector.onTouchEvent(event)

        // Handle single tap to select item
        if (event.action == MotionEvent.ACTION_UP && !isAnimating) {
            val touchX = event.x
            val touchY = event.y

            // Calculate touch angle
            val dx = touchX - centerX
            val dy = touchY - centerY
            val distance = sqrt(dx * dx + dy * dy)

            // Check if touch is within the menu radius
            if (distance <= radius && distance >= radius * 0.3f) {
                val touchAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()

                // Check if touch is in semi-circle range (-90 to 90)
                if (touchAngle >= -90f && touchAngle <= 90f) {
                    // Find which item was clicked based on position
                    val anglePerItem = 360f / allPlanTypes.size

                    // Find the closest item to the touch position
                    var closestItemIndex = -1
                    var smallestAngleDiff = Float.MAX_VALUE

                    allPlanTypes.forEachIndexed { itemIndex, _ ->
                        val baseItemAngle = itemIndex * anglePerItem
                        val rotatedAngle = baseItemAngle + currentRotationAngle
                        val normalizedAngle = ((rotatedAngle % 360f) + 360f) % 360f

                        if (normalizedAngle >= 270f || normalizedAngle <= 90f) {
                            val semiAngle = if (normalizedAngle > 180f) normalizedAngle - 360f else normalizedAngle
                            val angleDiff = abs(semiAngle - touchAngle)

                            if (angleDiff < smallestAngleDiff && angleDiff < anglePerItem / 2) {
                                smallestAngleDiff = angleDiff
                                closestItemIndex = itemIndex
                            }
                        }
                    }

                    // If clicked on an item that's not at center, rotate to it
                    if (closestItemIndex != -1) {
                        val targetItemAngle = closestItemIndex * anglePerItem
                        val rotatedTargetAngle = targetItemAngle + currentRotationAngle
                        val normalizedTargetAngle = ((rotatedTargetAngle % 360f) + 360f) % 360f
                        val semiTargetAngle = if (normalizedTargetAngle > 180f) normalizedTargetAngle - 360f else normalizedTargetAngle

                        // If not at center (0 degrees), rotate to center
                        if (abs(semiTargetAngle) > anglePerItem / 4) {
                            targetRotationAngle = currentRotationAngle - semiTargetAngle
                            startAnimation()
                            return true
                        }
                    }
                }
            }
        }

        // Return true if gesture was handled OR if touch is within our bounds
        return gestureHandled || event.action == MotionEvent.ACTION_DOWN || super.onTouchEvent(event)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = 300
        val desiredHeight = 150

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val width = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> min(desiredWidth, widthSize)
            else -> desiredWidth
        }

        val height = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> min(desiredHeight, heightSize)
            else -> desiredHeight
        }

        setMeasuredDimension(width, height)
    }
}