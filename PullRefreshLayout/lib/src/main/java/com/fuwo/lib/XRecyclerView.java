package com.fuwo.lib;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.AttributeSet;
import android.view.View;

/**
 * 实现IPull接口的RecyclerView
 *
 * @author youngbin
 *         2016-09-20
 */
public class XRecyclerView extends RecyclerView implements IPull {

    public static final String TAG = "XRecyclerView";

    private boolean isLoad;

    public XRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        isLoad = true;
    }

    public XRecyclerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public XRecyclerView(Context context) {
        this(context, null);
    }


    public boolean isLoad() {
        return isLoad;
    }

    public void setLoad(boolean load) {
        isLoad = load;
    }

    @Override
    public boolean pullDown() {
        int firstPosition = getFirstPosition();
        int top = -1;
        if (firstPosition == 0) {
            View topView = getChildView(0);
            top = topView != null ? topView.getTop() : 0;
        }
        return getCount() == 0 || top >= 0;
    }

    @Override
    public boolean pullUp() {
        if(!isLoad){
            return false;
        }
        int lastPosition = getLastPosition();
        int bottom = -1;
        if (getCount() > 0 && lastPosition == getCount() - 1) {
            View bottomView = getChildView(lastPosition);
            bottom = bottomView != null ? bottomView.getBottom() : -1;
        }
        return getCount() > 0 && bottom >= 0 && getMeasuredHeight() >= bottom;
    }

    private int getFirstPosition() {
        int position = -1;
        LayoutManager manager = getLayoutManager();
        if (manager instanceof LinearLayoutManager) {
            position = ((LinearLayoutManager) manager).findFirstVisibleItemPosition();
        } else if (getLayoutManager() instanceof StaggeredGridLayoutManager) {
            StaggeredGridLayoutManager layoutManager = (StaggeredGridLayoutManager) getLayoutManager();
            int[] positions = layoutManager.findFirstVisibleItemPositions(new int[layoutManager.getSpanCount()]);
            position = getMinPosition(positions);
        }
        return position;
    }

    private int getLastPosition() {
        int position = -1;
        LayoutManager manager = getLayoutManager();
        if (manager instanceof LinearLayoutManager) {
            position = ((LinearLayoutManager) manager).findLastVisibleItemPosition();
        } else if (getLayoutManager() instanceof StaggeredGridLayoutManager) {
            StaggeredGridLayoutManager layoutManager = (StaggeredGridLayoutManager) getLayoutManager();
            int[] positions = layoutManager.findLastVisibleItemPositions(new int[layoutManager.getSpanCount()]);
            position = getMaxPosition(positions);
        }
        return position;
    }

    private int getCount() {
        return getLayoutManager().getItemCount();
    }

    private View getChildView(int position) {
        return getLayoutManager().findViewByPosition(position);
    }

    /**
     * 获得最小的位置
     */
    private int getMinPosition(int[] positions) {
        int minPosition = Integer.MAX_VALUE;
        for (int position : positions) {
            minPosition = Math.min(minPosition, position);
        }
        return minPosition;
    }

    /**
     * 获得最大的位置
     */
    private int getMaxPosition(int[] positions) {
        int maxPosition = Integer.MIN_VALUE;
        for (int position : positions) {
            maxPosition = Math.max(maxPosition, position);
        }
        return maxPosition;
    }

}
