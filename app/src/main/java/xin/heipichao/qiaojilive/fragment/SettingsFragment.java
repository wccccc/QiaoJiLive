package xin.heipichao.qiaojilive.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import xin.heipichao.qiaojilive.R;
import xin.heipichao.qiaojilive.data.bean.Cdn;
import xin.heipichao.qiaojilive.data.bean.LiveRoom;
import xin.heipichao.qiaojilive.data.bean.Stream;

public class SettingsFragment extends Fragment implements AdapterView.OnItemSelectedListener {
    private Spinner mPlayerTypeSpinner;
    private Spinner mLineSpinner;
    private Spinner mResolutionSpinner;
    private View mContentView;
    private int mPlayerType;
    private int mLine;
    private int mResolution;
    private LiveRoom mRoom;
    private static final String[] PLAYERS={
            "ExoPlayer",
            "MediaPlayer",
            "IjkPlayer"
    };

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if(mContentView==null){
            mContentView=inflater.inflate(R.layout.fragment_settings,null);
            mPlayerTypeSpinner=mContentView.findViewById(R.id.setting_player_type);
            mLineSpinner=mContentView.findViewById(R.id.setting_line);
            mResolutionSpinner=mContentView.findViewById(R.id.setting_resolution);
            mPlayerTypeSpinner.setOnItemSelectedListener(this);
            mLineSpinner.setOnItemSelectedListener(this);
            mResolutionSpinner.setOnItemSelectedListener(this);
        }
        ArrayAdapter<String> playerTypeAdapter=new ArrayAdapter<>(getContext(),android.R.layout.simple_expandable_list_item_1,PLAYERS);
        mPlayerTypeSpinner.setAdapter(playerTypeAdapter);
        mPlayerTypeSpinner.setSelection(mPlayerType);
        ArrayAdapter<String> lineAdapter=new ArrayAdapter<>(getContext(),android.R.layout.simple_expandable_list_item_1);
        for(Cdn cdn:mRoom.getCdns()){
            lineAdapter.add(cdn.getName());
        }
        mLineSpinner.setAdapter(lineAdapter);
        mLineSpinner.setSelection(mLine);
        ArrayAdapter<String> resolutionAdapter=new ArrayAdapter<>(getContext(),android.R.layout.simple_expandable_list_item_1);
        for(Stream stream:mRoom.getStreams()){
            resolutionAdapter.add(stream.getName());
        }
        mResolutionSpinner.setAdapter(resolutionAdapter);
        mResolutionSpinner.setSelection(mResolution);
        return mContentView;
    }

    public void setRoom(LiveRoom room) {
        this.mRoom = room;
    }

    public void setPlayerType(int playerType) {
        this.mPlayerType = playerType;
    }

    public void setLine(int line) {
        this.mLine = line;
    }

    public void setResolution(int resolution) {
        this.mResolution = resolution;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        switch (parent.getId()){
            case R.id.setting_player_type:
                break;
            case R.id.setting_line:
                break;
            case R.id.setting_resolution:
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
