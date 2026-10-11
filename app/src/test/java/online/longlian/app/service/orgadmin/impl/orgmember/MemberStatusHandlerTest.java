package online.longlian.app.service.orgadmin.impl.orgmember;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.OrganizationMemberMapper;
import online.longlian.app.pojo.entity.OrganizationMember;
import online.longlian.common.enumeration.Status;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class MemberStatusHandlerTest {
    @Mock private OrganizationMemberMapper members;
    private MemberStatusHandler handler;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), OrganizationMember.class);
        handler = new MemberStatusHandler(members, Clock.systemUTC());
    }

    /** 当前组织查不到目标时，不得继续状态修改。 */
    @Test
    void shouldRejectMissingMember() {
        assertThatThrownBy(() -> handler.getAndValidateMember(99L, 1L)).isInstanceOf(AppException.class)
                .extracting("code").isEqualTo(ResultCode.DATA_NOT_EXIT.getCode());
    }

    /** 同一状态重复请求不改变业务时间戳。 */
    @Test
    void shouldPreserveStateForIdempotentStatusRequest() {
        handler.updateMemberStatus(OrganizationMember.builder().id(1L).status(Status.DISABLED).build(), Status.DISABLED);
        verifyNoInteractions(members);
    }
}
