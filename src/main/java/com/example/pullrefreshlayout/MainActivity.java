package com.example.pullrefreshlayout;

import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import com.example.pullrefreshlayout.view.PullRefreshLayout;
import com.example.pullrefreshlayout.view.XRecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";

    private PullRefreshLayout mRefreshLayout;
    private XRecyclerView mXRecyclerView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mRefreshLayout = (PullRefreshLayout) findViewById(R.id.main_refresh_layout);
        mXRecyclerView = (XRecyclerView) findViewById(R.id.main_recycler);

        LinearLayoutManager manager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);

//        GridLayoutManager manager = new GridLayoutManager(this, 2, GridLayoutManager.VERTICAL, false);


        mXRecyclerView.setLayoutManager(manager);
        mXRecyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                super.getItemOffsets(outRect, view, parent, state);
                outRect.bottom = 2;
                outRect.top = 2;
                outRect.right = 2;
                outRect.left = 2;
            }
        });


        List<String> list = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            list.add("item" + (i));
        }
        mRefreshLayout.setAutoLoad(true);

        final ItemAdapter adapter = new ItemAdapter(list);
        mXRecyclerView.setAdapter(adapter);


        mRefreshLayout.setOnRefreshListener(new PullRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.i(TAG, "刷新");
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mRefreshLayout.stopRefreshAndLoad();
                    }
                }, 2000);
            }
        });

        mRefreshLayout.setOnLoadListener(new PullRefreshLayout.OnLoadListener() {
            @Override
            public void onLoad() {
                Log.i(TAG, "加载更多");
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (adapter.getItemCount() > 20) {
                            mRefreshLayout.setNoMore(true);
                        } else {
                            adapter.addItem("add item");
                        }
                        mRefreshLayout.stopRefreshAndLoad();
                    }
                }, 2000);
            }
        });
        mRefreshLayout.postRefresh(true);
    }
}
