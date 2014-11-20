package cloudx.display_file_list;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import cloudx.main.MainActivity;
import cloudx.main.R;
import cloudx.views.RemoteDesktopActivity;
import cloudx.view.CircleProgressBar.CircleProgressBar;
import cloudx.views.widgets.FindMyDeviceAlertDialog;
import cloudx.view.listview.ListViewCompat;
import cloudx.view.listview.MessageItem;
import cloudx.view.listview.SlideView;
import cloudx.view.messagebox.MessageBox;
import com.google.protobuf.ByteString;
import common.message.Data;
import data.information.FileInfo;
import data.information.Constants;
import cloudx.network.AudioInputThread;
import cloudx.network.ListeningThread;
import utils.OpenFileUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

//启动时需putExtra来限定当前模式
public class FileManagerActivity extends Activity implements OnItemClickListener, OnClickListener,
        SlideView.OnSlideListener {

    private static final String TAG = "FileManagerActivity";

    private CircleProgressBar circleProgressBar = null;
    private ListViewCompat listView = null;
    private List<MessageItem> messageItemArrayList = new ArrayList<MessageItem>();
    private SlideView lastSlideViewWithStatusOn = null;
    private ProgressBar progressBar = null;
    private Handler handler = new Handler()//用于在asyncTask中将数据库中item显示出来
    {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Log.e(TAG, "handle message");

            if (msg.arg1 == Constants.MessageType_FindMyDevice) {
                new FindMyDeviceAlertDialog(FileManagerActivity.this);
            } else if (msg.arg1 == Constants.MessageType_Message) {
                new MessageBox(FileManagerActivity.this, (String) msg.obj).show();
            } else if (msg.arg1 == Constants.MessageType_FileInfo) {
                //todo

                if (!circleProgressBar.isShown()) {
                    circleProgressBar.setVisibility(View.VISIBLE);
                }

                final FileInfo info = (FileInfo) msg.obj;

                // Log.e(TAG, info.totalReceivedSize + " / " + info.fileLength);

                if (info != null) {
                    if (info.totalReceivedSize >= info.fileLength) {

                        circleProgressBar.setVisibility(View.INVISIBLE);

                        new AlertDialog.Builder(FileManagerActivity.this).setTitle("文件已保存至内存卡，是否打开")
                                .setPositiveButton(R.string.Confirm, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        startActivity(OpenFileUtils.openFile(info.filePath));
                                    }
                                })
                                .setNegativeButton(R.string.Cancel, null)
                                .create().show();

                        Log.e(TAG, "File transmission done");

                    } else
                        circleProgressBar.setProgress((int)
                                (((double) info.totalReceivedSize / (double) info.fileLength) * 100));

                }
            } else if (msg.arg1 == -1) {
                progressBar.setVisibility(View.INVISIBLE);
            } else {
                Bundle bundle = msg.getData();

                MessageItem messageItem = new MessageItem();
                //todo set icon
                //TODO
                //TODO
                //TODO
                //TODO
                //TODO
                //TODO
                //messageItem.icon = bundle.get("icon");
                messageItem.path = bundle != null ? bundle.getString("filePath") : null;
                Log.e(TAG, "TODO add item");
                addItem(messageItem);
            }
        }
    };
    private SlideAdapter slideAdapter = null;

    private ImageButton addFileButton = null;//TODO 从本地文件管理器选择需要上传的文件

    private Socket socket = null;
    //获取文件列表
    private Thread inputThread = new Thread() {//TODO 后期需要转移到MainInputThread
        private InputStream inputStream = null;

        @Override
        public void run() {
            super.run();
            try {
                inputStream = socket.getInputStream();

                while (!this.isInterrupted() && !socket.isClosed() && inputStream != null) {
                    Data.DataPacket dataPacket = Data.DataPacket.parseDelimitedFrom(inputStream);
                    if (dataPacket != null)
                        if (dataPacket.hasRequestFeedback()) {
                            Data.RequestFeedback requestFeedback = dataPacket.getRequestFeedback();

                            Message message = handler.obtainMessage();

                            String filePath = new String(requestFeedback.getFilePath().toByteArray());
                            String fileSize = "";//todo get size

                            //TODO get icon

                            Log.e(TAG, "receive a feed back : " + filePath);

                            if (filePath.equals("<NULL>"))//所有结果都返回完成的标记
                            {
                                message.arg1 = -1;// 所有record都已加载
                                Log.e(TAG, "All the records loaded");
                            } else {
                                Bundle bundle = new Bundle();
                                //todo get icon


                                bundle.putString("filePath", filePath);
                                message.setData(bundle);
                            }

                            message.sendToTarget();
                        }

                }

            } catch (IOException e) {
                e.printStackTrace();
                if (inputStream != null)
                    try {
                        inputStream.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }

                if (socket != null)
                    try {
                        socket.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
            }
        }

        @Override
        public void interrupt() {
            super.interrupt();
            if (inputStream != null)
                try {
                    inputStream.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            if (socket != null)
                try {
                    socket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
        }
    };
    private AudioInputThread audioInputThread = null;

    private LoadItemsTask fileItemsLoader = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.file_explorer_layout);

        circleProgressBar = (CircleProgressBar) findViewById(R.id.file_progress_indicator);

        ListeningThread.getInstance().setHandler(handler);

        initView();

        loadFileAsync();
    }

    /**
     * 向服务器请求文件列表
     */
    private void loadFileAsync() {

        if (fileItemsLoader == null) {

            progressBar.setVisibility(View.VISIBLE);
            //todo temp
            messageItemArrayList.clear();

            fileItemsLoader = new LoadItemsTask();
        }

        if (fileItemsLoader.getStatus() == AsyncTask.Status.PENDING) {
            fileItemsLoader.execute();
        }
    }

    private void sendInfo(String deviceName, int width, int height) throws IOException {
        if (socket != null)
            Data.DataPacket.newBuilder().setDataPacketType(Data.DataPacket.DataPacketType.Info)
                    .setInfo(
                            Data.Info.newBuilder()
                                    .setDeviceName(ByteString.copyFromUtf8(deviceName))
                                    .setHeight(height)
                                    .setWidth(width)
                                    .setPortAvailable(0)
                                    .build()
                    ).build().writeDelimitedTo(socket.getOutputStream());
    }

    private void sendRequest(Data.Request.RequestType requestType, String filePath) throws IOException {
        if (socket != null)
            Data.DataPacket
                    .newBuilder()
                    .setDataPacketType(Data.DataPacket.DataPacketType.Request)
                    .setRequest(
                            Data.Request.newBuilder().setRequestType(requestType)
                                    .setFilePath(ByteString.copyFrom(filePath.getBytes()))
                    ).build()
                    .writeDelimitedTo(socket.getOutputStream());
    }

    private void initView() {

        addFileButton = (ImageButton) findViewById(R.id.addFileButton);

        listView = (ListViewCompat) findViewById(R.id.fileNameList);
        slideAdapter = new SlideAdapter();
        listView.setAdapter(slideAdapter);
        listView.setOnItemClickListener(this);

        progressBar = (ProgressBar) findViewById(R.id.progress_bar_in_display_file_list);

        //Toast.makeText(this, "ProgressDialog Showing", Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            try {
                cleanUp();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    private void addItem(MessageItem item) {
        messageItemArrayList.add(item);

        slideAdapter.notifyDataSetChanged();
        listView.invalidateViews();
        Log.e(TAG, "add item " + item.path);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
                            long id) {
        Log.e(TAG, "onItemClick position = " + position);
        //TODO

        String fileName = (view.findViewById(R.id.file_name)).getContentDescription().toString();

//        if (currentMode == MovieMode) {
//
//            try {
//               movieMode(fileName);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//
//        } else if (currentMode == MusicMode) {
//
//            try {
//                musicMode(fileName);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        } else if (currentMode == FileMode) {
//            //todo add save as
//
//            try {
//               fileMode(fileName);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
    }

    private void movieMode(String filePath) throws IOException {
        sendRequest(Data.Request.RequestType.Movie, filePath);

        Intent intent = new Intent(this, RemoteDesktopActivity.class);
        intent.putExtra("mode", "movie");
        startActivity(intent);

        cleanUp();
        finish();
    }

    private void musicMode(String filePath) throws IOException {
        sendRequest(Data.Request.RequestType.Music, filePath);
        if (audioInputThread != null)
            audioInputThread.interrupt();
        audioInputThread = new AudioInputThread();
        audioInputThread.start();
    }


    private void fileMode(String filePath) throws IOException {
        sendRequest(Data.Request.RequestType.File, filePath);

        Intent intent = new Intent(this, RemoteDesktopActivity.class);

        startActivity(intent);

        cleanUp();
        finish();
    }

    @Override
    public void onSlide(View view, int status) {
        if (lastSlideViewWithStatusOn != null && lastSlideViewWithStatusOn != view) {
            lastSlideViewWithStatusOn.shrink();
        }
        if (status == SLIDE_STATUS_ON) {
            lastSlideViewWithStatusOn = (SlideView) view;
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.delete) {
            Log.e(TAG, "delete");
            lastSlideViewWithStatusOn.shrink();
            try {
                if (v.getContentDescription() != null) {
                   removeFile(v.getContentDescription().toString());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (v.getId() == R.id.save) {
            //TODO save and promote to open
            Log.e(TAG, "save");
        }
    }

    /**
     * 请求删除文件
     * @param filePath
     * @throws IOException
     */
    private void removeFile(String filePath) throws IOException {
        sendRequest(Data.Request.RequestType.RemoveFile, filePath);
    }

    private void sendCommand(Data.Command.CommandType commandType) throws IOException {
        Data.DataPacket.newBuilder().setDataPacketType(Data.DataPacket.DataPacketType.Command)
                .setCommand(Data.Command.newBuilder().setCommandType(commandType)).build().writeDelimitedTo
                (socket.getOutputStream());
    }

    private void cleanUp() throws IOException {
        //请求静音
        try {
            sendCommand(Data.Command.CommandType.StopAudioAndVideoTransmission);
        } catch (IOException e) {
            e.printStackTrace();
        }

        inputThread.interrupt();
        ListeningThread.getInstance().setHandler(null);

        if (audioInputThread != null)
            audioInputThread.interrupt();

        if (socket != null && !socket.isClosed())
            socket.close();
    }

    private static class ViewHolder {
        public ImageView fileIcon;
        public TextView fileName;
        public TextView filePath;
        public TextView fileSize;
        public ViewGroup saveHolder;
        public ViewGroup deleteHolder;

        ViewHolder(View view) {
            fileIcon = (ImageView) view.findViewById(R.id.file_icon);
            fileName = (TextView) view.findViewById(R.id.file_name);
            filePath = (TextView) view.findViewById(R.id.file_path);
            fileSize = (TextView) view.findViewById(R.id.file_size);
            saveHolder = (ViewGroup) view.findViewById(R.id.save);
            deleteHolder = (ViewGroup) view.findViewById(R.id.delete);
        }
    }

    private class LoadItemsTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            if (!aBoolean) {
                Toast.makeText(FileManagerActivity.this, R.string.ConnectionFailed, Toast.LENGTH_LONG).show();
                startActivity(new Intent(FileManagerActivity.this, MainActivity.class));
                finish();
            }
        }


        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                if (socket == null && Constants.ServerIP != null)
                    socket = new Socket(Constants.ServerIP,
                            Constants.ServerPort);

                Log.e(TAG, "Connected");

                if (inputThread.getState() != Thread.State.RUNNABLE)
                    inputThread.start();

                Log.e(TAG, "MainInputThread start");

                sendInfo(Constants.deviceName,
                        FileManagerActivity.this.getResources().getDisplayMetrics().widthPixels,
                        FileManagerActivity.this.getResources().getDisplayMetrics().heightPixels);

                sendCommand(Data.Command.CommandType.StopAudioAndVideoTransmission);

                Log.e(TAG, "stop audio and video transmission");

                Data.Request.RequestType requestType;

                requestType = Data.Request.RequestType.File;

                //请求所有列表
                sendRequest(requestType, "*");

                Log.e(TAG, "request of all file sent");

            } catch (IOException e) {
                e.printStackTrace();
                Constants.ServerIP = null;
                return false;
            }
            return true;
        }
    }

    private class SlideAdapter extends BaseAdapter {

        private LayoutInflater mInflater;

        SlideAdapter() {
            super();
            mInflater = getLayoutInflater();
        }

        @Override
        public int getCount() {
            return messageItemArrayList.size();
        }

        @Override
        public Object getItem(int position) {
            return messageItemArrayList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            SlideView slideView = (SlideView) convertView;
            if (slideView == null) {
                View itemView = mInflater.inflate(R.layout.file_list_item, null);
                slideView = new SlideView(FileManagerActivity.this);
                slideView.setContentView(itemView);

                holder = new ViewHolder(slideView);
                slideView.setOnSlideListener(FileManagerActivity.this);
                slideView.setTag(holder);
            } else {
                holder = (ViewHolder) slideView.getTag();
            }
            MessageItem item = messageItemArrayList.get(position);

            //todo set fileIcon
//                item.icon = R.drawable.movie;

            item.slideView = slideView;
            item.slideView.shrink();

            String fileName = item.path.substring(item.path.lastIndexOf('/') + 1, item.path.length());

            holder.fileIcon.setImageBitmap(item.icon);
            holder.fileName.setText(fileName);
            holder.fileName.setContentDescription(item.path);
            holder.filePath.setText(item.path);
            holder.filePath.setText(item.size);

            Log.e(TAG, "getView  " + fileName);

            holder.saveHolder.setOnClickListener(FileManagerActivity.this);
            holder.deleteHolder.setOnClickListener(FileManagerActivity.this);

            holder.saveHolder.setContentDescription(item.path);
            holder.deleteHolder.setContentDescription(item.path);
            return slideView;
        }

    }
}
