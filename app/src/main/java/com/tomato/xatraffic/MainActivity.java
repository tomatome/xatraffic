package com.tomato.xatraffic;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by renwei on 2017/1/13.
 */

public class MainActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {
    private static final int MSG_QUERY_BUS_LIST = 0;//查询 主--> 子
    private static final int MSG_QUERY_REAL_BUS = 1; //查询 主--> 子
    private static final int MSG_SUCCESS_BUS_LIST = 2;// 成功 子-->主
    private static final int MSG_SUCCESS_REAL_BUS = 3;// 成功 子-->主
    private static final int MSG_FAILURE = 4;// 失败 子-->主

    private boolean isNetworkOk = true;
    private TextView networkView;
    private SearchView searchView;
    private ListView busListView;
    private ArrayAdapter searchAdapter;
    private List<String> queryList;
    private Map<String, BusLine> busLineMap;

    private ExpandableListView mExpandView;
    private Handler mDataHandler;
    private HandlerThread mDataThread;
    private DataCenter dc;
    private ArrayList<String> groupList;
    private Map<String, BusData> dataMap;
    private MyExpandableListAdapter expandAdapter;

    private NetWorkStateReceiver receiver;
    private AutoRefresh refresh;

    private class AutoRefresh {
        private TextView numberView;
        private int number;
        private Handler handler;
        private boolean stop;

        private AutoRefresh() {
            this.number = 11;
            this.handler = new Handler();
            this.stop = false;
        }
    }

    private Handler mMainHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_SUCCESS_BUS_LIST:
                    BusLine[] bl = (BusLine[]) msg.obj;
                    for (int i = 0; i < bl.length; i++) {
                        queryList.add(bl[i].getROUTENAME());
                        busLineMap.put(bl[i].getROUTENAME(), bl[i]);
                    }
                    searchAdapter.notifyDataSetChanged();
                    break;
                case MSG_SUCCESS_REAL_BUS:
                    BusData bd = (BusData) msg.obj;
                    if (!bd.isHas_real_bus()) {
                        Toast.makeText(getApplication(), "该车辆没有实时信息", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getApplication(), "查询成功", Toast.LENGTH_SHORT).show();
                    }
                    BusData bd_old = dataMap.get(bd.getBusLine().getROUTENAME());
                    if (bd_old != null) {
                        bd.setInUp(bd_old.isInUp());
                        dataMap.remove(bd.getBusLine().getROUTENAME());
                    } else {
                        groupList.clear();
                        dataMap.clear();
                        groupList.add(bd.getBusLine().getROUTENAME());
                    }
                    dataMap.put(bd.getBusLine().getROUTENAME(), bd);
                    expandAdapter.refresh();
                    break;
                case MSG_FAILURE:
                    Toast.makeText(getApplication(), (String) msg.obj, Toast.LENGTH_LONG).show();
                    break;

            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        dc = new DataCenter();
        startBackDataThread();

        //网络状态
        networkView = (TextView) findViewById(R.id.netView);

        //实时搜索框
        busListView = (ListView) findViewById(R.id.query_suggestion);
        searchView = (SearchView) findViewById(R.id.searchView);
        searchView.setSubmitButtonEnabled(true);
        searchView.setImeOptions(EditorInfo.IME_ACTION_DONE);
        searchView.setOnQueryTextListener(this);
        queryList = new ArrayList();
        busLineMap = new HashMap<String, BusLine>();
        searchAdapter = new ArrayAdapter(this, android.R.layout.simple_expandable_list_item_1, queryList);
        busListView.setAdapter(searchAdapter);
        busListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                // When clicked, show a toast with the TextView text
                String s = (String) searchAdapter.getItem(position);
                BusLine bl = busLineMap.get(s);
                mDataHandler.obtainMessage(MSG_QUERY_REAL_BUS, bl).sendToTarget();
                searchView.setQuery(bl.getROUTENAME(), false);
                searchView.clearFocus(); // 不获取焦点
                mExpandView.setVisibility(View.VISIBLE);
                busListView.setVisibility(View.GONE);
            }
        });
        busListView.setVisibility(View.GONE);


        // 公交实时信息展示
        groupList = new ArrayList<String>();
        dataMap = new HashMap<String, BusData>();
        mExpandView = (ExpandableListView) findViewById(R.id.expand_list);
        expandAdapter = new MyExpandableListAdapter(this, groupList, dataMap);
        expandAdapter.setRefreshCallBack(new MyExpandableListAdapter.IRefreshCallBack() {

            @Override
            public void refreshBusData(String busName) {
                BusData bd = dataMap.get(busName);
                mDataHandler.obtainMessage(MSG_QUERY_REAL_BUS, bd.getBusLine()).sendToTarget();
            }
        });
        mExpandView.setAdapter(expandAdapter);
        mExpandView.setGroupIndicator(null);

        //网络监听
        receiver = new NetWorkStateReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(receiver, filter);
        //获取坐标
        //receiver.initNearByBusLine();
        //自动更新
        refresh = new AutoRefresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("debug", "onResume: in");
        refresh.handler.postDelayed(freshRunnable, 1000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("debug", "onPause: in");
        refresh.handler.removeCallbacks(freshRunnable);
    }

    //查询数据线程
    private void startBackDataThread() {
        mDataThread = new HandlerThread("");
        mDataThread.start();
        mDataHandler = new Handler(mDataThread.getLooper()) {
            public void handleMessage(Message msg) {//3、定义处理消息的方法
                switch (msg.what) {
                    case MSG_QUERY_BUS_LIST:
                        String routeId = (String) msg.obj;
                        BusLine[] tBlist = dc.getBusLine(routeId);
                        if (tBlist != null) {
                            mMainHandler.obtainMessage(MSG_SUCCESS_BUS_LIST, tBlist).sendToTarget();
                        } else {
                            mMainHandler.obtainMessage(MSG_FAILURE, "获取车辆信息失败.").sendToTarget();
                        }
                        break;
                    case MSG_QUERY_REAL_BUS:
                        BusLine tBl = (BusLine) msg.obj;
                        BusData tBd = dc.getBusData(tBl.getROUTEID());
                        if (tBd != null) {
                            tBd.setBusLine(tBl);
                            mMainHandler.obtainMessage(MSG_SUCCESS_REAL_BUS, tBd).sendToTarget();
                        } else {
                            mMainHandler.obtainMessage(MSG_FAILURE, "获取车辆详细信息失败.").sendToTarget();
                        }
                }
            }
        };
        mDataHandler.obtainMessage(MSG_QUERY_BUS_LIST, "").sendToTarget();
    }

    Runnable freshRunnable = new Runnable() {
        @Override
        public void run() {
            if (dataMap.size() > 0) {
                if (refresh.number == 0) {
                    for (BusData bd : dataMap.values()) {
                        if (!bd.isHas_real_bus())
                            continue;
                        BusLine bl = bd.getBusLine();
                        mDataHandler.obtainMessage(MSG_QUERY_REAL_BUS, bl).sendToTarget();
                    }
                    refresh.number = 11;
                } else {
                    refresh.number = refresh.number - 1;
                    refresh.numberView.setText("" + refresh.number);
                }
                refresh.numberView.setVisibility(View.VISIBLE);
            }

            /*if (receiver.location == null) {
                receiver.updateLocation();
            }*/
            refresh.handler.postDelayed(this, 1000);
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        final MenuItem item = menu.findItem(R.id.autoNumber);
        refresh.numberView = (TextView) item.getActionView();
        refresh.numberView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOptionsItemSelected(item);
            }
        });
        refresh.numberView.setVisibility(View.INVISIBLE);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.autoNumber:
                if (refresh.stop) {
                    Toast.makeText(getApplicationContext(), "开始自动刷新...", Toast.LENGTH_SHORT).show();
                    refresh.handler.postDelayed(freshRunnable, 1000);
                    refresh.stop = false;
                } else {
                    Toast.makeText(getApplicationContext(), "暂停自动刷新...", Toast.LENGTH_SHORT).show();
                    refresh.handler.removeCallbacks(freshRunnable);
                    refresh.stop = true;
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onQueryTextSubmit(String queryText) {
        // 得到输入管理对象
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            // 输入法如果是显示状态，那么就隐藏输入法
            imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);
        }
        searchView.clearFocus(); // 不获取焦点
        if (isNetworkOk) {
            BusLine bl = null;
            for (int i = 0; i < queryList.size(); i++) {
                if (queryList.get(i).startsWith(queryText)) {
                    bl = busLineMap.get(queryList.get(i));
                    break;
                }
            }
            if (bl == null) {
                Toast.makeText(getApplicationContext(), "没有该车辆信息...", Toast.LENGTH_LONG).show();
                searchView.setFocusable(true);
                return true;
            }
            mDataHandler.obtainMessage(MSG_QUERY_REAL_BUS, bl).sendToTarget();
            searchView.setQuery(bl.getROUTENAME(), false);
            searchView.clearFocus(); // 不获取焦点
            mExpandView.setVisibility(View.VISIBLE);
            busListView.setVisibility(View.GONE);
        } else {
            Toast.makeText(getApplicationContext(), "当前网络不可用...", Toast.LENGTH_LONG).show();
        }

        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if (newText.length() == 0) {
            mExpandView.setVisibility(View.VISIBLE);
            busListView.setVisibility(View.GONE);
            return false;
        }

        if (mExpandView.isShown()) {
            mExpandView.setVisibility(View.GONE);
            busListView.setVisibility(View.VISIBLE);
        }
        searchAdapter.getFilter().filter(newText);

        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (receiver != null) {
            unregisterReceiver(receiver);
            receiver = null;
        }
        refresh.handler.removeCallbacks(freshRunnable);
        mDataThread.quit();

    }

    public class NetWorkStateReceiver extends BroadcastReceiver {
        private PackageManager pm;
        private LocationManager locationManager;
        private Location location;
        private String provider;

        @Override
        public void onReceive(Context context, Intent intent) {
            if (pm == null)
                pm = getPackageManager();
            boolean hasPermission = checkPermission("ACCESS_NETWORK_STATE");
            if (!hasPermission) {
                networkView.setText("没有网络权限...");
                networkView.setVisibility(View.VISIBLE);
                networkView.setBackgroundResource(R.color.colorRed);
                isNetworkOk = false;
            }
            ConnectivityManager con = (ConnectivityManager) context.getSystemService(Activity.CONNECTIVITY_SERVICE);
            boolean wifi = con.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnectedOrConnecting();
            boolean internet = con.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).isConnectedOrConnecting();
            if (wifi | internet) {
                networkView.setVisibility(View.GONE);
                isNetworkOk = true;
            } else {
                networkView.setText("当前网络不可用...");
                networkView.setVisibility(View.VISIBLE);
                isNetworkOk = false;
            }
        }

        public boolean initNearByBusLine() {
            if (pm == null)
                pm = getPackageManager();
            Log.d("debug", "initNearByBusLine");
            // 获取地理位置管理器
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            Criteria criteria = new Criteria();
            // 获得最好的定位效果
            criteria.setAccuracy(Criteria.ACCURACY_FINE);
            criteria.setAltitudeRequired(false);
            criteria.setBearingRequired(false);
            criteria.setCostAllowed(false);
            // 使用省电模式
            criteria.setPowerRequirement(Criteria.POWER_LOW);
            provider = locationManager.getBestProvider(criteria, true);
            boolean hasPermission = checkPermission("ACCESS_FINE_LOCATION");
            if (!hasPermission || locationManager == null) {
                Toast.makeText(getApplication(), "不能获取定位权限...", Toast.LENGTH_SHORT).show();
                return false;
            }
            // 获取Location
            updateLocation();

            // 不为空,显示地理位置经纬度
            networkView.setText(show(location));
            networkView.setVisibility(View.VISIBLE);
            // 监视地理位置变化
            locationManager.requestLocationUpdates(provider, 5000, 10,
                    new LocationListener() {
                        @Override
                        public void onStatusChanged(String provider, int status, Bundle arg2) {

                        }

                        @Override
                        public void onProviderEnabled(String provider) {

                        }

                        @Override
                        public void onProviderDisabled(String provider) {

                        }

                        @Override
                        public void onLocationChanged(Location location) {
                            //如果位置发生变化,重新显示
                            networkView.setText(show(location));
                            networkView.setVisibility(View.VISIBLE);

                        }
                    });
            return true;
        }

        private void updateLocation() {
            boolean hasPermission = (PackageManager.PERMISSION_GRANTED == pm.checkPermission("android.permission.ACCESS_FINE_LOCATION", "com.tomato.xatraffic"));
            if (!hasPermission || locationManager == null) {
                Toast.makeText(getApplication(), "不能获取定位权限...", Toast.LENGTH_SHORT).show();
                return;
            }
            location = locationManager.getLastKnownLocation(provider);
            show(location);
        }

        private String show(Location location) {
            if (location == null) {
                Toast.makeText(getApplication(), "不能获取定位信息...", Toast.LENGTH_SHORT).show();
                return "不能获取定位信息";
            }
            StringBuffer sb = new StringBuffer();
            sb.append("经度：" + location.getLongitude() + ", 纬度："
                    + location.getLatitude());
            Log.d("update", sb.toString());
            Toast.makeText(getApplication(), sb.toString(), Toast.LENGTH_SHORT).show();
            return sb.toString();
        }

        private boolean checkPermission(String permission) {
            return (PackageManager.PERMISSION_GRANTED == pm.checkPermission("android.permission." + permission, "com.tomato.xatraffic"));
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // 创建退出对话框
            AlertDialog isExit = new AlertDialog.Builder(this).create();
            // 设置对话框标题
            isExit.setTitle("系统提示");
            // 设置对话框消息
            isExit.setMessage("确定要退出吗");
            // 添加选择按钮并注册监听
            isExit.setButton("确定", listener);
            isExit.setButton2("取消", listener);
            // 显示对话框
            isExit.show();
            searchView.clearFocus();//取消焦点
            busListView.setVisibility(View.GONE);
            mExpandView.setVisibility(View.VISIBLE);
        }

        return false;

    }

    /**
     * 监听对话框里面的button点击事件
     */
    DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case AlertDialog.BUTTON_POSITIVE:// "确认"按钮退出程序
                    finish();
                    break;
                case AlertDialog.BUTTON_NEGATIVE:// "取消"第二个按钮取消对话框
                    break;
                default:
                    break;
            }
        }
    };
}
