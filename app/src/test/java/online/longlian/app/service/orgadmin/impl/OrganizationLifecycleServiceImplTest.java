package online.longlian.app.service.orgadmin.impl;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.mapper.*;
import online.longlian.app.pojo.bo.orgadmin.*;
import online.longlian.app.pojo.entity.*;
import online.longlian.app.service.common.*;
import online.longlian.app.service.orgadmin.impl.orgmember.OrganizationMemberPolicy;
import online.longlian.common.enumeration.Status;
import online.longlian.common.service.DistributedLockService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.*;
import org.springframework.transaction.*;
import org.springframework.transaction.support.*;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
class OrganizationLifecycleServiceImplTest {
    private final OrganizationMapper organizations=mock(OrganizationMapper.class);
    private final OrganizationMemberMapper members=mock(OrganizationMemberMapper.class);
    private final UserMapper users=mock(UserMapper.class);
    private final GroupApplicationMapper applications=mock(GroupApplicationMapper.class);
    private final OrganizationAuthorizationService authorization=mock(OrganizationAuthorizationService.class);
    private final LockService locks=mock(LockService.class);
    private final RecordingTransactions transactions=new RecordingTransactions();
    private OrganizationLifecycleServiceImpl service;
    @BeforeEach void setUp(){
        for(Class<?> entity:List.of(User.class,Organization.class,OrganizationMember.class,GroupApplication.class))
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(),""),entity);
        when(locks.tryAcquireOrThrow("org:member:role:1",0,TimeUnit.SECONDS)).thenReturn(mock(DistributedLockService.Lock.class));
        service=new OrganizationLifecycleServiceImpl(organizations,members,users,applications,authorization,
                new OrganizationMemberPolicy(),locks,transactions,Clock.systemUTC());
    }
    @Test void shouldRollbackTransferWhenPromotionFails(){
        when(authorization.requireOwner(1L,1L)).thenReturn(member(1L,"ORG_OWNER"));
        when(members.selectOne(any())).thenReturn(member(2L,"ORG_ADMIN"));
        when(users.selectOne(any())).thenReturn(User.builder().status(Status.ENABLED).build());
        when(members.update(isNull(),any())).thenReturn(1,0);
        assertThatThrownBy(()->service.transferOwnership(OrgMemberTransferOwnershipParamsBO.builder()
                .orgId(1L).operatorUserId(1L).memberId(2L).build())).isInstanceOf(AppException.class);
        assertThat(transactions.rollbacks).isEqualTo(1);
    }
    @Test void shouldRejectOwnerExitBeforeDeleting(){
        when(members.selectOne(any())).thenReturn(member(1L,"ORG_OWNER"));
        assertThatThrownBy(()->service.exitOrganization(OrgMemberExitParamsBO.builder().orgId(1L).operatorUserId(1L).build()))
                .isInstanceOf(AppException.class);
        verify(members,never()).deleteById(any());
    }
    @Test void shouldRemoveMemberWithoutChangingGlobalStatus(){
        when(authorization.requireManager(1L,1L)).thenReturn(member(1L,"ORG_OWNER"));
        when(members.selectOne(any())).thenReturn(member(2L,"ORG_USER"));
        when(members.deleteById(2L)).thenReturn(1);
        service.removeMember(OrgMemberRemoveParamsBO.builder().orgId(1L).operatorUserId(1L).memberId(2L).build());
        verify(users).update(isNull(),argThat(w->((com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<User>)w).getSqlSet().contains("default_org_id")));
    }
    private OrganizationMember member(long id,String role){
        return OrganizationMember.builder().id(id).userId(id).orgId(1L).orgRole(role).status(Status.ENABLED).build();
    }
    static class RecordingTransactions extends AbstractPlatformTransactionManager{
        int rollbacks;
        protected Object doGetTransaction(){return new Object();}
        protected void doBegin(Object o,TransactionDefinition d){}
        protected void doCommit(DefaultTransactionStatus s){}
        protected void doRollback(DefaultTransactionStatus s){rollbacks++;}
    }
}
