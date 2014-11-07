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
import cloudx.remote_desktop.RemoteDesktopActivity;
import cloudx.view.CircleProgressBar.CircleProgressBar;
import cloudx.view.findMyDevice.FindMyDeviceAlertDialog;
import cloudx.view.listview.ListViewCompat;
import cloudx.view.listview.MessageItem;
import cloudx.view.listview.SlideView;
import cloudx.view.messagebox.MessageBox;
import com.google.protobuf.ByteString;
import common.message.Data;
import data.information.FileInfo;
import data.information.GlobalSettingsAndInformation;
import model.network.ListeningThread;
import model.network.AudioInputThread;
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
    private static final int MovieMode = 1;
    private static final int MusicMode = 2;
    private static final int FileMode = 0;
    private CircleProgressBar circleProgressBar = null;
    private int currentMode = 0;
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

            if (msg.arg1 == GlobalSettingsAndInformation.MessageType_FindMyDevice) {
                new FindMyDeviceAlertDialog(FileManagerActivity.this);
            } else if (msg.arg1 == GlobalSettingsAndInformation.MessageType_Message) {
                new MessageBox(FileManagerActivity.this, (String) msg.obj).show();
            } else if (msg.arg1 == GlobalSettingsAndInformation.MessageType_FileInfo) {
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
                //todo set iconResource
                //messageItem.iconResource = bundle.get("iconResource");
                messageItem.name = bundle != null ? bundle.getString("fileName") : null;
                Log.e(TAG, "TODO add item");
                addItem(messageItem);
            }
        }
    };
    private SlideAdapter slideAdapter = null;

    private ImageButton fileModeButton = null;
    private ImageButton musicModeButton = null;
    private ImageButton movieModeButton = null;

    private ImageButton addFileButton = null;
    private TextView tittle = null;

    private Socket socket = null;
    private Thread inputThread = new Thread() {
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

                            Log.e(TAG, "receive a feed back : " + filePath);

                            if (filePath.equals("<NULL>"))//所有结果都返回完成的标记
                            {
                                message.arg1 = -1;// 所有record都已加载
                                Log.e(TAG, "All the records loaded");
                            } else {
                                Bundle bundle = new Bundle(2);
                                //todo get iconResource
                                bundle.putString("fileName", filePath);
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
    private LoadItemsTask musicItemsLoader = null;
    private LoadItemsTask movieItemsLoader = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.display_file_list_layout);

        circleProgressBar = (CircleProgressBar) findViewById(R.id.file_progress_indicator);

        ListeningThread.getInstance().setHandler(handler);

        initView();
        setCurrentMode(FileMode);
//        new AsyncTask<Void, Void, Void>() {
//            @Override
//            protected Void doInBackground(Void... params) {
//                try {
//
//                    socket = new Socket(GlobalSettingsAndInformation.ServerIP,
//                            GlobalSettingsAndInformation.ServerPort);
//
//                    inputThread.start();
//
//                    Log.e(TAG, "MainInputThread start");
//
//                    sendInfo(GlobalSettingsAndInformation.deviceName,
//                            FileManagerActivity.this.getResources().getDisplayMetrics().widthPixels,
//                            FileManagerActivity.this.getResources().getDisplayMetrics().heightPixels);
//
//                    sendCommand(Data.Command.CommandType.StopAudioAndVideoTransmission);
//
//                    Log.e(TAG, "stop audio and video transmission");
//
//                    Data.Request.RequestType requestType = null;
//
//                    if (currentMode == MovieMode)
//                        requestType = Data.Request.RequestType.Movie;
//                    else if (currentMode == MusicMode)
//                        requestType = Data.Request.RequestType.Music;
//                    else
//                        requestType = Data.Request.RequestType.File;
//
//                    //请求所有视频列表
//                    sendRequest(requestType, "*");
//
//                    Log.e(TAG, "request of all file sent");
//
//                } catch (IOException e) {
//                    e.printStackTrace();
//                    GlobalSettingsAndInformation.ServerIP = null;
//                }
//                return null;
//            }
//
//        }.execute();

    }

    private void setCurrentMode(int currentMode) {
        this.currentMode = currentMode;


        switch (currentMode) {
            case FileMode:
                tittle.setText(R.string.FileMode);
                loadFileAsync();
                break;
            case MovieMode:
                tittle.setText(R.string.MovieMode);
                loadMovieAsync();
                break;
            case MusicMode:
                tittle.setText(R.string.MusicMode);
                loadMusicAsync();
                break;
            default:
                break;
        }
    }

    private void loadFileAsync() {

        Log.e(TAG, "FileMode");

        if (fileItemsLoader == null) {

            progressBar.setVisibility(View.VISIBLE);
            //todo temp
            messageItemArrayList.clear();

            fileItemsLoader = new LoadItemsTask();

            if (musicItemsLoader != null) {
                musicItemsLoader.cancel(true);
                musicItemsLoader = null;
            }
            if (movieItemsLoader != null) {
                movieItemsLoader.cancel(true);
                movieItemsLoader = null;
            }

            if (fileItemsLoader.getStatus() == AsyncTask.Status.PENDING) {
                fileItemsLoader.execute();
            }

        }
    }

    private void loadMusicAsync() {

        Log.e(TAG, "MusicMode");

        if (musicItemsLoader == null) {

            progressBar.setVisibility(View.VISIBLE);
            //todo temp
            messageItemArrayList.clear();

            musicItemsLoader = new LoadItemsTask();

            if (fileItemsLoader != null) {
                fileItemsLoader.cancel(true);
                fileItemsLoader = null;
            }
            if (movieItemsLoader != null) {
                movieItemsLoader.cancel(true);
                movieItemsLoader = null;
            }

            if (musicItemsLoader.getStatus() == AsyncTask.Status.PENDING) {
                musicItemsLoader.execute();
            }
        }
    }

    private void loadMovieAsync() {

        Log.e(TAG, "MovieMode");

        if (movieItemsLoader == null) {

            progressBar.setVisibility(View.VISIBLE);
            //todo temp
            messageItemArrayList.clear();

            movieItemsLoader = new LoadItemsTask();

            if (fileItemsLoader != null) {
                fileItemsLoader.cancel(true);
                fileItemsLoader = null;
            }
            if (musicItemsLoader != null) {
                musicItemsLoader.cancel(true);
                musicItemsLoader = null;
            }

            if (movieItemsLoader.getStatus() == AsyncTask.Status.PENDING) {
                movieItemsLoader.execute();
            }
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
        fileModeButton = (ImageButton) findViewById(R.id.fileListButton);
        fileModeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setCurrentMode(FileMode);
                fileModeButton.setImageResource(R.drawable.file_mode_selected);
                movieModeButton.setImageResource(R.drawable.movie_mode_normal);
                musicModeButton.setImageResource(R.drawable.music_mode_normal);
            }
        });
        movieModeButton = (ImageButton) findViewById(R.id.movieListButton);
        movieModeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setCurrentMode(MovieMode);
                movieModeButton.setImageResource(R.drawable.movie_mode_selected);
                fileModeButton.setImageResource(R.drawable.file_mode_normal);
                musicModeButton.setImageResource(R.drawable.music_mode_normal);
            }
        });
        musicModeButton = (ImageButton) findViewById(R.id.musicListButton);
        musicModeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setCurrentMode(MusicMode);
                musicModeButton.setImageResource(R.drawable.music_mode_selected);
                fileModeButton.setImageResource(R.drawable.file_mode_normal);
                movieModeButton.setImageResource(R.drawable.movie_mode_normal);
            }
        });

        addFileButton = (ImageButton) findViewById(R.id.addFileButton);

        tittle = (TextView) findViewById(R.id.listTittle);

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
        Log.e(TAG, "add item " + item.name);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
                            long id) {
        Log.e(TAG, "onItemClick position = " + position);
        //todo

        String fileName = (view.findViewById(R.id.file_name)).getContentDescription().toString();

        if (currentMode == MovieMode) {

            try {
                sendRequest(Data.Request.RequestType.Movie, fileName);

                Intent intent = new Intent(this, RemoteDesktopActivity.class);
                intent.putExtra("mode", "movie");
                startActivity(intent);

                cleanUp();
                finish();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else if (currentMode == MusicMode) {

            try {
                sendRequest(Data.Request.RequestType.Music, fileName);
                if (audioInputThread != null)
                    audioInputThread.interrupt();
                audioInputThread = new AudioInputThread();
                audioInputThread.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (currentMode == FileMode) {
            //todo add save as

            try {
                sendRequest(Data.Request.RequestType.File, fileName);

                Intent intent = new Intent(this, RemoteDesktopActivity.class);

                startActivity(intent);

                cleanUp();
                finish();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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

                    if (currentMode == MovieMode)
                        sendRequest(Data.Request.RequestType.RemoveMovie, v.getContentDescription().toString());
                    else if (currentMode == MusicMode)
                        sendRequest(Data.Request.RequestType.RemoveMusic, v.getContentDescription().toString());
                    else if (currentMode == FileMode) {
                        sendRequest(Data.Request.RequestType.RemoveFile, v.getContentDescription().toString());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (v.getId() == R.id.save) {
            //todo save and open
            Log.e(TAG, "save");

        }


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

    private void sendCommand(Data.Command.CommandType commandType) throws IOException {
        Data.DataPacket.newBuilder().setDataPacketType(Data.DataPacket.DataPacketType.Command)
                .setCommand(Data.Command.newBuilder().setCommandType(commandType)).build().writeDelimitedTo
                (socket.getOutputStream());
    }

    private static class ViewHolder {
        public ImageView icon;
        public TextView fileNameTextView;
        public ViewGroup saveHolder;
        public ViewGroup deleteHolder;

        ViewHolder(View view) {
            icon = (ImageView) view.findViewById(R.id.icon);
            fileNameTextView = (TextView) view.findViewById(R.id.file_name);
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

//        @Override
//        protected void onCancelled(Boolean aBoolean) {
//            super.onCancelled(aBoolean);
//            Log.e(TAG, "onCancelled");
//            try {
//                socket.close();
//                socket = null;
//
//                inputThread.interrupt();
//
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//
//        @Override
//        protected void onCancelled() {
//            super.onCancelled();
//            Log.e(TAG, "onCancelled");
//            try {
//                socket.close();
//                socket = null;
//
//                inputThread.interrupt();
//
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                if (socket == null && GlobalSettingsAndInformation.ServerIP != null)
                    socket = new Socket(GlobalSettingsAndInformation.ServerIP,
                            GlobalSettingsAndInformation.ServerPort);

                Log.e(TAG, "Connected");

                if (inputThread.getState() != Thread.State.RUNNABLE)
                    inputThread.start();

                Log.e(TAG, "MainInputThread start");

                sendInfo(GlobalSettingsAndInformation.deviceName,
                        FileManagerActivity.this.getResources().getDisplayMetrics().widthPixels,
                        FileManagerActivity.this.getResources().getDisplayMetrics().heightPixels);

                sendCommand(Data.Command.CommandType.StopAudioAndVideoTransmission);

                Log.e(TAG, "stop audio and video transmission");

                Data.Request.RequestType requestType = null;

                if (currentMode == MovieMode)
                    requestType = Data.Request.RequestType.Movie;
                else if (currentMode == MusicMode)
                    requestType = Data.Request.RequestType.Music;
                else
                    requestType = Data.Request.RequestType.File;

                //请求所有列表
                sendRequest(requestType, "*");

                Log.e(TAG, "request of all file sent");

            } catch (IOException e) {
                e.printStackTrace();
                GlobalSettingsAndInformation.ServerIP = null;
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
                View itemView = mInflater.inflate(R.layout.list_item, null);

                slideView = new SlideView(FileManagerActivity.this);
                slideView.setContentView(itemView);

                holder = new ViewHolder(slideView);
                slideView.setOnSlideListener(FileManagerActivity.this);
                slideView.setTag(holder);
            } else {
                holder = (ViewHolder) slideView.getTag();
            }
            MessageItem item = messageItemArrayList.get(position);

            if (currentMode == MusicMode)
                item.iconResource = R.drawable.music_file_icon_3_48px;
            else if (currentMode == MovieMode)
                item.iconResource = R.drawable.movie;
            else if (currentMode == FileMode) {
                //todo set icon
            }

            item.slideView = slideView;
            item.slideView.shrink();


            String fileName = item.name.substring(item.name.lastIndexOf('\\') + 1, item.name.length());


            holder.icon.setImageResource(item.iconResource);
            holder.fileNameTextView.setText(fileName);
            holder.fileNameTextView.setContentDescription(item.name);

            Log.e(TAG, "getView  " + fileName);

            holder.saveHolder.setOnClickListener(FileManagerActivity.this);
            holder.deleteHolder.setOnClickListener(FileManagerActivity.this);


            holder.saveHolder.setContentDescription(item.name);
            holder.deleteHolder.setContentDescription(item.name);
            return slideView;
        }

    }
}
