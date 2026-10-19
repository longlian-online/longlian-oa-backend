package online.longlian.app.service.common.impl;

import lombok.RequiredArgsConstructor;
import java.time.Instant;
import online.longlian.app.pojo.vo.common.SystemInfoVO;
import online.longlian.app.service.common.SystemInfoService;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SystemInfoServiceImpl implements SystemInfoService {

    private final BuildProperties buildProperties;

    @Override
    public SystemInfoVO getSystemInfo() {
        Instant buildTime = buildProperties.getTime();
        return new SystemInfoVO(buildProperties.getVersion(), buildTime == null ? null : buildTime.toString());
    }
}
