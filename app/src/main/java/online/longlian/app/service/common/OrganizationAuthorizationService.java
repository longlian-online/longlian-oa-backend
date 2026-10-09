package online.longlian.app.service.common;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.OrganizationMapper;
import online.longlian.app.pojo.entity.Organization;
import online.longlian.common.enumeration.Status;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrganizationAuthorizationService {
    private final OrganizationMapper organizationMapper;

    /** 入组申请提交时先锁组织行，串行化同一组织的申请查重与写入。 */
    public Organization lockOrganization(Long orgId, boolean requireEnabled) {
        Organization organization = organizationMapper.selectOne(new LambdaQueryWrapper<Organization>()
                .eq(Organization::getId, orgId).last("FOR UPDATE"));
        if (organization == null) throw new AppException(ResultCode.DATA_NOT_EXIT, "组织不存在");
        if (requireEnabled && organization.getStatus() != Status.ENABLED)
            throw new AppException(ResultCode.OPERATION_FAIL, "组织已被禁用");
        return organization;
    }
}
