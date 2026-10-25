package online.longlian.app.service.common.impl;

import java.time.Instant;
import online.longlian.app.pojo.vo.common.SystemInfoVO;
import online.longlian.app.service.common.SystemInfoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Service;

@Service
public class SystemInfoServiceImpl implements SystemInfoService {

    private final BuildProperties buildProperties;
    private final String displayName;

    public SystemInfoServiceImpl(BuildProperties buildProperties,
                                 @Value("${longlian.display-name}") String displayName) {
        this.buildProperties = buildProperties;
        this.displayName = displayName;
    }

    @Override
    public SystemInfoVO getSystemInfo() {
        Instant buildTime = buildProperties.getTime();
        return new SystemInfoVO(buildProperties.getVersion(), buildTime == null ? null : buildTime.toString(), displayName);
    }
}
