package online.longlian.app.service.common.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import online.longlian.app.mapper.OneTimePasswordMapper;
import online.longlian.app.pojo.bo.OneTimePasswordCreateParamsBO;
import online.longlian.app.pojo.entity.OneTimePassword;
import online.longlian.app.service.common.OneTimePasswordService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OneTimePasswordServiceImpl extends ServiceImpl<OneTimePasswordMapper, OneTimePassword> implements OneTimePasswordService {

    private final OneTimePasswordMapper oneTimePasswordMapper;

    @Override
    public OneTimePassword generateOTP(OneTimePasswordCreateParamsBO params) {
        OneTimePassword oneTimePassword = OneTimePassword.builder()
                .code(params.getCode())
                .expiredAt(params.getExpiredAt())
                .bizType(params.getBizType())
                .creatorId(params.getCreatorId())
                .build();
        oneTimePasswordMapper.insert(oneTimePassword);
        return oneTimePassword;
    }

    @Override
    public void useOTP(String code) {
    }
}
