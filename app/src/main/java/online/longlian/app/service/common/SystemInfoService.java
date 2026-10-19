package online.longlian.app.service.common;

import online.longlian.app.pojo.vo.common.SystemInfoVO;

/**
 * 系统信息服务接口。
 * <p>
 * 为无需登录的公共接口提供系统信息，不依赖用户身份或组织上下文。
 */
public interface SystemInfoService {

    /**
     * 获取当前系统信息。
     * <p>
     * 版本与 UTC 构建时间取自 Maven 构建信息，保留开发版或预发布版后缀；缺失构建时间时返回空值。
     *
     * @return 系统信息，包含后端版本号与构建时间
     */
    SystemInfoVO getSystemInfo();
}
