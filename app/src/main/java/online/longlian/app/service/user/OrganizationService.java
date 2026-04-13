package online.longlian.app.service.user;

import com.baomidou.mybatisplus.extension.service.IService;
import online.longlian.app.common.result.Result;
import online.longlian.app.pojo.entity.Organization;
import online.longlian.app.pojo.vo.orgadmin.InviteCodeVO;

public interface OrganizationService extends IService<Organization> {

    Result<InviteCodeVO> generateCreateOrgInviteCode();
}
