package online.longlian.app.service.system.impl;

import java.time.Instant;
import online.longlian.app.pojo.bo.common.SystemInfoResultBO;
import online.longlian.app.service.system.SystemInfoService;
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
    public SystemInfoResultBO getSystemInfo() {
        Instant buildTime = buildProperties.getTime();
        return new SystemInfoResultBO(buildProperties.getVersion(), buildTime == null ? null : buildTime.toString(), displayName);
    }
}
