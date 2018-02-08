package xin.heipichao.qiaojilive.view;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.List;

/**
 * Created by Chaochao.Wen on 2018/2/6.
 */

public class ViewPagerAdapter extends FragmentPagerAdapter{
    private List<Fragment> mFragments;
    public ViewPagerAdapter(FragmentManager fm, List<Fragment> fragments) {
        super(fm);
        setFragments(fragments);
    }

    public void setFragments(List<Fragment> fragments) {
        this.mFragments = fragments;
    }

    public List<Fragment> getFragments() {
        return mFragments;
    }

    @Override
    public Fragment getItem(int position) {
        return mFragments==null?null:mFragments.get(position);
    }

    @Override
    public int getCount() {
        return mFragments==null?0:mFragments.size();
    }
}
