package cloudx.model;

import android.os.Build;

/**
 * Created by Gongpu on 14-3-22.
 */
public class Constants {
    public static final int MessageType_Bitmap = 10;
    public static final int MessageType_Message = 20;
    public static final int MessageType_FileInfo = 30;
    public static final int MessageType_FindMyDevice = 40;

    //shared preferences keys
    public static final String SharedPreferenceName = "CloudX";
    public static final String SharedPreference_Key_Account = "Account";
    public static final String SharedPreference_Key_Password = "Password";

    /**
     * 调用RemoteDesktopActivity时，需要传入以下参数
     */
    public static final String ParaName_PeerIP = "PeerIP";
    public static final String ParaName_PeerPort = "PeerPort";
    public static final String ParaName_Resolution_Width = "Resolution_Width";
    public static final String ParaName_Resolution_Height = "Resolution_Height";


    public static String deviceName = Build.BRAND + " " + Build.MODEL;

}
