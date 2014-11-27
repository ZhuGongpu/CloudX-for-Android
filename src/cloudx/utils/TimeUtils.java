package cloudx.utils;

import java.util.Calendar;

/**
 * Created by zhugongpu on 14/11/13.
 */
public class TimeUtils {
    /**
     * 生成当前UNIX时间戳
     * @return
     */
    public static long getCurrentTimeStamp()
    {
        return Calendar.getInstance().getTimeInMillis();
    }
}
