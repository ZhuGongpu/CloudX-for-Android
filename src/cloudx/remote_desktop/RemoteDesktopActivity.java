package cloudx.remote_desktop;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import cloudx.display_file_list.FileManagerActivity;
import cloudx.main.MainActivity;
import cloudx.main.R;
import cloudx.view.CircleProgressBar.CircleProgressBar;
import cloudx.view.findMyDevice.FindMyDeviceAlertDialog;
import cloudx.view.menu.ResideMenu;
import cloudx.view.menu.ResideMenuItem;
import cloudx.view.messagebox.MessageBox;
import cloudx.view.messagebox.MessageSender;
import com.google.protobuf.ByteString;
import common.message.Data;
import common.message.LocalCommand;
import data.information.FileInfo;
import data.information.GlobalSettingsAndInformation;
import model.listener.CommandSender;
import model.listener.GestureListener;
import model.listener.OnLongClickGestureListener;
import model.network.ActiveInputThread;
import model.network.AudioInputThread;
import model.network.MainPassiveInputThread;
import utils.KeyCodeConverter;
import utils.OpenFileUtils;

import java.io.IOException;
import java.net.Socket;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Queue;

public class RemoteDesktopActivity extends Activity {

    private static final int MovieMode = 1;
    private static final int MAX_QUEUE_SIZE = 5;
    private static String TAG = "RemoteDesktopActivity";
    //private static float scaleRate = 1;
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
    private MainPassiveInputThread videoThread = null;
    private AudioInputThread audioThread = null;
    private boolean stopAudioAndVideoStream = true;//用于标记从movie mode 的 remote desktop 切换到 remote desktop时是否需要关闭
    private boolean isScreenLocked = false;

    private ResideMenu resideMenu = null;
    private ImageView mainView = null;

    private ImageButton showKeyboardButton = null;
    private ImageButton lockScreenButton = null;
    private Bitmap previousBitmap = null;

    private boolean networkDataHandlerEnabled = true;
    private CircleProgressBar progressBar = null;
    private Queue<Bitmap> bitmapQueue = new LinkedList<Bitmap>();
    private Handler networkDataHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg
                    .arg1) {
                case GlobalSettingsAndInformation.MessageType_Bitmap:
                    if (networkDataHandlerEnabled)
                        if (msg.obj instanceof Bitmap) {
                            if (mainView != null) {
                                long startTime = Calendar.getInstance().getTimeInMillis();

                                bitmapQueue.offer((Bitmap) msg.obj);

                                while (bitmapQueue.size() >= MAX_QUEUE_SIZE) {
                                    previousBitmap = bitmapQueue.poll();
                                    if (!previousBitmap.isRecycled())
                                        previousBitmap.recycle();
                                }

                                previousBitmap = bitmapQueue.poll();
                                long endTime = Calendar.getInstance().getTimeInMillis();

//                                Log.e(TAG, "Queue : " + (endTime - startTime) + " ms");

                                startTime = endTime;

                                if (previousBitmap != null)
                                    mainView.setImageBitmap(previousBitmap);

                                endTime = Calendar.getInstance().getTimeInMillis();

//                                Log.e(TAG, "SetBitmap : " + (endTime - startTime) + " ms");

//                    if (previousBitmap != null && !previousBitmap.isRecycled()) {
//                        previousBitmap.recycle();
//                        Log.e(TAG, "RECYCLED");
//                    }
//                    previousBitmap = (Bitmap) msg.obj;
//                    mainView.setImageBitmap(previousBitmap);
//                    mainView.setImageBitmap(Bitmap.createScaledBitmap(previousBitmap, (int) (previousBitmap.getWidth() * scaleRate),
//                            (int) (previousBitmap.getHeight() * scaleRate), true));
                            }
                        }
                    break;
                case GlobalSettingsAndInformation.MessageType_Message:
                    new MessageBox(RemoteDesktopActivity.this, (String) msg.obj).show();
                    break;
                case GlobalSettingsAndInformation.MessageType_FileInfo:


//                    Log.e(TAG, "TO DO start activity");
                    networkDataHandlerEnabled = false;

                    if (!progressBar.isShown()) {
                        progressBar.setVisibility(View.VISIBLE);
                    }

                    final FileInfo info = (FileInfo) msg.obj;

                    // Log.e(TAG, info.totalReceivedSize + " / " + info.fileLength);

                    if (info != null) {
                        if (info.totalReceivedSize >= info.fileLength) {

                            progressBar.setVisibility(View.INVISIBLE);
                            networkDataHandlerEnabled = true;

//                            Toast.makeText(RemoteDesktopActivity.this,
//                                    getText(R.string.FileTransmissionDone), Toast.LENGTH_LONG).show();

//                            try {
//                                Runtime.getRuntime().exec(info.filePath);
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }

                            new AlertDialog.Builder(RemoteDesktopActivity.this).setTitle("文件已保存至内存卡，是否打开")
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
                            progressBar.setProgress((int)
                                    (((double) info.totalReceivedSize / (double) info.fileLength) * 100));
                    }
                    break;

                case GlobalSettingsAndInformation.MessageType_FindMyDevice:
                    new FindMyDeviceAlertDialog(RemoteDesktopActivity.this);
                    break;
                default:
                    break;
            }
        }
    };
    private GestureListener gestureListener = null;
    private Handler localCommandHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.arg1) {
                case LocalCommand.Show_OptionMenu:
                    resideMenu.openMenu();
                    gestureListener.setGestureListenerEnabled(false);
                    break;
                case LocalCommand.Hide_OptionMenu:
                    resideMenu.closeMenu();
                    gestureListener.setGestureListenerEnabled(true);
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        Log.e(TAG, "onKeyDown : " + keyCode);

        if (keyCode == KeyEvent.KEYCODE_BACK) {

            startActivity(new Intent(this, MainActivity.class));
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
        setContentView(R.layout.remote_desktop);

        progressBar = (CircleProgressBar) findViewById(R.id.file_transmission_progress_indicator);

        networkDataHandlerEnabled = true;
        //set handler
        ActiveInputThread.getInstance().setHandler(networkDataHandler);


        new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(Void... voids) {

                try {
                    socket = new Socket(GlobalSettingsAndInformation.ServerIP,
                            GlobalSettingsAndInformation.ServerPort);

                    sendInfo(GlobalSettingsAndInformation.deviceName,
                            RemoteDesktopActivity.this.getResources().getDisplayMetrics().widthPixels,
                            RemoteDesktopActivity.this.getResources().getDisplayMetrics().heightPixels);

                    sendCommand(Data.Command.CommandType.StartAudioAndVideoTransmission);
                    Log.e(TAG, "doInBackground : StartAudioAndVideoTransmission request send");
                    return true;
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                    GlobalSettingsAndInformation.ServerIP = null;
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean result) {
                super.onPostExecute(result);

                if (result) {
                    Log.d(TAG, "connected");

                    Toast.makeText(RemoteDesktopActivity.this, "已连接", Toast.LENGTH_SHORT).show();

                    initViews();


                    videoThread = new MainPassiveInputThread(socket, networkDataHandler
//                            , scaleRate
                    );
                    videoThread.start();

                    audioThread = new AudioInputThread();
                    audioThread.start();

                } else {
                    Log.e(TAG, "connection failed");
                    Toast.makeText(RemoteDesktopActivity.this, "请检查网络设置", Toast.LENGTH_LONG).show();
                    cleanUpAndFinish();
                }
            }
        }.execute();
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

    private void initViews() {
        findViewById(R.id.progress_bar).setVisibility(View.GONE);


        showKeyboardButton = (ImageButton) findViewById(R.id.showKeyboard);
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

        gestureListener = new GestureListener(localCommandHandler);
        gestureListener.setGestureListenerEnabled(true);
        gestureListener.setOnLongClickGestureListener(selectWindowListener);
        gestureListener.setCommandSender(new CommandSender() {
            @Override
            public void sendCommand(Data.Command.CommandType commandType, float x, float y) {
                try {
                    Data.DataPacket.newBuilder()
                            .setDataPacketType((Data.DataPacket.DataPacketType.Command))
                            .setCommand(
                                    Data.Command.newBuilder()
                                            .setCommandType(commandType)
                                            .setX(x)
                                            .setY(y)
                                            .build()
                            )
                            .build().writeDelimitedTo(socket.getOutputStream());
                } catch (IOException e) {
                    e.printStackTrace();
                    //todo handler exception
                }
            }
        });
        mainView.setOnTouchListener(gestureListener);

        resideMenu = new ResideMenu(this);
        resideMenu.setBackground(R.drawable.menu_background);
        resideMenu.attachToActivity(this);

        // create menu items;


        ResideMenuItem fileManager = new ResideMenuItem(this);
        fileManager.setTitle(R.string.ShareFile);
        fileManager.setIcon(R.drawable.share);
        fileManager.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    sendCommand(Data.Command.CommandType.StopAudioAndVideoTransmission);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Intent intent = new Intent(RemoteDesktopActivity.this, FileManagerActivity.class);

                startActivity(intent);
                cleanUpAndFinish();

            }
        });

        ResideMenuItem exit = new ResideMenuItem(this);
        exit.setTitle(R.string.exit);
        exit.setIcon(R.drawable.exit);
        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exit();
            }
        });

//        if (currentMode == NormalMode) {
//            movieItem = new ResideMenuItem(this);
//            movieItem.setTitle(R.string.MovieMode);
//            movieItem.setIcon(R.drawable.movie);
//            movieItem.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    try {
//                        sendCommand(Data.Command.CommandType.StopAudioAndVideoTransmission);
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//
//                    Intent intent = new Intent(RemoteDesktopActivity.this, FileManagerActivity.class);
//                    intent.putExtra("mode", "movie");
//                    stopAudioAndVideoStream = false;
//                    startActivity(intent);
//                    cleanUpAndFinish();
//                }
//            });
//
//        } else {
//            // movie mode
//            remoteDesktop = new ResideMenuItem(this);
//            remoteDesktop.setTitle(R.string.RemoteDesktop);
//            remoteDesktop.setIcon(R.drawable.remotedesktop);
//            remoteDesktop.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    try {
//                        sendCommand(Data.Command.CommandType.StopAudioAndVideoTransmission);
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                    stopAudioAndVideoStream = false;
//                    startActivity(new Intent(RemoteDesktopActivity.this, RemoteDesktopActivity.class));
//                    cleanUpAndFinish();
//                }
//            });
//
//        }


        ResideMenuItem sendMessage = new ResideMenuItem(this);
        sendMessage.setTitle(R.string.SendMessage);
        sendMessage.setIcon(R.drawable.message);
        sendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                new MessageBox(RemoteDesktopActivity.this, new MessageSender() {
                    @Override
                    public void sendMessage(String message) throws IOException {
                        RemoteDesktopActivity.this.sendMessage(message);
                    }
                }).show();
            }

        });

//        //add views
//        if (remoteDesktop != null)
//            resideMenu.addMenuItem(remoteDesktop);
//
//        if (movieItem != null)
//            resideMenu.addMenuItem(movieItem);

        resideMenu.addMenuItem(fileManager);
        resideMenu.addMenuItem(sendMessage);

        resideMenu.addMenuItem(exit);
    }

    private void showKeyboard() {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

//    private void hideKeyboard() {
//        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//
//        inputMethodManager.hideSoftInputFromWindow(invisibleEditText.getWindowToken(), 0);
//    }

    public void sendKeyCode(int keyCode) throws IOException {
        if (socket != null)
            Data.DataPacket.newBuilder().setDataPacketType(Data.DataPacket.DataPacketType.KeyboardEvent)
                    .setKeyboardEvent(
                            Data.KeyboardEvent.newBuilder().setKeyCode(keyCode)
                    ).build()
                    .writeDelimitedTo(socket.getOutputStream());
    }

    private void sendMessage(String message) throws IOException {
        if (socket != null) {
            Data.DataPacket.newBuilder()
                    .setDataPacketType(Data.DataPacket.DataPacketType.SharedMessage)
                    .setSharedMessage(
                            Data.SharedMessage.newBuilder()
                                    .setContent(ByteString.copyFrom(message.getBytes())).build()
                    ).build()
                    .writeDelimitedTo(socket.getOutputStream());
        }
    }

    private void sendCommand(Data.Command.CommandType commandType) throws IOException {
        if (socket != null)
            Data.DataPacket.newBuilder().setDataPacketType(Data.DataPacket.DataPacketType.Command)
                    .setCommand(Data.Command.newBuilder()
                            .setCommandType(commandType)).build()
                    .writeDelimitedTo(socket.getOutputStream());
    }

    private void exit() {
        cleanUpAndFinish();
        GlobalSettingsAndInformation.ServerIP = null;//todo
        ActiveInputThread.getInstance().interrupt();
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

        ActiveInputThread.getInstance().setHandler(null);

        finish();
    }


}
