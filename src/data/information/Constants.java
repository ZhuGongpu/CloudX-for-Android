package data.information;

import android.os.Build;

/**
 * Created by Gongpu on 14-3-22.
 */
public class Constants {
    public static final int MessageType_Bitmap = 10;
    public static final int MessageType_Message = 20;
    public static final int MessageType_FileInfo = 30;
    public static final int MessageType_FindMyDevice = 40;

    /**
     * 调用RemoteDesktopActivity时，需要传入以下参数
     */
    public static final String ParaName_PeerIP = "PeerIP";
    public static final String ParaName_PeerPort = "PeerPort";
    public static final String ParaName_PeerPortAvailable = "PeerPortAvailable";
    public static final String ParaName_Resolution_Width = "Resolution_Width";
    public static final String ParaName_Resolution_Height = "Resolution_Height";



    //    public static String ServerIP = null;
//    public static int ServerPort = 50323;
    public static String deviceName = Build.BRAND + " " + Build.MODEL;
    public static int portAvailable = 50323;
//    public static String localIP = null;
//    public static int ServerPortAvailable = 50324;
//    public static int ServerResolutionWidth = 1366;
//    public static int ServerResolutionHeight = 768;
}
