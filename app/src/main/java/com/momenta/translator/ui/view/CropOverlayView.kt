package com.momenta.translator.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

/**
 * 图片裁剪选择框视图
 * 用户可以拖动四个角和四条边来调整选择区域
 */
class CropOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // 选择区域
    private val cropRect = RectF()

    // 绘制工具
    private val borderPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val overlayPaint = Paint().apply {
        color = Color.parseColor("#88000000")
        style = Paint.Style.FILL
    }

    private val gridPaint = Paint().apply {
        color = Color.parseColor("#88FFFFFF")
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }

    private val cornerPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    // 交互状态
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var activeHandle = Handle.NONE

    private val touchSlop = 50f // 触摸容差（像素）
    private val minSize = 100f // 最小选择框尺寸

    enum class Handle {
        NONE,
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT,
        LEFT, RIGHT, TOP, BOTTOM,
        CENTER
    }

    init {
        // 初始化选择框为中心区域
        post {
            val padding = 50f
            cropRect.set(
                padding,
                padding,
                width - padding,
                height - padding
            )
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 1. 绘制半透明遮罩（选择区域外）
        canvas.drawRect(0f, 0f, width.toFloat(), cropRect.top, overlayPaint)
        canvas.drawRect(0f, cropRect.bottom, width.toFloat(), height.toFloat(), overlayPaint)
        canvas.drawRect(0f, cropRect.top, cropRect.left, cropRect.bottom, overlayPaint)
        canvas.drawRect(cropRect.right, cropRect.top, width.toFloat(), cropRect.bottom, overlayPaint)

        // 2. 绘制九宫格线
        val gridW = cropRect.width() / 3
        val gridH = cropRect.height() / 3
        for (i in 1..2) {
            // 竖线
            canvas.drawLine(
                cropRect.left + gridW * i,
                cropRect.top,
                cropRect.left + gridW * i,
                cropRect.bottom,
                gridPaint
            )
            // 横线
            canvas.drawLine(
                cropRect.left,
                cropRect.top + gridH * i,
                cropRect.right,
                cropRect.top + gridH * i,
                gridPaint
            )
        }

        // 3. 绘制边框
        canvas.drawRect(cropRect, borderPaint)

        // 4. 绘制四个角的控制点
        val cornerSize = 20f
        drawCorner(canvas, cropRect.left, cropRect.top, cornerSize)
        drawCorner(canvas, cropRect.right, cropRect.top, cornerSize)
        drawCorner(canvas, cropRect.left, cropRect.bottom, cornerSize)
        drawCorner(canvas, cropRect.right, cropRect.bottom, cornerSize)
    }

    private fun drawCorner(canvas: Canvas, x: Float, y: Float, size: Float) {
        canvas.drawCircle(x, y, size, cornerPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                activeHandle = getHandleAt(event.x, event.y)
                return activeHandle != Handle.NONE
            }

            MotionEvent.ACTION_MOVE -> {
                if (activeHandle != Handle.NONE) {
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY
                    adjustCropRect(activeHandle, dx, dy)
                    lastTouchX = event.x
                    lastTouchY = event.y
                    invalidate()
                    return true
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                activeHandle = Handle.NONE
            }
        }
        return super.onTouchEvent(event)
    }

    private fun getHandleAt(x: Float, y: Float): Handle {
        val corners = touchSlop

        return when {
            // 四个角
            isNear(x, cropRect.left, corners) && isNear(y, cropRect.top, corners) -> Handle.TOP_LEFT
            isNear(x, cropRect.right, corners) && isNear(y, cropRect.top, corners) -> Handle.TOP_RIGHT
            isNear(x, cropRect.left, corners) && isNear(y, cropRect.bottom, corners) -> Handle.BOTTOM_LEFT
            isNear(x, cropRect.right, corners) && isNear(y, cropRect.bottom, corners) -> Handle.BOTTOM_RIGHT

            // 四条边
            isNear(x, cropRect.left, corners) && y > cropRect.top && y < cropRect.bottom -> Handle.LEFT
            isNear(x, cropRect.right, corners) && y > cropRect.top && y < cropRect.bottom -> Handle.RIGHT
            isNear(y, cropRect.top, corners) && x > cropRect.left && x < cropRect.right -> Handle.TOP
            isNear(y, cropRect.bottom, corners) && x > cropRect.left && x < cropRect.right -> Handle.BOTTOM

            // 中心区域（整体移动）
            cropRect.contains(x, y) -> Handle.CENTER

            else -> Handle.NONE
        }
    }

    private fun isNear(a: Float, b: Float, tolerance: Float): Boolean {
        return abs(a - b) < tolerance
    }

    private fun adjustCropRect(handle: Handle, dx: Float, dy: Float) {
        when (handle) {
            Handle.TOP_LEFT -> {
                cropRect.left = (cropRect.left + dx).coerceIn(0f, cropRect.right - minSize)
                cropRect.top = (cropRect.top + dy).coerceIn(0f, cropRect.bottom - minSize)
            }
            Handle.TOP_RIGHT -> {
                cropRect.right = (cropRect.right + dx).coerceIn(cropRect.left + minSize, width.toFloat())
                cropRect.top = (cropRect.top + dy).coerceIn(0f, cropRect.bottom - minSize)
            }
            Handle.BOTTOM_LEFT -> {
                cropRect.left = (cropRect.left + dx).coerceIn(0f, cropRect.right - minSize)
                cropRect.bottom = (cropRect.bottom + dy).coerceIn(cropRect.top + minSize, height.toFloat())
            }
            Handle.BOTTOM_RIGHT -> {
                cropRect.right = (cropRect.right + dx).coerceIn(cropRect.left + minSize, width.toFloat())
                cropRect.bottom = (cropRect.bottom + dy).coerceIn(cropRect.top + minSize, height.toFloat())
            }
            Handle.LEFT -> {
                cropRect.left = (cropRect.left + dx).coerceIn(0f, cropRect.right - minSize)
            }
            Handle.RIGHT -> {
                cropRect.right = (cropRect.right + dx).coerceIn(cropRect.left + minSize, width.toFloat())
            }
            Handle.TOP -> {
                cropRect.top = (cropRect.top + dy).coerceIn(0f, cropRect.bottom - minSize)
            }
            Handle.BOTTOM -> {
                cropRect.bottom = (cropRect.bottom + dy).coerceIn(cropRect.top + minSize, height.toFloat())
            }
            Handle.CENTER -> {
                val newLeft = (cropRect.left + dx).coerceIn(0f, width - cropRect.width())
                val newTop = (cropRect.top + dy).coerceIn(0f, height - cropRect.height())
                val w = cropRect.width()
                val h = cropRect.height()
                cropRect.set(newLeft, newTop, newLeft + w, newTop + h)
            }
            Handle.NONE -> {}
        }
    }

    /**
     * 获取当前选择的矩形区域（相对于View的坐标）
     */
    fun getCropRect(): RectF = RectF(cropRect)

    /**
     * 获取选择区域相对于ImageView显示图片的比例位置
     */
    fun getCropRectRatio(): RectF {
        return RectF(
            cropRect.left / width,
            cropRect.top / height,
            cropRect.right / width,
            cropRect.bottom / height
        )
    }
}
