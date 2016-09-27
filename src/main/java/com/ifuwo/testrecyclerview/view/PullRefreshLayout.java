package com.ifuwo.testrecyclerview.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ifuwo.testrecyclerview.AppUtil;
import com.ifuwo.testrecyclerview.R;
import com.ifuwo.testrecyclerview.view.biz.IPull;

/**
 * 下拉刷新 上拉加载
 */
public class PullRefreshLayout extends RelativeLayout implements View.OnClickListener {

    public static final String TAG = "PullRefreshLayout";

    public final int REFRESH_MIN = AppUtil.dip2px(60);         // 最小刷新距离

    public static final int PULL_DOWN_TO_REFRESH = 1; // 下拉刷新
    public static final int REFRESHING = 2; //刷新中
    public static final int RELEASE_TO_REFRESH = 3; //释放刷新

    public static final int PULL_UP_TO_LOAD = 4;      // 上拉加载
    public static final int LOADING = 5;    // 加载中
    public static final int RELEASE_TO_LOAD = 6;    // 释放加载

    private Context mContext;
    private View mContentView;
    private View mHeaderView;
    private ImageView mHeaderImageView;

    private View mFooterView;
    private ImageView mFooterImageView;
    private TextView mNoMoreTextView;

    private Animation mRotateAnim;
    private int mScrollY;
    private float mLastY;
    private boolean mPulled;
    private int mPullState;
    private boolean mNoMore;


    private OnRefreshListener mRefreshListener;
    private OnLoadListener mLoadListener;

    public PullRefreshLayout(Context context) {
        super(context, null);
    }

    public PullRefreshLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PullRefreshLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        mRotateAnim = AnimationUtils.loadAnimation(context, R.anim.rotate);
        mNoMore = false;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (getChildCount() == 1) {
            mContentView = getChildAt(0);
            ViewGroup.LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            mHeaderView = LayoutInflater.from(mContext).inflate(R.layout.refresh_header, this, false);
            mHeaderImageView = (ImageView) mHeaderView.findViewById(R.id.header_pull_iv);
            addView(mHeaderView, params);
            mFooterView = LayoutInflater.from(mContext).inflate(R.layout.refresh_footer, this, false);
            mFooterImageView = (ImageView) mFooterView.findViewById(R.id.footer_pull_iv);
            mNoMoreTextView = (TextView) mFooterView.findViewById(R.id.footer_no_more_tv);
            mNoMoreTextView.setVisibility(GONE);
            mNoMoreTextView.setOnClickListener(this);
            addView(mFooterView, params);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (mContentView != null && mHeaderView != null && mFooterView != null) {
            int contentHeight = mContentView.getMeasuredHeight();
            mContentView.layout(l, mScrollY, r, contentHeight + mScrollY);
            mHeaderView.layout(l, -mHeaderView.getMeasuredHeight() + mScrollY, r, mScrollY);
            mFooterView.layout(l, contentHeight + mScrollY, r, contentHeight + mScrollY + mFooterView.getMeasuredHeight());
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downY(ev);
                break;
            case MotionEvent.ACTION_MOVE:
                moveY(ev);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                upY(ev);
                break;
            default:
        }
        return mPulled || super.dispatchTouchEvent(ev);
    }

    private void downY(MotionEvent ev) {
        mLastY = (int) ev.getY();
        mPulled = false;
        if (mPullState != REFRESHING && mPullState != LOADING) {
            if (!mNoMore) {
                mScrollY = 0;
            }
            mHeaderImageView.clearAnimation();
            mFooterImageView.clearAnimation();
        }
    }

    private void moveY(MotionEvent ev) {
        if (ev.getPointerCount() != 1)
            return;
        float moveY = ev.getY();
        float y = moveY - mLastY;
        mPulled = false;
        if (((IPull) mContentView).pullDown()) {
            mPulled = 0 < y || mScrollY > 0;
            if(mPullState != REFRESHING && mPullState != LOADING){
                mPullState = PULL_DOWN_TO_REFRESH;
            }
        } else if (((IPull) mContentView).pullUp()) {
            mPulled = 0 > y || mScrollY < 0;
            if(mPullState != REFRESHING && mPullState != LOADING){
                mPullState = PULL_UP_TO_LOAD;
            }
        }
        if (mPulled && (Math.abs(y) > 5 || mScrollY != 0)) {
            rotateView(y);
            requestLayout();
        }
        mLastY = moveY;
    }

    private void upY(MotionEvent ev) {
        mPulled = false;
        if (checkRefreshState()) {
            ev.setAction(MotionEvent.ACTION_CANCEL);
            if (mPullState == REFRESHING) {
                postRefresh(true);
            } else if (mPullState == LOADING) {
                postLoad(true);
            } else {
                stopRefreshAndLoad();
            }
        }
    }

    /**
     * 下拉状态， 旋转图标
     * 刷新状态时， 下拉高度小于刷新最小高度
     */
    private void rotateView(float y) {
        mScrollY += y / 2;
        if (mPullState == REFRESHING || mPullState == LOADING) {
            if (mScrollY > REFRESH_MIN) {
                mScrollY = REFRESH_MIN;
            } else if (mScrollY < -REFRESH_MIN) {
                mScrollY = -REFRESH_MIN;
            }
        } else if (mScrollY < 0 && mNoMore) {
            //没有更多 且 上拉
            mScrollY += y / 2;
            if (mScrollY < -REFRESH_MIN)
                mScrollY = -REFRESH_MIN;
        } else {
            if (mScrollY > 0) {
                mHeaderImageView.setRotation(getDegree());
            } else {
                mFooterImageView.setRotation(getDegree());
            }
        }
        //移动之间距离过大时， 避免下拉出现上拉现象
        if (((mPullState == PULL_DOWN_TO_REFRESH || mPullState == REFRESHING) && mScrollY < 0) ||
                ((mPullState == PULL_UP_TO_LOAD || mPullState == LOADING) && mScrollY > 0)) {
            mScrollY = 0;
        }
    }

    private float getDegree() {
        return (float) mScrollY / REFRESH_MIN * 360;
    }

    /**
     * 判断是否刷新
     */
    private boolean checkRefreshState() {
        boolean result = false;
        if (mScrollY < 0 && mNoMore) {
            result = false;
        } else if (mPullState != REFRESHING && mPullState != LOADING) {
            if (mScrollY >= REFRESH_MIN) {
                mPullState = REFRESHING;
                result = true;
            } else if (mScrollY <= -REFRESH_MIN) {
                mPullState = LOADING;
                result = true;
            }
        }
        return result;
    }

    /**
     * 停止刷新
     */
    public void stopRefreshAndLoad() {
        Log.i(TAG, "stopRefreshAndLoad, scrollY=" + mScrollY);
        if (mScrollY == 0) {
            mPullState = PULL_DOWN_TO_REFRESH;
            return;
        }
        if (mScrollY < 0 && mNoMore) {
            mScrollY = -REFRESH_MIN;
        } else {
            TranslateAnimation animation = new TranslateAnimation(0, 0, mScrollY, 0);
            animation.setDuration(200);
            mContentView.clearAnimation();
            mContentView.startAnimation(animation);
            if (mScrollY > 0) {
                mHeaderView.clearAnimation();
                mHeaderView.startAnimation(animation);
            } else {
                mFooterView.clearAnimation();
                mFooterView.startAnimation(animation);
            }
            mScrollY = 0;
        }
        mHeaderImageView.clearAnimation();
        mFooterImageView.clearAnimation();
        mPulled = false;
        mPullState = PULL_DOWN_TO_REFRESH;
        requestLayout();
    }


    /**
     * 执行刷新
     */
    public void postRefresh(boolean anim) {
        if (anim) {
            mScrollY = REFRESH_MIN;
            mPullState = REFRESHING;
            mHeaderImageView.clearAnimation();
            mHeaderImageView.startAnimation(mRotateAnim);
            requestLayout();
        }
        if (mRefreshListener != null) {
            mRefreshListener.onRefresh();
        } else {
            stopRefreshAndLoad();
        }
    }

    /**
     * 执行加载
     */
    public void postLoad(boolean anim) {
        setNoMore(false);
        if (anim) {
            mScrollY = -REFRESH_MIN;
            mPullState = LOADING;
            mFooterImageView.clearAnimation();
            mFooterImageView.startAnimation(mRotateAnim);
            requestLayout();
        }
        if (mLoadListener != null) {
            mLoadListener.onLoad();
        } else {
            stopRefreshAndLoad();
        }
    }

    public void setNoMore(boolean noMore) {
        mNoMore = noMore;
        mFooterImageView.clearAnimation();
        if (noMore) {
            mFooterImageView.setVisibility(GONE);
            mNoMoreTextView.setVisibility(VISIBLE);
        } else {
            mFooterImageView.setVisibility(VISIBLE);
            mNoMoreTextView.setVisibility(GONE);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.footer_no_more_tv:
                postLoad(true);
                break;
            default:
        }
    }

    public interface OnRefreshListener {
        void onRefresh();
    }

    public void setOnRefreshListener(OnRefreshListener listener) {
        this.mRefreshListener = listener;
    }

    public interface OnLoadListener {
        void onLoad();

    }

    public void setOnLoadListener(OnLoadListener listener) {
        this.mLoadListener = listener;
    }


}
