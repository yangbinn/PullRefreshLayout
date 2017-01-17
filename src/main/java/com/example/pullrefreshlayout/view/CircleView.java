package com.example.pullrefreshlayout.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.example.pullrefreshlayout.AppUtil;

/**
 * 圆环控件
 */

public class CircleView extends View {

    public static final String TAG = "CircleView";

    private Paint mBGPaint;
    private Paint mPaint;
    private RectF mRectF;
    private int mStrokeWidth;
    private int mDegree;
    private PaintFlagsDrawFilter pfd;


    public CircleView(Context context) {
        this(context, null);
    }

    public CircleView(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public CircleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }


    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            if (mRectF == null)
                mRectF = new RectF();

            int width = right - left - getPaddingLeft() - getPaddingRight() - mStrokeWidth;
            int height = bottom - top - getPaddingTop() - getPaddingBottom() - mStrokeWidth;
            int halfStrokeWidth = mStrokeWidth / 2;

            if (width >= height) {
                mRectF.left = (width - height) / 2f + halfStrokeWidth;
                mRectF.right = mRectF.left + height;
                //noinspection SuspiciousNameCombination
                mRectF.top = halfStrokeWidth;
                mRectF.bottom = height + halfStrokeWidth;
            } else {
                mRectF.left = halfStrokeWidth;
                mRectF.right = width + halfStrokeWidth;
                mRectF.top = (height - width) / 2f + halfStrokeWidth;
                mRectF.bottom = mRectF.top + width;
            }
            Log.i(TAG, left + " " + top + " " + right + " " + bottom);
            Log.i(TAG, "rect=" + mRectF.toString());
        }
    }

    private void init() {
        mStrokeWidth = AppUtil.dip2px(2);
        mPaint = new Paint();
        mPaint.setColor(0xFF0000FF);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(mStrokeWidth);
        mPaint.setAntiAlias(true);
        mPaint.setDither(false);


        mBGPaint = new Paint();
        mBGPaint.setColor(0xFFAAAAAA);
        mBGPaint.setStyle(Paint.Style.STROKE);
        mBGPaint.setStrokeWidth(mStrokeWidth);
        mBGPaint.setAntiAlias(true);
        mBGPaint.setDither(false);
        mDegree = 0;
        pfd = new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.setDrawFilter(pfd);
        canvas.drawArc(mRectF, 0, 360, false, mBGPaint);
        canvas.drawArc(mRectF, 270, mDegree, false, mPaint);
    }

    public void setDegree(int degree){
        mDegree = degree;
        invalidate();
    }


    public void startAnim(){
        mDegree = 300;
        invalidate();
    }
}
