package com.hitsz.autotrack;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity{

    BluetoothFragment bluetoothFragment;
    DetecterActivity detecterActivity;
    private BlueToothStateReceiver mReceiver;//广播接收器

    Handler uiHandler =new Handler();

    public Handler getUiHandler() {
        return uiHandler;
    }

    public Handler getbHandler(){
        return bluetoothFragment.getmHandler();
    }

    TabLayout tabLayout;
    ViewPager viewPager;
    MyPagerAdapter pagerAdapter;
    String[] titleList=new String[]{"蓝牙连接","自追踪拍摄"};
    List<Fragment> fragmentList=new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initUI();
        registerRec();
    }

    /**
     * 初始化界面
     */
    private void initUI() {
        tabLayout= (TabLayout) findViewById(R.id.tab_layout);
        viewPager= (ViewPager) findViewById(R.id.view_pager);

        tabLayout.addTab(tabLayout.newTab().setText(titleList[0]));
        tabLayout.addTab(tabLayout.newTab().setText(titleList[1]));

        bluetoothFragment=new BluetoothFragment();
        detecterActivity = new DetecterActivity();
        fragmentList.add(bluetoothFragment);
        fragmentList.add(detecterActivity);

        pagerAdapter=new MyPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(pagerAdapter);
        tabLayout.setupWithViewPager(viewPager);
    }

    /**
     * ViewPager 适配器
     */
    public class MyPagerAdapter extends FragmentPagerAdapter {

        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return fragmentList.get(position);
        }

        @Override
        public int getCount() {
            return fragmentList.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return titleList[position];
        }
    }

    private void registerRec() {
        //3.注册蓝牙广播
        mReceiver = new BlueToothStateReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);//搜多到蓝牙
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);//搜索结束
        registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onDestroy() {
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
        }
        super.onDestroy();
    }

    class BlueToothStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case BluetoothDevice.ACTION_FOUND:
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (bluetoothFragment.listView_device != null) {
                        bluetoothFragment.addDevice(device);
                    }
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    bluetoothFragment.addMessage("搜索结束");
                    break;
            }
        }
    }
}
