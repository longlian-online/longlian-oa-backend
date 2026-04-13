package online.longlian.app.service.user.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.enumeration.InviteMode;
import online.longlian.app.common.result.Result;
import online.longlian.app.common.util.InviteCodeUtil;
import online.longlian.app.mapper.OrganizationMapper;
import online.longlian.app.pojo.entity.Organization;
import online.longlian.app.pojo.vo.orgadmin.InviteCodeVO;
import online.longlian.app.service.user.OrganizationService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrganizationServiceImpl extends ServiceImpl<OrganizationMapper, Organization> implements OrganizationService {

    private final InviteCodeUtil inviteCodeUtil;
    @Override
    public Result<InviteCodeVO> generateCreateOrgInviteCode() {
        // 超管邀请码用于邀请新用户注册并创建组织。
        InviteCodeVO inviteCodeVO = inviteCodeUtil.generateInviteCode(
                InviteMode.SUPER_ADMIN_CREATE_ORG,
                null,
                null
        );
        return Result.success("生成成功", inviteCodeVO);
    }
}
