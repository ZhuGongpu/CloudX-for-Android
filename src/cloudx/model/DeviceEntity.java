package cloudx.model;

import common.message.Data;

/**
 * Created by zhugongpu on 14/11/20.
 */
public class DeviceEntity {
    public String deviceName;
    public String AccessPointName;
    public String AccessPointMACAddress;
    public String ipAddress;
    public String MACAddress;
    public DeviceType deviceType;
    public Data.Resolution resolution;
    public int port;

    public enum DeviceType {
        Desktop, MobilePhone, Tablet, Camera, AirConditioner, Socket
    }
}
