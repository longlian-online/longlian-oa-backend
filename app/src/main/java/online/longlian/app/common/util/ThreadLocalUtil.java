package online.longlian.app.common.util;


import online.longlian.app.pojo.bo.UserBO;

public class ThreadLocalUtil {

    public static ThreadLocal<UserBO> threadLocal = new ThreadLocal<>();

    public static void setUserBO(UserBO UserBO) {
        threadLocal.set(UserBO);
    }

    public static UserBO getUserBO() {
        return threadLocal.get();
    }

    public static void removeUserBO() {
        threadLocal.remove();
    }

}