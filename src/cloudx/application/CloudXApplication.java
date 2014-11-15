package cloudx.application;

import android.app.Application;

import java.io.IOException;
import java.net.Socket;

/**
 * Created by zhugongpu on 14/11/15.
 */
public class CloudXApplication extends Application {

    public static String SharedPreferenceName = "CloudX";
    public static String SharedPreference_Key_Account = "Account";
    public static String SharedPreference_Key_Password = "Password";
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

                //TODO 接收数据

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
