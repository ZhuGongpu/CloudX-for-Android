package cloudx.views;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import cloudx.listener.CommandSender;
import cloudx.listener.GestureListener;
import cloudx.listener.OnLongClickGestureListener;
import cloudx.model.Constants;
import cloudx.network.AudioInputThread;
import cloudx.network.VideoInputThread;
import cloudx.utils.KeyCodeConverter;
import common.message.Data;
import common.message.ProtoBufHelper;

import java.io.IOException;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;

public class RemoteDesktopActivity extends Activity {


    private static final int MAX_QUEUE_SIZE = 5;
    private static String TAG = "RemoteDesktopActivity";

    /**
     * 调用RemoteDesktopActivity时，需要设置以下参数
     */
    private String peerIP = null;
    private int peerPort = 0;
    private int resolution_width = 0;
    private int resolution_height = 0;

    private Socket socket = null;
    private OnLongClickGestureListener selectWindowListener = new OnLongClickGestureListener() {
        public boolean isWindowSelected = false;

        @Override
        public void onLongClick(MotionEvent event) {
            if (!isWindowSelected && event.getX() > 0 && event.getY() > 0) {
                try {
                    if (socket != null)
                        Data.DataPacket.newBuilder().setDataPacketType(Data.DataPacket.DataPacketType.Command).setCommand(
                                Data.Command.newBuilder().setCommandType(Data.Command.CommandType.SelectWindow)
                                        .setX(event.getX()).setY(event.getY())
                        ).build().writeDelimitedTo(socket.getOutputStream());

                    Log.d(TAG, "Long Click Command Sent");
                    isWindowSelected = true;

                    Toast.makeText(RemoteDesktopActivity.this, R.string.WinddowSelected, Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    if (socket != null)
                        Data.DataPacket.newBuilder().setDataPacketType(Data.DataPacket.DataPacketType.Command).setCommand(
                                Data.Command.newBuilder().setCommandType(Data.Command.CommandType.SelectWindow)
                                        .setX(-1).setY(-1)
                        ).build().writeDelimitedTo(socket.getOutputStream());
                    isWindowSelected = false;

                    Toast.makeText(RemoteDesktopActivity.this, R.string.WindowUnselected, Toast.LENGTH_SHORT).show();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    };
    private VideoInputThread videoThread = null;
    private AudioInputThread audioThread = null;
    private boolean isScreenLocked = false;

    private ImageView mainView = null;

    private ImageButton lockScreenButton = null;


    private Queue<Bitmap> bitmapQueue = new LinkedList<Bitmap>();
    /**
     * 处理RemoteDesktopActivity专用数据
     */
    private Handler networkDataHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            if (msg.arg1 == Constants.MessageType_Bitmap && msg.obj instanceof Bitmap) {

                Log.e(TAG, "UI get bitmap");

                if (mainView != null) {
                    bitmapQueue.offer((Bitmap) msg.obj);

                    Bitmap previousBitmap;
                    while (bitmapQueue.size() >= MAX_QUEUE_SIZE) {
                        previousBitmap = bitmapQueue.poll();
                        if (!previousBitmap.isRecycled())
                            previousBitmap.recycle();
                    }
                    previousBitmap = bitmapQueue.poll();
//TODO 如果子线程中不能使用canvas，则需要在此修改bitmap

                    if (previousBitmap != null)
                        mainView.setImageBitmap(previousBitmap);
                }
            }
        }
    };

    /**
     * 响应手势事件
     */
    private GestureListener gestureListener = null;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.e(TAG, "onKeyDown : " + keyCode);
        if (keyCode == KeyEvent.KEYCODE_BACK) {
//
//            startActivity(new Intent(this, MainActivity.class));
            cleanUpAndFinish();
            return true;
        } else {
            //send the keycode
            int key = KeyCodeConverter.AndroidKeyToWindowsKey(keyCode);
            if (key != 0)
                try {
                    sendKeyCode(key);

                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /**
         * 获取peer信息
         */
        Intent intent = getIntent();
        if (intent == null) finish();
        else {
            peerIP = intent.getStringExtra(Constants.ParaName_PeerIP);
            peerPort = intent.getIntExtra(Constants.ParaName_PeerPort, 0);
            resolution_width = intent.getIntExtra(Constants.ParaName_Resolution_Width, 0);
            resolution_height = intent.getIntExtra(Constants.ParaName_Resolution_Height, 0);
        }
        if (peerIP == null || peerPort <= 0 || resolution_height <= 0 || resolution_width <= 0)
            finish();

        setContentView(R.layout.remote_desktop);

        Log.e(TAG, "RemoteDesktopActivity onCreate");

        /**
         * 连接选定的peer
         */
        new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(Void... voids) {

                try {
                    Log.e(TAG, "doInBackground RemoteDesktopActivity");
                    Log.e(TAG, "Connecting to " + peerIP + " at " + peerPort);

                    socket = new Socket(peerIP,
                            peerPort);

                    socket.setTcpNoDelay(true);
                    socket.setSoTimeout(10000);

                    //发送设备信息
                    ProtoBufHelper.sendDeviceInfo(
                            ProtoBufHelper.genDeviceInfo(
                                    Constants.deviceName,
                                    ProtoBufHelper.genResolution(
                                            RemoteDesktopActivity.this.getResources().getDisplayMetrics().widthPixels,
                                            RemoteDesktopActivity.this.getResources().getDisplayMetrics().heightPixels
                                    )),
                            socket.getOutputStream());

                    /**
                     * 开始传输音频和视频信息
                     */
                    ProtoBufHelper.sendCommand(
                            ProtoBufHelper.genCommandBuilder(
                                    Data.Command.CommandType.StartVideoTransmission
                            ),
                            socket.getOutputStream());

                    Log.e(TAG, "doInBackground : StartAudioAndVideoTransmission request send");
                    return true;
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean result) {
                super.onPostExecute(result);

                if (result) {
                    Log.d(TAG, "connected");

                    Toast.makeText(RemoteDesktopActivity.this, getText(R.string.Connected), Toast.LENGTH_SHORT).show();
                    initViews();

                    videoThread = new VideoInputThread(socket, networkDataHandler
//                            , scaleRate
                    );

                    videoThread.start();

                    audioThread = new AudioInputThread(peerIP, peerPort);
                    audioThread.start();
                } else {
                    Log.e(TAG, "connection failed");
                    Toast.makeText(RemoteDesktopActivity.this, getText(R.string.ConnectionFailed), Toast.LENGTH_LONG).show();
                    cleanUpAndFinish();
                }
            }
        }.execute();


        //set handler
//        ListeningThread.getInstance().setHandler(networkDataHandler);
    }

    private void initViews() {
        findViewById(R.id.progress_bar).setVisibility(View.GONE);


        ImageButton showKeyboardButton = (ImageButton) findViewById(R.id.showKeyboard);
        showKeyboardButton.setVisibility(View.VISIBLE);
        showKeyboardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showKeyboard();
            }
        });

        lockScreenButton = (ImageButton) findViewById(R.id.lockScreen);
        lockScreenButton.setVisibility(View.VISIBLE);
        lockScreenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isScreenLocked) {
                    lockScreenButton.setImageResource(R.drawable.unlock);
                    isScreenLocked = false;
                } else {
                    lockScreenButton.setImageResource(R.drawable.lock);
                    isScreenLocked = true;
                }

                if (gestureListener != null) {
                    gestureListener.setScreenLocked(isScreenLocked);
                }
            }
        });

        mainView = (ImageView) findViewById(R.id.main_view);

        gestureListener = new GestureListener(resolution_width, resolution_height);
        gestureListener.setGestureListenerEnabled(true);
        gestureListener.setOnLongClickGestureListener(selectWindowListener);
        gestureListener.setCommandSender(new CommandSender() {
            @Override
            public void sendCommand(Data.Command.CommandType commandType, float x, float y) {
                try {
                    ProtoBufHelper.sendCommand(
                            ProtoBufHelper.genCommand(commandType, x, y),
                            socket.getOutputStream()
                    );
                } catch (IOException e) {
                    e.printStackTrace();

                }
            }
        });
        mainView.setOnTouchListener(gestureListener);
    }

    private void showKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }


    public void sendKeyCode(int keyCode) throws IOException {
        if (socket != null)
            ProtoBufHelper.sendKeyboardEvent(
                    ProtoBufHelper.genKeyboardEvent(keyCode),
                    socket.getOutputStream()

            );
    }

    private void sendCommand(Data.Command.CommandType commandType) throws IOException {
        if (socket != null)
            ProtoBufHelper.sendCommand(
                    ProtoBufHelper.genCommandBuilder(commandType),
                    socket.getOutputStream()
            );
    }

    private void cleanUpAndFinish() {
        //请求静音
        try {
            sendCommand(Data.Command.CommandType.StopAudioAndVideoTransmission);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (videoThread != null)
            videoThread.interrupt();
        if (audioThread != null)
            audioThread.interrupt();

        for (Bitmap bitmap : bitmapQueue) {
            if (!bitmap.isRecycled())
                bitmap.recycle();
        }

        if (socket != null && !socket.isClosed())
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        finish();
    }


}
