package online.longlian.app.service.user;

import jakarta.servlet.http.HttpServletRequest;
import online.longlian.app.common.result.Result;
import online.longlian.app.pojo.dto.app.LoginByCodeDTO;
import online.longlian.app.pojo.dto.app.LoginByPwdDTO;
import online.longlian.app.pojo.vo.app.LoginVO;

public interface SessionService {

    Result<LoginVO> loginByPwd(LoginByPwdDTO loginByPwdDTO);

    Result<LoginVO> loginByCode(LoginByCodeDTO loginByCodeDTO);

    Result<Void> logout(HttpServletRequest request);
}
