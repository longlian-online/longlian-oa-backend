package online.longlian.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import online.longlian.app.pojo.bo.orgadmin.OrgAdminJoinInviteCodeResultBO;
import online.longlian.app.pojo.entity.OrganizationJoinOtp;
import online.longlian.common.enumeration.OTPStatus;
import online.longlian.common.enumeration.OTPType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

/**
 * <p>
 * 邀请加入组织表 Mapper 接口
 * </p>
 *
 * @author longlian
 * @since 2026-04-20
 */
@Mapper
public interface OrganizationJoinOtpMapper extends BaseMapper<OrganizationJoinOtp> {

    /**
     * 组织归属在关联表，到期和剩余次数在验证码表，用尽还要比较两列。
     * LambdaQueryWrapper 无法在一次分页里表达这个关联。
     */
    @Select("""
            <script>
            SELECT j.id AS id,
                   o.code AS inviteCode,
                   COALESCE(o.use_count, 0) AS useCount,
                   o.max_uses AS maxUses,
                   o.expired_at AS expiredAt,
                   CASE
                       WHEN o.max_uses IS NOT NULL AND COALESCE(o.use_count, 0) &gt;= o.max_uses THEN 'USED'
                       WHEN o.status = #{usedStatus} THEN 'USED'
                       ELSE 'PENDING'
                   END AS status,
                   o.created_at AS createdAt
            FROM organization_join_otp j
            INNER JOIN one_time_password o ON o.id = j.otp_id
            WHERE j.org_id = #{orgId}
              AND j.deleted_at IS NULL
              AND o.biz_type = #{bizType}
              AND (o.expired_at IS NULL OR o.expired_at &gt; #{now})
            <choose>
                <when test="status != null and status.name() == 'PENDING'">
                    AND o.status = #{status}
                    AND (o.max_uses IS NULL OR COALESCE(o.use_count, 0) &lt; o.max_uses)
                </when>
                <when test="status != null and status.name() == 'USED'">
                    AND (o.status = #{status}
                        OR (o.max_uses IS NOT NULL AND COALESCE(o.use_count, 0) &gt;= o.max_uses))
                </when>
            </choose>
            ORDER BY o.created_at DESC, o.id DESC
            </script>
            """)
    Page<OrgAdminJoinInviteCodeResultBO> selectActiveJoinInviteCodePage(
            Page<OrgAdminJoinInviteCodeResultBO> page,
            @Param("orgId") Long orgId,
            @Param("status") OTPStatus status,
            @Param("usedStatus") OTPStatus usedStatus,
            @Param("bizType") OTPType bizType,
            @Param("now") LocalDateTime now);
}
