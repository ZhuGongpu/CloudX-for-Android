package cloudx.views;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import cloudx.main.R;
import cloudx.views.fragments.DeviceListFragment;
import cloudx.views.fragments.FileListFragment;
import cloudx.views.fragments.SettingsFragment;

import java.util.ArrayList;

public class MainActivity extends FragmentActivity {

    private static final String TAG = "MainActivity";

    private ViewPager viewPager = null;

    private ArrayList<android.support.v4.app.Fragment> fragments = new ArrayList<android.support.v4.app.Fragment>(3);
    private ArrayList<Integer> bottomBarItems = new ArrayList<Integer>(3);

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        init();
    }

    /**
     * 初始化
     */
    private void init() {
        fragments.add(new DeviceListFragment());
        fragments.add(new FileListFragment());
        fragments.add(new SettingsFragment());

        bottomBarItems.add(R.id.bottom_bar_devices);
        bottomBarItems.add(R.id.bottom_bar_files);
        bottomBarItems.add(R.id.bottom_bar_settings);

        //view pager
        viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int i) {
                return fragments.get(i);
            }

            @Override
            public int getCount() {
                return fragments.size();
            }
        });
        viewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                setBottomBarSelected(position);

                Log.e(TAG, "Selected at : " + position);
            }
        });
        //设置第一个被选中
        setBottomBarSelected(0);

    }

    private void setBottomBarSelected(int index) {
        for (Integer layoutID : bottomBarItems)
            findViewById(layoutID).setSelected(false);

        findViewById(bottomBarItems.get(index)).setSelected(true);
        viewPager.setCurrentItem(index);
    }

    /**
     * On Click Listener for Bottom Bar
     *
     * @param view
     */
    public void setBottomBarSelected(View view) {
        setBottomBarSelected(bottomBarItems.indexOf(view.getId()));
    }
}
