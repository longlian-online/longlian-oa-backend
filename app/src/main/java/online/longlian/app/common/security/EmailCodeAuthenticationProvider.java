package online.longlian.app.common.security;

import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.pojo.bo.OTPUseContextBO;
import online.longlian.app.pojo.bo.OTPValidateContextBO;
import online.longlian.app.pojo.entity.OneTimePassword;
import online.longlian.app.service.otp.OTPServiceFactory;
import online.longlian.app.service.otp.OTPStrategyService;
import online.longlian.generator.enumeration.OTPType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailCodeAuthenticationProvider implements AuthenticationProvider {
    private final UserDetailsServiceImpl userDetailsServiceImpl;
    private final OTPServiceFactory otpServiceFactory;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String email = (String) authentication.getPrincipal();
        String code = (String) authentication.getCredentials();

        OTPStrategyService otpStrategyService = otpServiceFactory.get(OTPType.EmailVerify);
        OneTimePassword oneTimePassword = otpStrategyService.getValid(
                OTPValidateContextBO.builder().code(code).target(email).build());

        UserDetails userDetails = userDetailsServiceImpl.loadUserByEmailOnly(email);
        if (!userDetails.isEnabled()) {
            throw new AppException(ResultCode.OPERATION_FAIL, "账号已被禁用");
        }
        otpStrategyService.use(OTPUseContextBO.builder().otpId(oneTimePassword.getId()).build());

        return new EmailCodeAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return EmailCodeAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
