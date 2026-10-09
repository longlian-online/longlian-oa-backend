package online.longlian.app.api.orgadmin;

import io.restassured.response.Response;
import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.service.common.LockService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.SpyBean;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;

class OrgAdminMemberConcurrencyApiTest extends BaseApiTest {
    @SpyBean private LockService locks;

    /** 两个转让都通过初次入口鉴权，但只有第一个提交者保留转让资格。 */
    @Test void shouldCommitOnlyOneOwnershipTransfer() throws Exception {
        prepare();
        String token=loginAs("owner","123456");
        CountDownLatch blocked=new CountDownLatch(1),resume=new CountDownLatch(1);
        blockFirst(blocked,resume);
        try(var executor=Executors.newVirtualThreadPerTaskExecutor()){
            CompletableFuture<Response> stale=CompletableFuture.supplyAsync(
                    ()->authRequest(token,1L).put("/orgadmin/members/3/ownership"),executor);
            try{
                assertThat(blocked.await(15,TimeUnit.SECONDS)).isTrue();
                authRequest(token,1L).put("/orgadmin/members/2/ownership")
                        .then().body("code",org.hamcrest.Matchers.equalTo(ResultCode.SUCCESS.getCode()));
            }finally{resume.countDown();}
            assertThat(stale.get(15,TimeUnit.SECONDS).jsonPath().getInt("code"))
                    .isEqualTo(ResultCode.UNAUTHORIZED_OPERATION.getCode());
        }
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM organization_member WHERE org_role='ORG_OWNER' AND deleted_at IS NULL",Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT org_role FROM organization_member WHERE id=2",String.class)).isEqualTo("ORG_OWNER");
    }

    /** 管理员请求锁前暂停，所有者先提升目标；恢复后不能使用过时的目标角色。 */
    @Test void shouldRejectDisableAfterTargetBecomesAdmin() throws Exception {
        prepare();
        jdbcTemplate.update("UPDATE organization_member SET org_role='ORG_USER' WHERE id=3");
        String owner=loginAs("owner","123456"),admin=loginAs("member2","123456");
        CountDownLatch blocked=new CountDownLatch(1),resume=new CountDownLatch(1);
        blockFirst(blocked,resume);
        try(var executor=Executors.newVirtualThreadPerTaskExecutor()){
            CompletableFuture<Response> stale=CompletableFuture.supplyAsync(
                    ()->authRequest(admin,1L).body(Map.of("status","DISABLED")).patch("/orgadmin/members/3/status"),executor);
            try{
                assertThat(blocked.await(15,TimeUnit.SECONDS)).isTrue();
                authRequest(owner,1L).body(Map.of("orgRole","ORG_ADMIN")).patch("/orgadmin/members/3/role")
                        .then().body("code",org.hamcrest.Matchers.equalTo(ResultCode.SUCCESS.getCode()));
            }finally{resume.countDown();}
            assertThat(stale.get(15,TimeUnit.SECONDS).jsonPath().getInt("code"))
                    .isEqualTo(ResultCode.UNAUTHORIZED_OPERATION.getCode());
        }
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM organization_member WHERE id=3",Integer.class)).isEqualTo(1);
    }

    private void prepare(){
        createUserWithOrganization(1L,"owner","123456","owner@example.com",1L,1L,"ORG_OWNER");
        for(long id=2;id<=3;id++){
            createTestUser(id,"member"+id,"123456","member"+id+"@example.com");
            createOrganizationMember(id,1L,id,"ORG_ADMIN");
        }
    }
    private void blockFirst(CountDownLatch blocked,CountDownLatch resume){
        AtomicBoolean first=new AtomicBoolean(true);
        doAnswer(call->{
            if(first.compareAndSet(true,false)){
                blocked.countDown();
                assertThat(resume.await(15,TimeUnit.SECONDS)).isTrue();
            }
            return call.callRealMethod();
        }).when(locks).tryAcquireOrThrow(eq("org:member:role:1"),eq(0L),eq(TimeUnit.SECONDS));
    }
}
