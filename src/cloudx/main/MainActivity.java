package cloudx.main;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.*;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import cloudx.display_file_list.FileManagerActivity;
import cloudx.remote_desktop.RemoteDesktopActivity;
import cloudx.view.findMyDevice.FindMyDeviceAlertDialog;
import cloudx.view.menu.ResideMenu;
import cloudx.view.menu.ResideMenuItem;
import cloudx.view.messagebox.MessageBox;
import cloudx.view.messagebox.MessageSender;
import com.google.protobuf.ByteString;
import common.message.Data;
import data.information.GlobalSettingsAndInformation;
import model.network.ListeningThread;

import java.io.IOException;
import java.lang.Process;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Gongpu on 14-3-22.
 */
public class MainActivity extends Activity {

    private static final int logoWaitTime = 1000;
    private static final int RemoteControllMode = 1;
    private static final int FileManagerMode = 2;
    private static final int SendMessageMode = 3;
    private static boolean isFirstLoad = true;
    private int selectedMode = 0;
    private String TAG = "MainActivity";
    private ResideMenu resideMenu = null;
    private PopupWindow popupWindow = null;
    private ListView listView = null;
    private ProgressBar progressBar = null;
    private EditText ipEditText = null;
    private SimpleAdapter simpleAdapter = null;
    private ArrayList<HashMap<String, String>> ipList = new ArrayList<HashMap<String, String>>();
    private Handler handler = new Handler() {
        int counter = 0;//用于标记有几个task已完成

        @Override
        public void handleMessage(Message msg) {
//TODO marked at 2014/11/6
            if (msg.arg1 == GlobalSettingsAndInformation.MessageType_FindMyDevice) {
//find my device
                new FindMyDeviceAlertDialog(MainActivity.this);
            } else if (msg.arg1 == GlobalSettingsAndInformation.MessageType_FileInfo) {

            } else if (msg.arg1 == GlobalSettingsAndInformation.MessageType_Message) {

            }


            if (msg.obj != null && msg.obj instanceof HashMap) {
                ipList.add((HashMap<String, String>) msg.obj);
                simpleAdapter.notifyDataSetChanged();
            }
            if (msg.arg1 == 1) {
                counter++;
                if (counter >= 2) {
                    progressBar.setVisibility(View.GONE);
                }
            }
        }
    };
    private WiFiScannerTask wiFiScanner1 = null;
    private WiFiScannerTask wiFiScanner2 = null;

    private Socket socket = null;//用于send message

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_view);
        initViews();

//        Log.e(TAG, Build.BRAND + " " + Build.MODEL + " " + Build.ID + " " + getStringIP());

//        Log.e(TAG, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RINGTONES).getAbsolutePath());


        GlobalSettingsAndInformation.deviceName = Build.BRAND + " " + Build.MODEL;
        GlobalSettingsAndInformation.localIP = getStringIP();

        //start the listening thread
        if (ListeningThread.getInstance().getState() != Thread.State.RUNNABLE)
            ListeningThread.getInstance().start();

        ListeningThread.getInstance().setHandler(handler);
    }

    /**
     * 调用文件选择软件来选择文件 *
     */
    private void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "请选择一个要上传的文件"),
                    1011);
        } catch (android.content.ActivityNotFoundException ex) {
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(this, "请安装文件管理器", Toast.LENGTH_SHORT)
                    .show();
        }
    }

    /**
     * 根据返回选择的文件，来进行上传操作 *
     */
//    @Override
//    public void onActivityResult(int requestCode, int resultCode, Intent data) {

//        if (resultCode == Activity.RESULT_OK) {
//            // Get the Uri of the selected file
//            Uri uri = data.getData();
//            String url;
//            try {
//                url = FFileUtils.getPath(getActivity(), uri);
//                Log.i("ht", "url" + url);
//                String fileName = url.substring(url.lastIndexOf("/") + 1);
//                intent = new Intent(getActivity(), UploadServices.class);
//                intent.putExtra("fileName", fileName);
//                intent.putExtra("url", url);
//                intent.putExtra("type ", "");
//                intent.putExtra("fuid", "");
//                intent.putExtra("type", "");
//
//                getActivity().startService(intent);
//
//            } catch (URISyntaxException e) {

//                e.printStackTrace();
//            }
//        }
//        super.onActivityResult(requestCode, resultCode, data);
//    }
    private void initViews() {

        resideMenu = new ResideMenu(this);
        resideMenu.setBackground(R.drawable.menu_background);
        resideMenu.attachToActivity(this);

        final ResideMenuItem remoteControll = new ResideMenuItem(this);
        remoteControll.setIcon(R.drawable.remotedesktop);
        remoteControll.setTitle(R.string.RemoteDesktop);
        remoteControll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedMode = RemoteControllMode;
                if (GlobalSettingsAndInformation.ServerIP == null) {
                    if (!popupWindow.isShowing())
                        showPopupWindow(remoteControll);
                } else {
                    //jump to remote desktop mode
                    jumpToSelectedMode();
                }
            }
        });
        resideMenu.addMenuItem(remoteControll);

        //文件共享
        final ResideMenuItem fileManagerItem = new ResideMenuItem(this, R.drawable.share, R.string.FileExplorer);
        fileManagerItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedMode = FileManagerMode;
                if (GlobalSettingsAndInformation.ServerIP == null) {
                    if (!popupWindow.isShowing())
                        showPopupWindow(fileManagerItem);
                } else {
                    //jump to movie mode
                    jumpToSelectedMode();
                }
            }
        });
        resideMenu.addMenuItem(fileManagerItem);

        //发送消息
        ResideMenuItem sendMessageItem = new ResideMenuItem(this, R.drawable.message, R.string.SendMessage);
        sendMessageItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedMode = SendMessageMode;
                if (GlobalSettingsAndInformation.ServerIP == null) {
                    if (!popupWindow.isShowing())
                        showPopupWindow(fileManagerItem);
                } else {
                    jumpToSelectedMode();
                }

            }
        });
        resideMenu.addMenuItem(sendMessageItem);


        final ResideMenuItem exit = new ResideMenuItem(this);
        exit.setTitle(R.string.exit);
        exit.setIcon(R.drawable.exit);
        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exit();
            }
        });
        resideMenu.addMenuItem(exit);

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    if (isFirstLoad)
                        Thread.sleep(logoWaitTime);
                    isFirstLoad = false;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                isFirstLoad = false;
                resideMenu.openMenu();
            }
        }.execute();

        //init popupWindow
        View contentView = LayoutInflater.from(this).inflate(R.layout.ip_list, null);

        popupWindow = new PopupWindow(
                contentView,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                true);

        popupWindow.setOutsideTouchable(true);
        popupWindow.setFocusable(true);
        popupWindow.getContentView().setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //popupWindow.setFocusable(false);
                if (wiFiScanner1 != null)
                    wiFiScanner1.cancel(true);
                if (wiFiScanner2 != null)
                    wiFiScanner2.cancel(true);
                popupWindow.dismiss();
                return false;
            }
        });

        listView = (ListView) contentView.findViewById(R.id.ipListView);
        listView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK && popupWindow.isShowing()) {
                    if (wiFiScanner1 != null)
                        wiFiScanner1.cancel(true);
                    if (wiFiScanner2 != null)
                        wiFiScanner2.cancel(true);
                    popupWindow.dismiss();
                }

                //todo bug
                return true;
            }
        });

        ipEditText = (EditText) contentView.findViewById(R.id.ipEditText);

        ipEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ipEditText.setText(getLocalHostPrefix());
                if (ipEditText.getText() != null)
                    ipEditText.setSelection(ipEditText.getText().toString().length());
            }
        });

        ipEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO && ipEditText.getText() != null) {

                    String ip = ipEditText.getText().toString();

                    if (isIllegalIP(ip)) {
                        GlobalSettingsAndInformation.ServerIP = ip;

                        if (wiFiScanner1 != null)
                            wiFiScanner1.cancel(true);
                        if (wiFiScanner2 != null)
                            wiFiScanner2.cancel(true);
                        popupWindow.dismiss();

                        jumpToSelectedMode();

                    } else
                        Toast.makeText(MainActivity.this, "IP地址不合法", Toast.LENGTH_LONG).show();
                    return true;
                }
                return false;
            }
        });

        ImageButton confirmButton = (ImageButton) contentView.findViewById(R.id.confirmIP);
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String ip = ipEditText.getText().toString();

                if (isIllegalIP(ip)) {
                    GlobalSettingsAndInformation.ServerIP = ip;

                    if (wiFiScanner1 != null)
                        wiFiScanner1.cancel(true);
                    if (wiFiScanner2 != null)
                        wiFiScanner2.cancel(true);
                    popupWindow.dismiss();

                    jumpToSelectedMode();

                } else
                    Toast.makeText(MainActivity.this, "IP地址不合法", Toast.LENGTH_LONG).show();

            }
        });

        progressBar = (ProgressBar) contentView.findViewById(R.id.progressBar);

        simpleAdapter = new SimpleAdapter(this, ipList, R.layout.ip_list_item, new String[]{"IP"},
                new int[]{R.id.ipTextView});
        listView.setAdapter(simpleAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.e(TAG, ((TextView) view.findViewById(R.id.ipTextView)).getText() + " clicked");

                GlobalSettingsAndInformation.ServerIP = ((TextView) view.findViewById(R.id.ipTextView)).getText().toString();
                if (wiFiScanner1 != null)
                    wiFiScanner1.cancel(true);
                if (wiFiScanner2 != null)
                    wiFiScanner2.cancel(true);
                popupWindow.dismiss();

                jumpToSelectedMode();

            }
        });
    }

    private boolean isIllegalIP(String ip) {
        String ipPrefix = getLocalHostPrefix();

        if (ip.length() > ipPrefix.length() && ip.length() - ipPrefix.length() <= 3) {
            if (ip.substring(0, ipPrefix.length()).equals(ipPrefix)) {
                int suffix = Integer.parseInt(ip.substring(ipPrefix.length()));
                return suffix >= 0 && suffix <= 255;
            } else
                return false;
        } else
            return false;
    }

    private void jumpToSelectedMode() {

        if (wiFiScanner1 != null)
            wiFiScanner1.cancel(true);
        if (wiFiScanner2 != null)
            wiFiScanner2.cancel(true);

        switch (selectedMode) {
            case FileManagerMode:
                startActivity(new Intent(this, FileManagerActivity.class));
                cleanUpAndFinish();
                break;
            case RemoteControllMode:
                startActivity(new Intent(this, RemoteDesktopActivity.class));
                cleanUpAndFinish();
                break;
            case SendMessageMode:
                new MessageBox(this, new MessageSender() {
                    @Override
                    public void sendMessage(final String message) throws IOException {

                        new AsyncTask<Void, Void, Void>() {

                            @Override
                            protected Void doInBackground(Void... params) {
                                if (socket == null) {
                                    try {
                                        socket = new Socket(GlobalSettingsAndInformation.ServerIP,
                                                GlobalSettingsAndInformation.ServerPort);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }

                                if (!socket.isClosed())
                                    try {

                                        sendInfo(GlobalSettingsAndInformation.deviceName, Data.Info.InfoType.NormalInfo,
                                                MainActivity.this.getResources().getDisplayMetrics().widthPixels,
                                                MainActivity.this.getResources().getDisplayMetrics().heightPixels);

                                        Data.DataPacket.newBuilder()
                                                .setDataPacketType(Data.DataPacket.DataPacketType.SharedMessage)
                                                .setSharedMessage(
                                                        Data.SharedMessage.newBuilder()
                                                                .setContent(
                                                                        ByteString
                                                                                .copyFrom(message.getBytes())
                                                                ).build()
                                                ).build()
                                                .writeDelimitedTo(socket.getOutputStream());
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                return null;
                            }
                        }.execute();

                    }
                }).show();
                break;
            default:
                break;
        }
    }


    private void showPopupWindow(View view) {
        if (wiFiScanner1 != null)
            wiFiScanner1.cancel(true);
        if (wiFiScanner2 != null)
            wiFiScanner2.cancel(true);

        ipList.clear();
        simpleAdapter.notifyDataSetChanged();

        wiFiScanner1 = new WiFiScannerTask();
        wiFiScanner1.execute(getLocalHostSuffix() + 1);
        wiFiScanner2 = new WiFiScannerTask();
        wiFiScanner2.execute(getLocalHostSuffix() - 1);

        progressBar.setVisibility(View.VISIBLE);
        popupWindow.showAsDropDown(view);
    }

    private void exit() {

        //close the file input thread
        ListeningThread.getInstance().interrupt();
        isFirstLoad = true;
        GlobalSettingsAndInformation.ServerIP = null;

        cleanUpAndFinish();
        Log.e(TAG, "EXIT MAIN ACTIVITY");
    }

    private void cleanUpAndFinish() {
        if (wiFiScanner1 != null)
            wiFiScanner1.cancel(true);
        if (wiFiScanner2 != null)
            wiFiScanner2.cancel(true);
        if (popupWindow != null && popupWindow.isShowing())
            popupWindow.dismiss();

        try {
            if (socket != null && !socket.isClosed())
                socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        ListeningThread.getInstance().setHandler(null);

        finish();
    }

    private int getLocalHostSuffix() {
        String ip = getStringIP();
        return Integer.parseInt(ip.substring(ip.lastIndexOf('.') + 1));
    }

    private String getLocalHostPrefix() {
        String ip = getStringIP();
        return ip.substring(0, ip.lastIndexOf('.') + 1);
    }

    private String getStringIP() {
//把整型地址转换成“*.*.*.*”地址
        return intToIp(getIntIP());
    }

    private int getIntIP() {
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        //检查Wifi状态
        if (!wifiManager.isWifiEnabled())
            wifiManager.setWifiEnabled(true);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        //获取32位整型IP地址
        return wifiInfo.getIpAddress();
    }

    private String intToIp(int i) {
        return (i & 0xFF) + "." +
                ((i >> 8) & 0xFF) + "." +
                ((i >> 16) & 0xFF) + "." +
                (i >> 24 & 0xFF);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.e(TAG, "onKeyDown : " + keyCode);

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            exit();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void sendInfo(String deviceName, Data.Info.InfoType type, int width, int height) throws IOException {
        if (socket != null)
            Data.DataPacket.newBuilder().setDataPacketType(Data.DataPacket.DataPacketType.Info)
                    .setInfo(
                            Data.Info.newBuilder()
                                    .setInfoType(type)
                                    .setDeviceName(ByteString.copyFromUtf8(deviceName))
                                    .setHeight(height)
                                    .setWidth(width)
                                    .setPortAvailable(0)
                                    .build()
                    ).build().writeDelimitedTo(socket.getOutputStream());
    }

    private class WiFiScannerTask extends AsyncTask<Integer, Void, Void> {
        private final String pingCMD = "ping -c 1 -w 0.1 ";

        private boolean ping(String ip) throws IOException, InterruptedException {
            Process process = Runtime.getRuntime().exec(pingCMD + ip);
            return process.waitFor() == 0;
        }


        @Override
        protected Void doInBackground(Integer... params) {
            Log.e(TAG, "doInBackground");
            int localHostSuffix = getLocalHostSuffix();
            String localHostPrefix = getLocalHostPrefix();
            int MaxFailedCount = 10;
            try {
                int failedCount = 0;
                int index = params[0];
                if (index < localHostSuffix)
                    while (index > 0 && failedCount < MaxFailedCount) {
                        String ip = localHostPrefix + index;
                        if (ping(ip)) {
                            Log.e(TAG, ip
                                    + " is reachable");
                            Message message = handler.obtainMessage();
                            HashMap<String, String> hashMap = new HashMap<String, String>(1);
                            hashMap.put("IP", ip);
                            message.obj = hashMap;
                            message.sendToTarget();

                            failedCount = 0;
                        } else {
                            Log.e(TAG, ip + " is unreachable");
                            failedCount++;
                        }
                        index--;
                    }
                else
                    while (index < 255 && failedCount < MaxFailedCount) {
                        String ip = localHostPrefix + index;
                        if (ping(ip)) {
                            Log.e(TAG, ip
                                    + " is reachable");
                            Message message = handler.obtainMessage();
                            HashMap<String, String> hashMap = new HashMap<String, String>(1);
                            hashMap.put("IP", ip);
                            message.obj = hashMap;
                            message.sendToTarget();

                            failedCount = 0;
                        } else {
                            Log.e(TAG, ip + " is unreachable");
                            failedCount++;
                        }
                        index++;
                    }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Message message = handler.obtainMessage();
            message.arg1 = 1;
            message.sendToTarget();

            return null;
        }
    }
}
