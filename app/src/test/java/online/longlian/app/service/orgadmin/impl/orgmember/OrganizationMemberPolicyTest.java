package online.longlian.app.service.orgadmin.impl.orgmember;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.pojo.entity.OrganizationMember;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.assertj.core.api.Assertions.*;
class OrganizationMemberPolicyTest {
    private final OrganizationMemberPolicy policy=new OrganizationMemberPolicy();
    @ParameterizedTest
    @CsvSource({"ORG_OWNER,ORG_ADMIN,true","ORG_OWNER,ORG_USER,true","ORG_OWNER,ORG_OWNER,false",
        "ORG_ADMIN,ORG_USER,true","ORG_ADMIN,ORG_ADMIN,false","ORG_ADMIN,ORG_OWNER,false",
        "ORG_USER,ORG_USER,false","ORG_USER,ORG_ADMIN,false","ORG_USER,ORG_OWNER,false"})
    void shouldEnforceTargetRoleMatrix(String operator,String target,boolean allowed) {
        OrganizationMember actor=member(1L,operator),subject=member(2L,target);
        if(allowed) assertThatCode(()->policy.requireManageableTarget(actor,subject)).doesNotThrowAnyException();
        else assertThatThrownBy(()->policy.requireManageableTarget(actor,subject)).isInstanceOf(AppException.class);
    }
    @Test
    void shouldRejectSelfAndAdminPromotion() {
        assertThatThrownBy(()->policy.requireManageableTarget(member(1L,"ORG_ADMIN"),member(1L,"ORG_ADMIN"))).isInstanceOf(AppException.class);
        assertThatThrownBy(()->policy.requireRoleChange(member(1L,"ORG_ADMIN"),member(2L,"ORG_USER"))).isInstanceOf(AppException.class);
        assertThatCode(()->policy.requireRoleChange(member(1L,"ORG_OWNER"),member(2L,"ORG_ADMIN"))).doesNotThrowAnyException();
    }
    private OrganizationMember member(long userId,String role){return OrganizationMember.builder().userId(userId).orgRole(role).build();}
}
