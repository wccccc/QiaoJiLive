package xin.heipichao.qiaojilive.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;

import java.util.ArrayList;
import java.util.List;

import xin.heipichao.qiaojilive.R;
import xin.heipichao.qiaojilive.data.DataParser;
import xin.heipichao.qiaojilive.data.bean.LiveRoom;
import xin.heipichao.qiaojilive.view.RoomsAdapter;

/**
 * Created by Chaochao.Wen on 2018/2/6.
 */

public class RoomsFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, RoomsAdapter.OnItemClickListener {
    private static final String TAG="RoomsFragment";
    private List<LiveRoom> mRooms;
    private RecyclerView mRecyclerView;
    private RoomsAdapter mAdapter;
    private View mContentView;
    private SwipeRefreshLayout mRefreshView;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if(mContentView ==null){
            mContentView = inflater.inflate(R.layout.fragment_rooms,container,false);
            mRefreshView=mContentView.findViewById(R.id.sl_refresh);
            mRecyclerView=mContentView.findViewById(R.id.rv_list);
            mRecyclerView.setLayoutManager(new GridLayoutManager(getContext(),2));
            mRefreshView.setColorSchemeResources(R.color.mainColor);
            mRefreshView.setOnRefreshListener(this);
            mRefreshView.setRefreshing(true);
            requestData();
        }
        return mContentView;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void requestData(){
        DataParser.parseAllLive(DataParser.TYPE_HUYA, new DataParser.ParserCallback() {
            @Override
            public void callback(String result) {
                Log.e(TAG,"result:\n"+result);
//                mRooms=JSON.parseArray(result, LiveRoom.class);
                mRooms=JSON.parseObject(result,new TypeReference<ArrayList<LiveRoom>>(){});
                mAdapter=new RoomsAdapter(mRooms);
                mAdapter.setOnItemClickListener(RoomsFragment.this);
                mRecyclerView.setAdapter(mAdapter);
                mRefreshView.setRefreshing(false);
                mRefreshView.setEnabled(true);
            }
        });
    }

    @Override
    public void onRefresh() {
        mRefreshView.setRefreshing(false);
    }

    @Override
    public void onItemClick(RoomsAdapter.RoomViewHolder holder, int position) {
        String url=mRooms.get(position).getUrl();
        DataParser.parseOneLive(url, new DataParser.ParserCallback() {
            @Override
            public void callback(String result) {
                Log.e(TAG,"result:"+result);
            }
        });
    }
}
