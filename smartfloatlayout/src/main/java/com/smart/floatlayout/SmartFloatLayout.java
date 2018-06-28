package com.smart.floatlayout;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;


/**
 * @author fengjh
 * @date 2018/6/26
 * @email fengjianhui1989@gmail.com
 */
public class SmartFloatLayout extends FrameLayout {

    public static final String sTagSmartFloat = "SmartFloatLayout";
    public static final String KEY_FLOATING_X = "KEY_FLOATING_X";
    public static final String KEY_FLOATING_Y = "KEY_FLOATING_Y";
    private View mFloatView;
    private ViewDragHelper mViewDragHelper;
    SharedPreferences sp = getContext().getSharedPreferences(sTagSmartFloat, Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = sp.edit();
    private float mXRadio, mYRadio;
    private DIRECTION mDirection;

    private enum DIRECTION {
        LEFT,
        RIGHT,
        TOP,
        BOTTOM
    }

    public SmartFloatLayout(@NonNull Context context) {
        this(context, null);
    }

    public SmartFloatLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SmartFloatLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, 0);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public SmartFloatLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        mViewDragHelper = ViewDragHelper.create(this, 1.0f, mFloatCallback);
        if (editor != null) {
            editor.clear().apply();
        }
    }

    private ViewDragHelper.Callback mFloatCallback = new ViewDragHelper.Callback() {
        @Override
        public boolean tryCaptureView(@NonNull View child, int pointerId) {
            if (mFloatView != null) {
                return child == mFloatView;
            }
            return false;
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            if (top > getHeight() - child.getMeasuredHeight()) {
                top = getHeight() - child.getMeasuredHeight();
            } else if (top < 0) {
                top = 0;
            }
            return top;
        }

        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            if (left > getWidth() - child.getMeasuredWidth()) {
                left = getWidth() - child.getMeasuredWidth();
            } else if (left < 0) {
                left = 0;
            }
            return left;
        }

        @Override
        public int getViewVerticalDragRange(View child) {
            return getMeasuredHeight() - child.getMeasuredHeight();
        }

        @Override
        public int getViewHorizontalDragRange(View child) {
            return getMeasuredWidth() - child.getMeasuredWidth();
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            super.onViewPositionChanged(changedView, left, top, dx, dy);
            savePosition();
        }

        @Override
        public void onViewDragStateChanged(int state) {
            super.onViewDragStateChanged(state);
            if (state == ViewDragHelper.STATE_SETTLING) { // 拖拽结束
                restorePosition();
            }
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            if (mFloatView == null) return;
            if (releasedChild == mFloatView) {
                float x = mFloatView.getX();
                float y = mFloatView.getY();
                if (x < (getMeasuredWidth() / 2f - releasedChild.getMeasuredWidth() / 2f)) { // 0-x/2
                    if (x < releasedChild.getMeasuredWidth() / 3f) {
                        mDirection = DIRECTION.LEFT;//left(bottom)
                        x = 0;
                    } else if (y < (releasedChild.getMeasuredHeight() * 3)) { // 0-y/3
                        mDirection = DIRECTION.TOP;//top(left)
                        y = 0;
                    } else if (y > (getMeasuredHeight() - releasedChild.getMeasuredHeight() * 3)) { // 0-(y-y/3)
                        mDirection = DIRECTION.BOTTOM;//bottom(left)
                        y = getMeasuredHeight() - releasedChild.getMeasuredHeight();
                    } else {
                        mDirection = DIRECTION.LEFT;//left(top)
                        x = 0;
                    }
                } else { // x/2-x
                    if (x > getMeasuredWidth() - releasedChild.getMeasuredWidth() / 3f - releasedChild.getMeasuredWidth()) {
                        mDirection = DIRECTION.RIGHT;//right(top)
                        x = getMeasuredWidth() - releasedChild.getMeasuredWidth();
                    } else if (y < (releasedChild.getMeasuredHeight() * 3)) { // 0-y/3
                        mDirection = DIRECTION.TOP;//top(right)
                        y = 0;
                    } else if (y > (getMeasuredHeight() - releasedChild.getMeasuredHeight() * 3)) { // 0-(y-y/3)
                        mDirection = DIRECTION.BOTTOM;//bottom(right)
                        y = getMeasuredHeight() - releasedChild.getMeasuredHeight();
                    } else {
                        mDirection = DIRECTION.RIGHT;//right(bottom)
                        x = getMeasuredWidth() - releasedChild.getMeasuredWidth();
                    }
                }
                int measuredWidth = getMeasuredWidth();
                int measuredHeight = getMeasuredHeight();
                mXRadio = x / measuredWidth * 1.0f;
                mYRadio = y / measuredHeight * 1.0f;
                /**
                 * 移动到指定位置
                 */
                mViewDragHelper.smoothSlideViewTo(releasedChild, (int) x, (int) y);
                invalidate();
            }
        }
    };

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        restorePosition();
    }

    /**
     * 保存位置
     */
    void savePosition() {
        if (mFloatView == null) return;
        float x = mFloatView.getX();
        float y = mFloatView.getY();
        editor.putFloat(KEY_FLOATING_X, x);
        editor.putFloat(KEY_FLOATING_Y, y);
        editor.commit();
    }

    /**
     * 更新位置
     */
    public void restorePosition() {
        if (mFloatView == null) return;
        int width = mFloatView.getMeasuredWidth();
        int height = mFloatView.getMeasuredHeight();
        int measuredWidth = getMeasuredWidth();
        int measuredHeight = getMeasuredHeight();
        // 读取保存的位置
        float x = sp.getFloat(KEY_FLOATING_X, -1);
        float y = sp.getFloat(KEY_FLOATING_Y, -1);
        if (x != -1 && y != -1) {
            x = mXRadio * measuredWidth;
            y = mYRadio * measuredHeight;
            if (mDirection == DIRECTION.LEFT) {
                x = 0;
                if (y + height > measuredHeight) {
                    y = measuredHeight - height;
                }
            } else if (mDirection == DIRECTION.RIGHT) {
                x = measuredWidth - width;
                if (y + height > measuredHeight) {
                    y = measuredHeight - height;
                }
            } else if (mDirection == DIRECTION.TOP) {
                y = 0;
            } else if (mDirection == DIRECTION.BOTTOM) {
                y = measuredHeight - height;
            }
            /**
             * 移动到指定位置
             */
            mViewDragHelper.smoothSlideViewTo(mFloatView, (int) x, (int) y);
            invalidate();
            savePosition();
        } else {
            if (x == -1 && y == -1) { // 初始位置
                x = measuredWidth - width;
                y = measuredHeight * 2 / 3;
            }
            mFloatView.layout((int) x, (int) y, (int) x + width, (int) y + height);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mViewDragHelper.shouldInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mViewDragHelper.processTouchEvent(event);
        return true;
    }

    @Override
    public void computeScroll() {
        if (mViewDragHelper.continueSettling(true)) {
            invalidate();
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child != null) {
                Object childTag = child.getTag();
                if (childTag != null && childTag instanceof String && sTagSmartFloat.equals(childTag)) {
                    mFloatView = child;
                    break;
                }
            }
        }
    }
}