package com.example.pullrefreshlayout.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.pullrefreshlayout.AppUtil;
import com.example.pullrefreshlayout.R;
import com.example.pullrefreshlayout.view.biz.IPull;

/**
 * 下拉刷新 上拉加载
 */
public class PullRefreshLayout extends RelativeLayout implements View.OnClickListener {

    public final int REFRESH_MIN = AppUtil.dip2px(60); // 最小刷新距离
    public final int REFRESH_MAX = AppUtil.dip2px(150);// 最大刷新距离
    public final int ROTATE_MIN = AppUtil.dip2px(42); //开始旋转的小距离


    public static final int PULL_DOWN_TO_REFRESH = 1; // 下拉刷新
    public static final int REFRESHING = 2; //刷新中
    public static final int RELEASE_TO_REFRESH = 3; //释放刷新

    public static final int PULL_UP_TO_LOAD = 4;      // 上拉加载
    public static final int LOADING = 5;    // 加载中
    public static final int RELEASE_TO_LOAD = 6;    // 释放加载

    private Context mContext;
    private View mContentView;
    private View mHeaderView;
    private CircleView mHeaderCircleView;

    private View mFooterView;
    private CircleView mFooterCircleView;
    private TextView mNoMoreTextView;

    private Animation mRotateAnim;
    private int mScrollY;
    private float mLastX;
    private float mLastY;
    private boolean mPulled;
    private int mPullState;
    private boolean mNoMore;
    private boolean mAutoLoad;

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
        mAutoLoad = false;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (getChildCount() > 0) {
            mContentView = getChildAt(0);
            mHeaderView = LayoutInflater.from(mContext).inflate(R.layout.refresh_header, this, false);
            RelativeLayout.LayoutParams params = (LayoutParams) mHeaderView.getLayoutParams();
            mHeaderCircleView = (CircleView) mHeaderView.findViewById(R.id.header_pull_iv);
            addView(mHeaderView, params);

            mFooterView = LayoutInflater.from(mContext).inflate(R.layout.refresh_footer, this, false);
            RelativeLayout.LayoutParams params1 = (LayoutParams) mFooterView.getLayoutParams();
            mFooterCircleView = (CircleView) mFooterView.findViewById(R.id.footer_pull_iv);
            mNoMoreTextView = (TextView) mFooterView.findViewById(R.id.footer_no_more_tv);
            mNoMoreTextView.setOnClickListener(this);
            addView(mFooterView, params1);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        int height = b - t;
        for (int i = 0, size = getChildCount(); i < size; i++) {
            View child = getChildAt(i);
            int top, bottom;
            if (child == mHeaderView) {
                top = t - height;
                bottom = t;
            } else if (child == mFooterView) {
                top = b;
                bottom = b + height;
            } else {
                top = t;
                bottom = b;
            }
            child.layout(l, top + mScrollY, r, bottom + mScrollY);
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
            case MotionEvent.ACTION_POINTER_UP:
                mLastX = ev.getX(1);
                mLastY = ev.getY(1);
                break;
            case MotionEvent.ACTION_POINTER_UP | 0x0100:
                mLastX = ev.getX(0);
                mLastY = ev.getY(0);
                break;

            default:
        }
        return mPulled || super.dispatchTouchEvent(ev);
    }

    private void downY(MotionEvent ev) {
        mLastX = ev.getX();
        mLastY = ev.getY();
        mPulled = false;
        if (mPullState != REFRESHING && mPullState != LOADING) {
            if (!mNoMore) {
                mScrollY = 0;
            }
            mHeaderCircleView.clearAnimation();
            mFooterCircleView.clearAnimation();
        }
    }

    private void moveY(MotionEvent ev) {
        float moveX = ev.getX();
        float moveY = ev.getY();
        float y = moveY - mLastY;
        mLastX = moveX;
        mLastY = moveY;
        if (Math.abs(y) > Math.abs(moveX - mLastX)) {
            if (((IPull) mContentView).pullDown()) {
                mPulled = 0 < y || mScrollY > 0;
                if (mPullState != REFRESHING && mPullState != LOADING) {
                    mPullState = PULL_DOWN_TO_REFRESH;
                }
            } else if (((IPull) mContentView).pullUp()) {
                if (mAutoLoad && !mNoMore) {
                    postLoad(false);
                }
                mPulled = 0 > y || mScrollY < 0;
                if (mPullState != REFRESHING && mPullState != LOADING) {
                    mPullState = PULL_UP_TO_LOAD;
                }
            }
        }
        if (mPulled && ((Math.abs(y) > 5 || mScrollY != 0))) {
            rotateView(y);
            requestLayout();
            setPressed(false);
        }
    }

    private void upY(MotionEvent ev) {
        if (checkRefreshState()) {
            if (mPullState == RELEASE_TO_REFRESH) {
                postRefresh(true);
            } else if (mPullState == RELEASE_TO_LOAD) {
                postLoad(true);
            } else {
                stopRefreshAndLoad();
            }
        }
        //当轻微上下拉的时候，事件取消
        if (mPulled) {
            ev.setAction(MotionEvent.ACTION_CANCEL);
            mPulled = false;
        }
    }

    /**
     * 下拉状态， 旋转图标
     * 刷新状态时， 下拉高度小于刷新最小高度
     */
    private void rotateView(float y) {
        mScrollY += y / 2;
        if (Math.abs(mScrollY) > REFRESH_MAX) {
            mScrollY = mScrollY > 0 ? REFRESH_MAX : -REFRESH_MAX;
        }
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
                if (mScrollY > ROTATE_MIN)
                    mHeaderCircleView.setDegree((int) getDegree());
            } else {
                if (mScrollY < -ROTATE_MIN)
                    mFooterCircleView.setDegree((int) getDegree());
            }
        }
        //移动之间距离过大时， 避免下拉出现上拉现象
        if (((mPullState == PULL_DOWN_TO_REFRESH || mPullState == REFRESHING) && mScrollY < 0) ||
                ((mPullState == PULL_UP_TO_LOAD || mPullState == LOADING) && mScrollY > 0)) {
            mScrollY = 0;
        }
    }

    private float getDegree() {
        return (float) (Math.abs(mScrollY) - ROTATE_MIN) / ROTATE_MIN * 360;
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
                mPullState = RELEASE_TO_REFRESH;
            } else if (mScrollY <= -REFRESH_MIN) {
                mPullState = RELEASE_TO_LOAD;
            }
            result = true;
        }
        return result;
    }

    /**
     * 停止刷新
     */
    public void stopRefreshAndLoad() {
        if (mScrollY == 0) {
            mPullState = PULL_DOWN_TO_REFRESH;
            requestLayout();
            return;
        }
        if (mScrollY < 0 && mNoMore) {
            mScrollY = -REFRESH_MIN;
        } else {
            TranslateAnimation animation = new TranslateAnimation(0, 0, mScrollY, 0);
            animation.setDuration(200);
            for (int i = 0, size = getChildCount(); i < size; i++) {
                View view = getChildAt(i);
                view.clearAnimation();
                view.startAnimation(animation);
            }
            if (mScrollY > 0) {
                mHeaderView.clearAnimation();
                mHeaderView.startAnimation(animation);
            } else {
                mFooterView.clearAnimation();
                mFooterView.startAnimation(animation);
            }
            mScrollY = 0;
        }
        mHeaderCircleView.clearAnimation();
        mHeaderCircleView.setDegree(0);
        mFooterCircleView.clearAnimation();
        mFooterCircleView.setDegree(0);
        mPullState = PULL_DOWN_TO_REFRESH;
        requestLayout();
    }


    /**
     * 执行刷新
     */
    public void postRefresh(boolean anim) {
        if (mPullState == REFRESHING || mPullState == LOADING) {
            return;
        }
        if (anim) {
            mScrollY = REFRESH_MIN;
            requestLayout();
        }
        if (mRefreshListener != null) {
            setNoMore(false);
            mPullState = REFRESHING;
            mHeaderCircleView.clearAnimation();
            mHeaderCircleView.startAnim();
            mHeaderCircleView.startAnimation(mRotateAnim);
            mRefreshListener.onRefresh();
        } else {
            stopRefreshAndLoad();
        }
    }

    /**
     * 执行加载
     */
    public void postLoad(boolean anim) {
        if (mPullState == REFRESHING || mPullState == LOADING) {
            return;
        }
        if (anim) {
            mScrollY = -REFRESH_MIN;
            requestLayout();
        }
        if (mLoadListener != null) {
            setNoMore(false);
            mPullState = LOADING;
            mFooterCircleView.clearAnimation();
            mFooterCircleView.startAnim();
            mFooterCircleView.startAnimation(mRotateAnim);
            mLoadListener.onLoad();
        } else {
            stopRefreshAndLoad();
        }
    }

    public void setNoMore(boolean noMore) {
        mNoMore = noMore;
        mFooterCircleView.clearAnimation();
        if (noMore) {
            mFooterCircleView.setVisibility(GONE);
            mNoMoreTextView.setVisibility(VISIBLE);
        } else {
            mFooterCircleView.setVisibility(VISIBLE);
            mNoMoreTextView.setVisibility(GONE);
        }
    }

    public boolean isAutoLoad() {
        return mAutoLoad;
    }

    public void setAutoLoad(boolean autoLoad) {
        this.mAutoLoad = autoLoad;
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.footer_no_more_tv) {
            postLoad(true);
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
