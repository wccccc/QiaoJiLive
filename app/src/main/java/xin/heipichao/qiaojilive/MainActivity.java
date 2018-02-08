package xin.heipichao.qiaojilive;

import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.RadioGroup;

import java.util.ArrayList;

import xin.heipichao.qiaojilive.fragment.CategoryFragment;
import xin.heipichao.qiaojilive.fragment.RoomsFragment;
import xin.heipichao.qiaojilive.view.SelectView;
import xin.heipichao.qiaojilive.view.ViewPagerAdapter;

public class MainActivity extends AppCompatActivity implements SelectView.OnSelectChangeListener, ViewPager.OnPageChangeListener {
    private static final String TAG="MainActivity";
    private ViewPager mViewPager;
    private SelectView mSelectView;
    private RadioGroup mRadioGroup;
    private ViewPagerAdapter mAdapter;
    private RoomsFragment mRecommendFragment;
    private CategoryFragment mCategoryFragment;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findView();
        mRecommendFragment=new RoomsFragment();
        mCategoryFragment=new CategoryFragment();
        ArrayList<Fragment> fragments=new ArrayList<>();
        mAdapter=new ViewPagerAdapter(getSupportFragmentManager(),fragments);
        fragments.add(mRecommendFragment);
        fragments.add(mCategoryFragment);
        mViewPager.setAdapter(mAdapter);
        mSelectView.setOnSelectChangeListener(this);
        mViewPager.addOnPageChangeListener(this);
    }

    private void flashViewPager(){
        // TODO
        if(mRadioGroup.getCheckedRadioButtonId()==R.id.rb_huya){

        }else{

        }
    }

    private void findView() {
        mViewPager= (ViewPager) findViewById(R.id.vp_viewPage);
        mSelectView= (SelectView) findViewById(R.id.sv_selectView);
        mRadioGroup= (RadioGroup) findViewById(R.id.rg_group);
    }

    @Override
    public void onSelectChange(SelectView view, boolean selected) {
        if(selected){
            mViewPager.setCurrentItem(1);
        }else{
            mViewPager.setCurrentItem(0);
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        mSelectView.setPosition(positionOffset+position);
        mSelectView.invalidate();
    }

    @Override
    public void onPageSelected(int position) {
        if(position==0){
            mSelectView.setSelected(false);
        }else{
            mSelectView.setSelected(true);
        }
        // TODO
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }
}
