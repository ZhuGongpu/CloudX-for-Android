package cloudx.model;

import java.util.Date;

/**
 * Created by zhugongpu on 14/11/15.
 */
public class User {

    private static String Token = null;
    //过期时间
    private static Date TokenExpirationTime = null;

    /**
     * 登录，并请求token
     *
     * @param account
     * @param password
     */
    public static void login(String account, String password) {
//TODO
    }

    /**
     * 注册
     *
     * @param account
     * @param password
     */
    public static void register(String account, String password) {
//TODO
    }

    /**
     * 返回现有的token
     * 若已过期，则返回null，需要调用方重新获取
     *
     * @return 结果可能为null
     */
    public static String getToken() {
        if (Token != null && TokenExpirationTime.after(new Date(System.currentTimeMillis())))
            return Token;
        else
            return null;
    }

    /**
     * 向服务器请求新的token，并设置过期时间
     */
    public static void requestNewToken() {
//TODO
    }

}
