package xin.heipichao.qiaojilive.view;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;

import java.util.List;

import xin.heipichao.qiaojilive.R;
import xin.heipichao.qiaojilive.data.bean.LiveRoom;

/**
 * Created by Chaochao.Wen on 2018/2/6.
 */

public class RoomsAdapter extends RecyclerView.Adapter<RoomsAdapter.RoomViewHolder>{
    private List<LiveRoom> mRooms;
    private OnItemClickListener mOnItemClickListener;

    public RoomsAdapter(List<LiveRoom> rooms){
        mRooms=rooms;
    }

    @Override
    public RoomViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new RoomViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_room,parent,false));
    }

    @Override
    public void onBindViewHolder(final RoomViewHolder holder, final int position) {
        LiveRoom room=mRooms.get(position);
        Glide.with(holder.itemView.getContext().getApplicationContext()).asBitmap()
                .load(room.getRoomImg()).into(new SimpleTarget<Bitmap>(374,300) {
            @Override
            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                holder.mLiveImg.setImageBitmap(resource);
            }
        });
        holder.mUserNick.setText(room.getUserNick());
        holder.mUserNum.setText(room.getNum());
        holder.mTitle.setText(room.getTitle());
        holder.mClickView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mOnItemClickListener!=null){
                    mOnItemClickListener.onItemClick(holder,position);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mRooms==null?0:mRooms.size();
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.mOnItemClickListener = onItemClickListener;
    }

    public static class RoomViewHolder extends RecyclerView.ViewHolder{
        ImageView mLiveImg;
        TextView mTitle;
        TextView mUserNick;
        TextView mUserNum;
        View mClickView;
        public RoomViewHolder(View itemView) {
            super(itemView);
            mLiveImg=itemView.findViewById(R.id.iv_liveImg);
            mTitle=itemView.findViewById(R.id.tv_roomTitle);
            mUserNick=itemView.findViewById(R.id.tv_userNick);
            mUserNum=itemView.findViewById(R.id.tv_num);
            mClickView=itemView.findViewById(R.id.v_clickView);
        }
    }

    public interface OnItemClickListener{
        void onItemClick(RoomViewHolder holder,int position);
    }
}
