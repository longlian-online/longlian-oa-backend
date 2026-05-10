package online.longlian.app.service.otp;

import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.common.enumeration.OTPType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 策略工厂 —— 通过 Spring DI 自动收集所有 {@link OTPStrategyService} 实现，按 {@link OTPType} 索引。
 * 新增验证码类型只需加一个 {@code @Service} 实现类，工厂零改动。
 */
@Component
public class OTPServiceFactory {

    private final Map<OTPType, OTPStrategyService> otpCodeServices;

    public OTPServiceFactory(List<OTPStrategyService> strategyList) {
        this.otpCodeServices = Map.copyOf(strategyList.stream()
                .collect(Collectors.toMap(
                    OTPStrategyService::getOtpType,
                    Function.identity()
                )));
    }

    public OTPStrategyService get(OTPType type) {
        OTPStrategyService otpStrategyService = otpCodeServices.get(type);
        if (otpStrategyService == null) {
            throw new AppException(ResultCode.OPERATION_FAIL, "不支持的一次性密码类型: " + type);
        }
        return otpStrategyService;
    }
}
