package cloudx.application;

import android.app.Application;
import common.message.Data;
import utils.ByteStringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

/**
 * Created by zhugongpu on 14/11/15.
 */
public class CloudXApplication extends Application {

    //shared preferences
    public static final String SharedPreferenceName = "CloudX";
    public static final String SharedPreference_Key_Account = "Account";
    public static final String SharedPreference_Key_Password = "Password";
    public static String CloudStorageToken = null;
    private NetworkThread networkThread = null;

    @Override
    public void onCreate() {
        super.onCreate();

    }


    public void startNetworkThread(String severIP, int severPort) {

        if (networkThread != null) {
            //当线程正在运行时，避免再次启动
            if (networkThread.getState() != Thread.State.TERMINATED)
                return;
            networkThread = null;
        }

        networkThread = new NetworkThread(severIP, severPort);
        networkThread.start();
    }

    public void terminateNetworkThhread() {
        if (networkThread != null) {
            networkThread.interrupt();
        }
        networkThread = null;
    }


    /**
     * 处理共有的基础网络数据
     */
    private class NetworkThread extends Thread {

        private Socket socket = null;

        private int severPort = -1;
        private String severIP = null;

        public NetworkThread(String severIP, int severPort) {
            this.severPort = severPort;
            this.severIP = severIP;
        }

        @Override
        public void run() {
            super.run();

            try {
                this.socket = new Socket(severIP, severPort);

                InputStream inputStream = socket.getInputStream();

                while (!this.isInterrupted() && inputStream != null) {
                    Data.DataPacket dataPacket = Data.DataPacket.parseDelimitedFrom(inputStream);

                    //TODO handle data input
                    if (dataPacket != null && dataPacket.getDataPacketType() != null)
                        switch (dataPacket.getDataPacketType()) {
                            case CloudStorageToken:
                                //保存token
                                CloudXApplication.CloudStorageToken = ByteStringUtils.byteStringToString(dataPacket.getSharedMessage().getContent());
                                break;
                            case DeviceInfo:
                                //TODO 保存 device info
                                break;
                            case FileRequest:
                                //TODO 传输文件
                                break;
                            case SharedMessage:
                                //TODO 弹出提示
                                break;
                            case Command:
                                //TODO 产生相应的响应（包括find my device）
                                break;
                            default:
                                break;
                        }
                }

                if (inputStream != null)
                    inputStream.close();

            } catch (IOException e) {
                e.printStackTrace();
            } finally {

                try {
                    if (socket != null)
                        socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
