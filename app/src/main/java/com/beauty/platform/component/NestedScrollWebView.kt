package com.beauty.platform.component

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.webkit.WebView
import androidx.core.view.NestedScrollingChild2
import androidx.core.view.NestedScrollingChildHelper
import androidx.core.view.ViewCompat
import kotlin.math.abs

/**
 * NestedScrolling을 지원하는 WebView
 * SwipeRefresh와 함께 사용할 때 pull-to-refresh가 정상 동작하도록 함
 */
class NestedScrollWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr), NestedScrollingChild2 {

    private val nestedScrollingChildHelper = NestedScrollingChildHelper(this)
    private var lastMotionY = 0f
    private var nestedOffsetY = 0
    private val scrollOffset = IntArray(2)
    private val scrollConsumed = IntArray(2)
    private var velocityTracker: VelocityTracker? = null
    private var lastTouchTime = 0L
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val minimumVelocity = ViewConfiguration.get(context).scaledMinimumFlingVelocity
    private val maximumVelocity = ViewConfiguration.get(context).scaledMaximumFlingVelocity

    init {
        // NestedScrolling 활성화
        isNestedScrollingEnabled = true
        // WebView 늘어지는 효과 제거
        overScrollMode = View.OVER_SCROLL_NEVER
    }

    override fun setNestedScrollingEnabled(enabled: Boolean) {
        nestedScrollingChildHelper.isNestedScrollingEnabled = enabled
    }

    override fun isNestedScrollingEnabled(): Boolean {
        return nestedScrollingChildHelper.isNestedScrollingEnabled
    }

    override fun startNestedScroll(axes: Int): Boolean {
        return nestedScrollingChildHelper.startNestedScroll(axes)
    }

    override fun startNestedScroll(axes: Int, type: Int): Boolean {
        return nestedScrollingChildHelper.startNestedScroll(axes, type)
    }

    override fun stopNestedScroll() {
        nestedScrollingChildHelper.stopNestedScroll()
    }

    override fun stopNestedScroll(type: Int) {
        nestedScrollingChildHelper.stopNestedScroll(type)
    }

    override fun hasNestedScrollingParent(): Boolean {
        return nestedScrollingChildHelper.hasNestedScrollingParent()
    }

    override fun hasNestedScrollingParent(type: Int): Boolean {
        return nestedScrollingChildHelper.hasNestedScrollingParent(type)
    }

    override fun dispatchNestedScroll(
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        offsetInWindow: IntArray?
    ): Boolean {
        return nestedScrollingChildHelper.dispatchNestedScroll(
            dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow
        )
    }

    override fun dispatchNestedScroll(
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        offsetInWindow: IntArray?,
        type: Int
    ): Boolean {
        return nestedScrollingChildHelper.dispatchNestedScroll(
            dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow, type
        )
    }


    override fun dispatchNestedPreScroll(
        dx: Int,
        dy: Int,
        consumed: IntArray?,
        offsetInWindow: IntArray?
    ): Boolean {
        return nestedScrollingChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow)
    }

    override fun dispatchNestedPreScroll(
        dx: Int,
        dy: Int,
        consumed: IntArray?,
        offsetInWindow: IntArray?,
        type: Int
    ): Boolean {
        return nestedScrollingChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type)
    }

    override fun dispatchNestedFling(
        velocityX: Float,
        velocityY: Float,
        consumed: Boolean
    ): Boolean {
        return nestedScrollingChildHelper.dispatchNestedFling(velocityX, velocityY, consumed)
    }

    override fun dispatchNestedPreFling(velocityX: Float, velocityY: Float): Boolean {
        return nestedScrollingChildHelper.dispatchNestedPreFling(velocityX, velocityY)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // 터치 이벤트 빈도 제한
        if (System.currentTimeMillis() - lastTouchTime < 16) { // 60fps 제한
            return super.onTouchEvent(event)
        }
        lastTouchTime = System.currentTimeMillis()
        
        val motionEvent = MotionEvent.obtain(event)
        val action = event.actionMasked

        // VelocityTracker 초기화 및 이벤트 추가
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
        }
        velocityTracker?.addMovement(motionEvent)

        if (action == MotionEvent.ACTION_DOWN) {
            nestedOffsetY = 0
        }

        motionEvent.offsetLocation(0f, nestedOffsetY.toFloat())

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                lastMotionY = motionEvent.y
                startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH)
            }

            MotionEvent.ACTION_MOVE -> {
                val y = motionEvent.y
                var deltaY = (lastMotionY - y).toInt()
                
                // 부모에서 스크롤을 먼저 처리할 기회를 줌
                if (dispatchNestedPreScroll(0, deltaY, scrollConsumed, scrollOffset, ViewCompat.TYPE_TOUCH)) {
                    deltaY -= scrollConsumed[1]
                    motionEvent.offsetLocation(0f, scrollOffset[1].toFloat())
                    nestedOffsetY += scrollOffset[1]
                }

                lastMotionY = y - scrollOffset[1]

                val oldY = scrollY
                val newScrollY = (oldY + deltaY).coerceAtLeast(0)
                val dyConsumed = newScrollY - oldY
                val dyUnconsumed = deltaY - dyConsumed

                // 남은 스크롤을 부모에게 전달
                if (dispatchNestedScroll(0, dyConsumed, 0, dyUnconsumed, scrollOffset, ViewCompat.TYPE_TOUCH)) {
                    lastMotionY -= scrollOffset[1]
                    motionEvent.offsetLocation(0f, scrollOffset[1].toFloat())
                    nestedOffsetY += scrollOffset[1]
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // 속도 계산
                velocityTracker?.computeCurrentVelocity(1000, maximumVelocity.toFloat())
                val velocityY = velocityTracker?.yVelocity ?: 0f
                
                // fling 이벤트를 먼저 부모에게 전달
                if (abs(velocityY) > minimumVelocity) {
                    if (!dispatchNestedPreFling(0f, -velocityY)) {
                        dispatchNestedFling(0f, -velocityY, false)
                    }
                }
                
                // NestedScrolling 종료 (중요!)
                stopNestedScroll(ViewCompat.TYPE_TOUCH)
                
                // VelocityTracker 정리
                velocityTracker?.recycle()
                velocityTracker = null
            }
        }

        val result = super.onTouchEvent(motionEvent)
        motionEvent.recycle()
        return result
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // VelocityTracker 정리
        velocityTracker?.recycle()
        velocityTracker = null
        // 메모리 누수 방지를 위해 helper 정리
        nestedScrollingChildHelper.onDetachedFromWindow()
    }
}