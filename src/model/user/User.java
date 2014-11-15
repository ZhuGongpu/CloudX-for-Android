package model.user;

/**
 * Created by zhugongpu on 14/11/15.
 */
public class User {

    private static String token = null;

    /**
     * 登录，并请求token
     *
     * @param account
     * @param password
     */
    public static void login(String account, String password) {

    }

    /**
     * 注册
     *
     * @param account
     * @param password
     */
    public static void register(String account, String password) {

    }

    /**
     * 返回现有的token
     *
     * @return 结果可能为null
     */
    public static String getToken() {
        return token;
    }

    /**
     * 向服务器请求新的token
     */
    public static void requestNewToken() {

    }

}
