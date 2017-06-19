package com.tomato.xatraffic;

import android.content.Context;
import android.database.DataSetObserver;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * Created by renwei on 2017/1/17.
 */

public class MyExpandableListAdapter extends BaseExpandableListAdapter {
    private List<String> groupList;
    private Map<String, BusData> dataMap;
    private Context mContext;
    private Handler exHandler;
    private IRefreshCallBack icallBack = null;
    String TAG = "MyExpandableListAdapter";

    public MyExpandableListAdapter(Context context, List<String> groupArray, Map<String, BusData> childMap) {
        mContext = context;
        this.groupList = groupArray;
        this.dataMap = childMap;
        exHandler = new Handler() {

            @Override
            public void handleMessage(Message msg) {
                notifyDataSetChanged();
                super.handleMessage(msg);
            }
        };
    }

    public void refresh() {
        exHandler.sendMessage(new Message());
    }

    public interface IRefreshCallBack {
        void refreshBusData(String s);
    }

    // set方法
    public void setRefreshCallBack(IRefreshCallBack iBack) {
        this.icallBack = iBack;
    }

    // get方法
    public IRefreshCallBack getRefreshCallBack() {
        return icallBack;
    }

    @Override
    public int getGroupCount() {
        return dataMap.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return 1;
    }

    @Override
    public Object getGroup(int groupPosition) {
        return dataMap.get(groupList.get(groupPosition));
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        BusData bd = dataMap.get(groupList.get(groupPosition));
        if (bd.isInUp()) {
            return bd.getUp();
        }
        return bd.getDown();

    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        GroupHolder holder = null;
        List<BusStation> bs = null;

        View view = convertView;
        if (view == null) {
            holder = new GroupHolder();
            view = LayoutInflater.from(mContext).inflate(R.layout.expand_parent, null);
            holder.busNameView = (TextView) view.findViewById(R.id.busName);
            holder.busTimeView = (TextView) view.findViewById(R.id.busTime);
            holder.startStationView = (TextView) view.findViewById(R.id.startStation);
            holder.switchView = (ImageView) view.findViewById(R.id.switchStation);
            holder.refreshView = (ImageView) view.findViewById(R.id.refreshStation);
            view.setTag(holder);
        } else {
            holder = (GroupHolder) view.getTag();
        }

        //view 长按
        /*view.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener()
        {
            public boolean onItemLongClick(AdapterView<?> parent, View childView, int flatPos, long id)
            {
                if (ExpandableListView.getPackedPositionType(id) == ExpandableListView.PACKED_POSITION_TYPE_CHILD)
                {
                    long packedPos = ((ExpandableListView) parent).getExpandableListPosition(flatPos);
                    int groupPosition = ExpandableListView.getPackedPositionGroup(packedPos);
                    int childPosition = ExpandableListView.getPackedPositionChild(packedPos);

                    showDeleteAlertDialog((AccountInfo) expAdapter.getChild(groupPosition, childPosition));
                    return true;
                }

                return false;
            }

        });*/

        final BusData bd = (BusData) getGroup(groupPosition);
        holder.busNameView.setText(bd.getBusLine().getROUTENAME());

        // 切换按钮
        holder.switchView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bd.isInUp()) {
                    bd.setInUp(false);
                } else {
                    bd.setInUp(true);
                }
                refresh();
            }
        });

        // 刷新按钮
        holder.refreshView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                icallBack.refreshBusData(bd.getBusLine().getROUTENAME());
            }
        });

        if (bd.isInUp()) {
            bs = bd.getUp();
        } else {
            bs = bd.getDown();
        }
        String start = bs.get(0).getStation_name();
        String end = bs.get(bs.size() - 1).getStation_name();
        if (start.length() + end.length() > 12) {
            if (start.length() > 7) {
                start = start.substring(0, 7);
                if (end.length() > 3) {
                    end = end.substring(0, 3);
                    holder.startStationView.setText(start + "...-->" + end + "...");
                } else {
                    holder.startStationView.setText(start + "...-->" + end);
                }
            } else {
                end = end.substring(0, 12 - start.length() - 1);
                holder.startStationView.setText(start + "-->" + end + "...");
            }
        } else {
            holder.startStationView.setText(start + "-->" + end);
        }

        holder.busTimeView.setText("发车间隔：" + bd.getAvg_interval() + "分钟");

        return view;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        ChildHolder holder = null;

        View view = convertView;
        if (view == null) {
            holder = new ChildHolder();
            view = LayoutInflater.from(mContext).inflate(R.layout.expand_child, null);
            //holder.busStation = (LinearLayout) view.findViewById(R.id.busStation);
            holder.childView = (WebView)view.findViewById(R.id.childWebView);
            view.setTag(holder);
        } else {
            holder = (ChildHolder) view.getTag();
            //holder.busStation.removeAllViewsInLayout();
        }
        BusData bd1 = (BusData) getGroup(groupPosition);
        String url="http://www.xajtfb.cn/www/dist/index.html?showFav=0&hideFooter=1&src=webapp_xiantraffic&utm_source=webapp_xiantraffic&utm_medium=entrance&cityId=076&cityName=%20%20%20%20%E5%AE%89&homePage=around&supportSubway=1&switchCity=0&lng=&lat=&src=webapp_xiantraffic#!/linedetail/2956913896/0/0760079/600";
        holder.childView.setWebChromeClient(new WebChromeClient());
        WebSettings settings = holder.childView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        holder.childView.setWebViewClient(new WebViewClient(){
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // TODO Auto-generated method stub
                //返回值是true的时候控制去WebView打开，为false调用系统浏览器或第三方浏览器
                view.loadUrl(url);
                return true;
            }
        });
        holder.childView.loadUrl(url);
        /*List<BusStation> bs = (List<BusStation>) getChild(groupPosition, childPosition);
        //Log.d(TAG, "getChildView: "+bs.get(i).getStation_name());
        for (int i = 0; i < bs.size(); i++) {
            View sView = LayoutInflater.from(mContext).inflate(R.layout.expand_station, null);
            if (bs.get(i).getBus_count() != 0) {
                TextView num = (TextView) sView.findViewById(R.id.busNumber);
                num.setText("" + bs.get(i).getBus_count());
                ImageView img = (ImageView) sView.findViewById(R.id.busImage);
                img.setImageResource(R.drawable.bus);
            }
            final TextView txt = (TextView) sView.findViewById(R.id.stationName);
            String d = bs.get(i).getStation_name();
            if (bs.get(i).getStation_name() == null) {
                BusData bd = (BusData) getGroup(groupPosition);
                if (bd.isInUp()) {
                    d = bd.getDown().get(bs.size() - i - 1).getStation_name();
                } else {
                    d = bd.getUp().get(bs.size() - i - 1).getStation_name();
                }
            }
            String s = handleBracketInStationName(d);
            txt.setText(s);
            holder.busStation.addView(sView);
            sView.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    txt.setBackgroundColor(v.getResources().getColor(R.color.colorAccent));
                }
            });
        }*/

        return view;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public void onGroupExpanded(int groupPosition) {
        for (int i = 0; i < groupList.size(); i++) {
            if (i != groupPosition) {
                Log.d(TAG, "onGroupExpanded: close " + i);
                onGroupCollapsed(i);
            }
        }
    }

    @Override
    public void onGroupCollapsed(int groupPosition) {
        Log.d(TAG, "onGroupCollapsed: close " + groupPosition);
    }

    @Override
    public long getCombinedChildId(long groupId, long childId) {
        return 0;
    }

    @Override
    public long getCombinedGroupId(long groupId) {
        return 0;
    }

    class GroupHolder {
        public TextView busNameView;
        public TextView busTimeView;
        public TextView startStationView;
        public ImageView switchView;
        public ImageView refreshView;
    }

    class ChildHolder {
        public LinearLayout busStation;
        public WebView childView;
    }

    private String handleBracketInStationName(String name) {
        String s = name;
        if (name.contains("（")) {
            String tmp = name.replace("（", "︵");
            s = tmp.replace("）", "︶");
        } else if (name.contains("(")) {
            String tmp = name.replace("(", "︵");
            s = tmp.replace(")", "︶");
        }

        return s;
    }
}

