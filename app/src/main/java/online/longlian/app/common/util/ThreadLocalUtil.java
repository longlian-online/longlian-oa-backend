package online.longlian.app.common.util;

import online.longlian.app.pojo.dto.UserDTO;

public class ThreadLocalUtil {

    public static ThreadLocal<UserDTO> threadLocal = new ThreadLocal<>();

    public static void setUserDTO(UserDTO userDTO) {
        threadLocal.set(userDTO);
    }

    public static UserDTO getUserDTO() {
        return threadLocal.get();
    }

    public static void removeUserDTO() {
        threadLocal.remove();
    }

}