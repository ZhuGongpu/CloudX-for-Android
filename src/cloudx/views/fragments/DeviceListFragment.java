package cloudx.views.fragments;


import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import cloudx.main.R;
import cloudx.views.RemoteDesktopActivity;
import com.squareup.picasso.Picasso;
import data.information.Constants;
import model.DeviceEntity;

import java.util.ArrayList;


/**
 * Created by zhugongpu on 14/11/17.
 */
public class DeviceListFragment extends Fragment {

    private static final String TAG = "DeviceListFragment";
    /**
     * 用于缓存整个fragment的view，防止onCreate时重复加载数据
     */
    private View fragmentView = null;

    private ListView deviceList = null;

    private ArrayList<DeviceEntity> data = new ArrayList<DeviceEntity>();
    private BaseAdapter adapter = null;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        if (fragmentView == null) {
            init(inflater, container);
            loadData();
        }
        //缓存的fragmentView需要判断是否已经被加过parent， 如果有parent需要从parent删除，要不然会发生这个fragmentView已经有parent的错误。
        ViewGroup parent = (ViewGroup) fragmentView.getParent();
        if (parent != null)
            parent.removeView(fragmentView);

        return fragmentView;

    }

    private void loadData() {
        for (int i = 0; i < 10; i++) {
            DeviceEntity entity = new DeviceEntity();
            entity.deviceName = "macbook pro";
            entity.deviceType = DeviceEntity.DeviceType.Desktop;
            entity.AccessPointName = "703";

            data.add(entity);
        }

        if (this.adapter != null)
            this.adapter.notifyDataSetChanged();

    }


    private void init(LayoutInflater inflater, ViewGroup container) {
        fragmentView = inflater.inflate(R.layout.device_list_fragment, container, false);

        deviceList = (ListView) fragmentView.findViewById(R.id.device_list);

        adapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return data.size();
            }

            @Override
            public Object getItem(int position) {
                return data.get(position);
            }

            @Override
            public long getItemId(int position) {
                return 0;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {

                Log.e(TAG, "getView : " + position);

                ViewHolder holder;
                if (convertView == null) {
                    convertView = View.inflate(getActivity(), R.layout.list_item, null);
                    holder = new ViewHolder();
                    holder.deviceIcon = (ImageView) convertView.findViewById(R.id.icon);
                    holder.deviceName = (TextView) convertView.findViewById(R.id.title);
                    holder.APName = (TextView) convertView.findViewById(R.id.body);
                    holder.remoteControl = (ImageView) convertView.findViewById(R.id.action);
                    convertView.setTag(holder);
                } else
                    holder = (ViewHolder) convertView.getTag();

                final DeviceEntity dataItem = data.get(position);

                //加载设备图标
                Picasso.with(getActivity()).load(getDeviceIconResourceID(dataItem.deviceType)).into(holder.deviceIcon);
                Picasso.with(getActivity()).load(R.drawable.remote_control).into(holder.remoteControl);

                //设置内容
                holder.deviceName.setText(dataItem.deviceName);
                holder.APName.setText(dataItem.AccessPointName);

                //设置listener
                holder.remoteControl.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //跳转到远程控制
                        Intent intent = new Intent(getActivity(), RemoteDesktopActivity.class);
                        //传入参数
                        intent.putExtra(Constants.ParaName_PeerIP, dataItem.ipAddress);
                        intent.putExtra(Constants.ParaName_PeerPort, dataItem.port);
                        intent.putExtra(Constants.ParaName_PeerPortAvailable, dataItem.portAvailable);
                        intent.putExtra(Constants.ParaName_Resolution_Width, dataItem.resolution.getWidth());
                        intent.putExtra(Constants.ParaName_Resolution_Height, dataItem.resolution.getHeight());
                        startActivity(intent);
                    }
                });

                convertView.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
                    @Override
                    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                        Log.e(TAG, "onCreateContextMenu");
                    }
                });

                return convertView;
            }

            class ViewHolder {
                ImageView deviceIcon = null;
                TextView deviceName = null;
                TextView APName = null;
                ImageView remoteControl = null;

            }
        };

        this.deviceList.setAdapter(this.adapter);

        this.deviceList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                DeviceListFragment.this.onItemSelected(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        //注册ContextMenu
        registerForContextMenu(deviceList);
    }

    private void onItemSelected(int position) {
        //TODO 跳转到会话界面
    }

    int getDeviceIconResourceID(DeviceEntity.DeviceType deviceType) {
        return R.drawable.desktop;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        menu.add(getText(R.string.Delete));
    }


}