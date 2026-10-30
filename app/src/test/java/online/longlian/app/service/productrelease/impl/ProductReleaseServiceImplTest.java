package online.longlian.app.service.productrelease.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.mapper.ProductReleaseChangeMapper;
import online.longlian.app.mapper.ProductReleaseMapper;
import online.longlian.app.mapper.UserProductReleaseStateMapper;
import online.longlian.app.pojo.bo.common.PageParamsBO;
import online.longlian.app.pojo.bo.common.PageResultBO;
import online.longlian.app.pojo.bo.productrelease.ProductReleaseBaselineResultBO;
import online.longlian.app.pojo.bo.productrelease.ProductReleaseDetailResultBO;
import online.longlian.app.pojo.bo.productrelease.ProductReleaseListItemResultBO;
import online.longlian.app.pojo.bo.productrelease.ProductReleaseListParamsBO;
import online.longlian.app.pojo.bo.productrelease.ProductReleaseNoticeResultBO;
import online.longlian.app.pojo.bo.productrelease.ProductReleaseSaveParamsBO;
import online.longlian.app.pojo.bo.productrelease.ProductReleaseUpdateParamsBO;
import online.longlian.app.pojo.entity.ProductRelease;
import online.longlian.app.pojo.entity.ProductReleaseChange;
import online.longlian.app.pojo.entity.UserProductReleaseState;
import online.longlian.app.service.common.LockService;
import online.longlian.common.enumeration.ProductReleaseChangeKind;
import online.longlian.common.enumeration.ProductReleaseStatus;
import online.longlian.common.service.DistributedLockService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductReleaseServiceImplTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-03-21T10:00:00Z"), ZoneId.of("Asia/Shanghai"));

    @Mock
    private ProductReleaseMapper productReleaseMapper;
    @Mock
    private ProductReleaseChangeMapper productReleaseChangeMapper;
    @Mock
    private UserProductReleaseStateMapper userProductReleaseStateMapper;
    @Mock
    private LockService lockService;
    @Mock
    private PlatformTransactionManager transactionManager;

    private ProductReleaseServiceImpl service;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, ProductRelease.class);
        TableInfoHelper.initTableInfo(assistant, ProductReleaseChange.class);
        TableInfoHelper.initTableInfo(assistant, UserProductReleaseState.class);
    }

    @BeforeEach
    void setUp() {
        lenient().when(lockService.tryAcquireOrThrow(any(), anyLong(), any(TimeUnit.class)))
                .thenReturn(mock(DistributedLockService.Lock.class));
        lenient().when(transactionManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        service = new ProductReleaseServiceImpl(
                productReleaseMapper,
                productReleaseChangeMapper,
                userProductReleaseStateMapper,
                lockService,
                transactionManager,
                CLOCK
        );
    }

    @Test
    void shouldKeepDraftWithoutPublishingWhenCreateIsNotPublished() {
        when(productReleaseMapper.selectCount(any())).thenReturn(0L);

        Long id = service.create(saveParams(false));

        assertThat(id).isNotNull();
        verify(productReleaseMapper).insert(any(ProductRelease.class));
        verify(productReleaseChangeMapper).insert(any(ProductReleaseChange.class));
        verify(lockService, never()).tryAcquireOrThrow(any(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void shouldRejectCreateWhenVersionAlreadyExists() {
        when(productReleaseMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.create(saveParams(false)))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("产品版本已存在");
        verify(productReleaseMapper, never()).insert(any(ProductRelease.class));
    }

    @Test
    void shouldReportPublishedPairConflictOnDuplicateKey() {
        when(productReleaseMapper.selectCount(any())).thenReturn(0L);
        when(productReleaseMapper.insert(any(ProductRelease.class))).thenThrow(
                new DuplicateKeyException("Duplicate entry '0.0.18-1.0.52-0' for key 'uk_product_release_published_pair'"));

        assertThatThrownBy(() -> service.create(saveParams(false)))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("这对前端和后端版本已经发布过");
    }

    @Test
    void shouldPublishNewReleaseOnCreateWhenRequested() {
        when(productReleaseMapper.selectCount(any())).thenReturn(0L);
        when(productReleaseMapper.selectPage(any(), any())).thenReturn(pageOf());
        when(productReleaseMapper.update(any(), any())).thenReturn(1);

        service.create(saveParams(true));

        verify(productReleaseMapper).update(any(), any());
    }

    @Test
    void shouldRejectPublishWhenPairMatchesLatest() {
        ProductRelease draft = release(8L, "1.1.0", ProductReleaseStatus.DRAFT, null);
        when(productReleaseMapper.selectById(8L)).thenReturn(draft);
        when(productReleaseChangeMapper.selectList(any())).thenReturn(List.of(change(8L)));
        when(productReleaseMapper.selectPage(any(), any())).thenReturn(
                pageOf(release(3L, "1.0.0", ProductReleaseStatus.PUBLISHED, LocalDateTime.now(CLOCK))));

        assertThatThrownBy(() -> service.publish(8L))
                .isInstanceOf(AppException.class)
                .hasMessage("这次没有更换前端或后端版本");
        verify(productReleaseMapper, never()).update(any(), any());
    }

    @Test
    void shouldRejectPublishWhenDraftIsMissingCopy() {
        when(productReleaseMapper.selectById(8L)).thenReturn(release(8L, "1.1.0", ProductReleaseStatus.DRAFT, null));
        when(productReleaseChangeMapper.selectList(any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.publish(8L))
                .isInstanceOf(AppException.class)
                .hasMessage("至少写一条变更");
    }

    @Test
    void shouldRejectPublishWhenReleaseIsNotDraft() {
        when(productReleaseMapper.selectById(8L)).thenReturn(
                release(8L, "1.1.0", ProductReleaseStatus.PUBLISHED, LocalDateTime.now(CLOCK)));

        assertThatThrownBy(() -> service.publish(8L))
                .isInstanceOf(AppException.class)
                .hasMessage("草稿才能发布");
    }

    @Test
    void shouldRejectPublishWhenPairWasAlreadyPublished() {
        when(productReleaseMapper.selectById(8L)).thenReturn(release(8L, "1.1.0", ProductReleaseStatus.DRAFT, null));
        when(productReleaseChangeMapper.selectList(any())).thenReturn(List.of(change(8L)));
        when(productReleaseMapper.selectPage(any(), any())).thenReturn(pageOf());
        when(productReleaseMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.publish(8L))
                .isInstanceOf(AppException.class)
                .hasMessage("这对前端和后端版本已经发布过");
    }

    @Test
    void shouldRejectPublishWhenDraftChangedBeforeUpdate() {
        when(productReleaseMapper.selectById(8L)).thenReturn(release(8L, "1.1.0", ProductReleaseStatus.DRAFT, null));
        when(productReleaseChangeMapper.selectList(any())).thenReturn(List.of(change(8L)));
        when(productReleaseMapper.selectPage(any(), any())).thenReturn(pageOf());
        when(productReleaseMapper.selectCount(any())).thenReturn(0L);
        when(productReleaseMapper.update(any(), any())).thenReturn(0);

        assertThatThrownBy(() -> service.publish(8L))
                .isInstanceOf(AppException.class)
                .hasMessage("草稿才能发布");
    }

    @Test
    void shouldReportVersionConflictOnPublishDuplicateKey() {
        when(productReleaseMapper.selectById(8L)).thenReturn(release(8L, "1.1.0", ProductReleaseStatus.DRAFT, null));
        when(productReleaseChangeMapper.selectList(any())).thenReturn(List.of(change(8L)));
        when(productReleaseMapper.selectPage(any(), any())).thenReturn(pageOf());
        when(productReleaseMapper.selectCount(any())).thenReturn(0L);
        when(productReleaseMapper.update(any(), any())).thenThrow(
                new DuplicateKeyException("Duplicate entry '1.1.0' for key 'uk_product_release_version'"));

        assertThatThrownBy(() -> service.publish(8L))
                .isInstanceOf(AppException.class)
                .hasMessage("产品版本已存在");
    }

    @Test
    void shouldWithdrawPublishedRelease() {
        when(productReleaseMapper.selectById(9L)).thenReturn(
                release(9L, "1.0.0", ProductReleaseStatus.PUBLISHED, LocalDateTime.now(CLOCK)));
        when(productReleaseMapper.update(any(), any())).thenReturn(1);

        service.withdraw(9L);

        verify(productReleaseMapper).update(any(), any());
    }

    @Test
    void shouldRejectWithdrawWhenReleaseIsNotPublished() {
        when(productReleaseMapper.selectById(9L)).thenReturn(release(9L, "1.0.0", ProductReleaseStatus.DRAFT, null));

        assertThatThrownBy(() -> service.withdraw(9L))
                .isInstanceOf(AppException.class)
                .hasMessage("已发布的产品版本才能撤回");
    }

    @Test
    void shouldRejectWithdrawWhenUpdateAffectsNothing() {
        when(productReleaseMapper.selectById(9L)).thenReturn(
                release(9L, "1.0.0", ProductReleaseStatus.PUBLISHED, LocalDateTime.now(CLOCK)));
        when(productReleaseMapper.update(any(), any())).thenReturn(0);

        assertThatThrownBy(() -> service.withdraw(9L))
                .isInstanceOf(AppException.class)
                .hasMessage("已发布的产品版本才能撤回");
    }

    @Test
    void shouldUpdateDraftBindingAndCopy() {
        when(productReleaseMapper.selectById(7L)).thenReturn(release(7L, "1.1.0", ProductReleaseStatus.DRAFT, null));
        when(productReleaseMapper.selectCount(any())).thenReturn(0L);
        when(productReleaseMapper.update(any(), any())).thenReturn(1);

        service.update(updateParams(7L, "1.2.0"));

        verify(productReleaseMapper).update(any(), any());
        verify(productReleaseChangeMapper).insert(any(ProductReleaseChange.class));
    }

    @Test
    void shouldRejectUpdateOnWithdrawnRelease() {
        when(productReleaseMapper.selectById(7L)).thenReturn(
                release(7L, "1.1.0", ProductReleaseStatus.WITHDRAWN, LocalDateTime.now(CLOCK)));

        assertThatThrownBy(() -> service.update(updateParams(7L, "1.2.0")))
                .isInstanceOf(AppException.class)
                .hasMessage("已撤回的产品版本不能修改");
    }

    @Test
    void shouldRejectBindingChangeOnPublishedRelease() {
        when(productReleaseMapper.selectById(7L)).thenReturn(
                release(7L, "1.1.0", ProductReleaseStatus.PUBLISHED, LocalDateTime.now(CLOCK)));

        assertThatThrownBy(() -> service.update(updateParams(7L, "1.2.0")))
                .isInstanceOf(AppException.class)
                .hasMessage("已发布的产品版本不能修改版本号或两端绑定");
    }

    @Test
    void shouldRejectEmptyCopyOnPublishedRelease() {
        when(productReleaseMapper.selectById(7L)).thenReturn(
                release(7L, "1.1.0", ProductReleaseStatus.PUBLISHED, LocalDateTime.now(CLOCK)));
        ProductReleaseUpdateParamsBO params = updateParams(7L, "1.1.0");
        params.setTitle(" ");
        params.setAdded(List.of());

        assertThatThrownBy(() -> service.update(params))
                .isInstanceOf(AppException.class)
                .hasMessage("标题不能为空");
    }

    @Test
    void shouldKeepPublishedTimestampWhenCopyIsCorrected() {
        LocalDateTime publishedAt = LocalDateTime.of(2026, 3, 20, 10, 0);
        when(productReleaseMapper.selectById(7L)).thenReturn(
                release(7L, "1.1.0", ProductReleaseStatus.PUBLISHED, publishedAt));
        when(productReleaseMapper.selectCount(any())).thenReturn(0L);
        when(productReleaseMapper.update(any(), any())).thenReturn(1);

        service.update(updateParams(7L, "1.1.0"));

        verify(productReleaseMapper, never()).selectPage(any(), any());
    }

    @Test
    void shouldRejectUpdateWhenVersionBelongsToAnotherRelease() {
        when(productReleaseMapper.selectById(7L)).thenReturn(release(7L, "1.1.0", ProductReleaseStatus.DRAFT, null));
        when(productReleaseMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.update(updateParams(7L, "1.2.0")))
                .isInstanceOf(AppException.class)
                .hasMessage("产品版本已存在");
    }

    @Test
    void shouldRejectUnknownRelease() {
        when(productReleaseMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> service.getAdmin(99L))
                .isInstanceOf(AppException.class)
                .hasMessage("产品版本不存在");
    }

    @Test
    void shouldReturnDetailWithGroupedChanges() {
        when(productReleaseMapper.selectById(7L)).thenReturn(release(7L, "1.1.0", ProductReleaseStatus.DRAFT, null));
        when(productReleaseChangeMapper.selectList(any())).thenReturn(List.of(
                change(7L, ProductReleaseChangeKind.ADDED, "一条新增"),
                change(7L, ProductReleaseChangeKind.FIXED, "一条修复")));

        ProductReleaseDetailResultBO detail = service.getAdmin(7L);

        assertThat(detail.getVersion()).isEqualTo("1.1.0");
        assertThat(detail.getFrontendVersion()).isEqualTo("0.0.18");
        assertThat(detail.getAdded()).containsExactly("一条新增");
        assertThat(detail.getFixed()).containsExactly("一条修复");
        assertThat(detail.getChanged()).isEmpty();
    }

    @Test
    void shouldListAdminReleasesWithStatusFilter() {
        when(productReleaseMapper.selectPage(any(), any())).thenReturn(
                pageOf(release(7L, "1.1.0", ProductReleaseStatus.DRAFT, null)));

        PageResultBO<ProductReleaseListItemResultBO> page = service.listAdmin(
                new ProductReleaseListParamsBO(ProductReleaseStatus.DRAFT, new PageParamsBO(1, 10)));

        assertThat(page.getTotal()).isEqualTo(1);
        assertThat(page.getList()).hasSize(1);
        assertThat(page.getList().get(0).getBackendVersion()).isEqualTo("1.0.52");
    }

    @Test
    void shouldReturnBaselineFromLatestPublishedRelease() {
        when(productReleaseMapper.selectPage(any(), any())).thenReturn(
                pageOf(release(3L, "1.0.0", ProductReleaseStatus.PUBLISHED, LocalDateTime.now(CLOCK))));

        ProductReleaseBaselineResultBO baseline = service.baseline();

        assertThat(baseline).isNotNull();
        assertThat(baseline.getFrontendVersion()).isEqualTo("0.0.18");
        assertThat(baseline.getBackendVersion()).isEqualTo("1.0.52");
    }

    @Test
    void shouldReturnNoBaselineWhenNothingIsPublished() {
        when(productReleaseMapper.selectPage(any(), any())).thenReturn(pageOf());

        assertThat(service.baseline()).isNull();
    }

    @Test
    void shouldReturnCurrentReleaseWithOlderCount() {
        LocalDateTime latest = LocalDateTime.of(2026, 3, 21, 18, 0);
        when(productReleaseMapper.selectPage(any(), any())).thenReturn(
                pageOf(release(5L, "1.2.0", ProductReleaseStatus.PUBLISHED, latest)));
        when(productReleaseChangeMapper.selectList(any())).thenReturn(List.of(change(5L)));
        when(productReleaseMapper.selectCount(any())).thenReturn(2L);
        when(userProductReleaseStateMapper.selectById(9L)).thenReturn(null);

        var current = service.current(9L);

        assertThat(current.getRelease()).isNotNull();
        assertThat(current.getRelease().getVersion()).isEqualTo("1.2.0");
        assertThat(current.getOlderCount()).isEqualTo(2);
    }

    @Test
    void shouldReturnNoCurrentWhenNothingIsPublished() {
        when(productReleaseMapper.selectPage(any(), any())).thenReturn(pageOf());

        assertThat(service.current(9L).getRelease()).isNull();
        assertThat(service.current(9L).getOlderCount()).isZero();
    }

    @Test
    void shouldHideCurrentReleaseOnceSeenAtItsPublishTime() {
        LocalDateTime publishedAt = LocalDateTime.of(2026, 3, 21, 18, 0);
        when(productReleaseMapper.selectPage(any(), any())).thenReturn(
                pageOf(release(5L, "1.2.0", ProductReleaseStatus.PUBLISHED, publishedAt)));
        UserProductReleaseState state = new UserProductReleaseState();
        state.setUserId(9L);
        state.setLastSeenPublishedAt(publishedAt);
        when(userProductReleaseStateMapper.selectById(9L)).thenReturn(state);

        assertThat(service.current(9L).getRelease()).isNull();
    }

    @Test
    void shouldListPublishedReleasesWithGroupedChanges() {
        when(productReleaseMapper.selectPage(any(), any())).thenReturn(
                pageOf(release(5L, "1.2.0", ProductReleaseStatus.PUBLISHED, LocalDateTime.now(CLOCK))));
        when(productReleaseChangeMapper.selectList(any())).thenReturn(List.of(
                change(5L, ProductReleaseChangeKind.CHANGED, "一条调整")));

        PageResultBO<ProductReleaseNoticeResultBO> page = service.listPublished(new PageParamsBO(1, 10));

        assertThat(page.getTotal()).isEqualTo(1);
        assertThat(page.getList().get(0).getChanged()).containsExactly("一条调整");
    }

    @Test
    void shouldNotMoveSeenCursorBackwardOrOntoWithdrawnRelease() {
        ProductRelease withdrawn = release(4L, "1.0.0", ProductReleaseStatus.WITHDRAWN, LocalDateTime.now(CLOCK));
        when(productReleaseMapper.selectById(4L)).thenReturn(withdrawn);

        assertThatThrownBy(() -> service.markSeen(9L, 4L))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("更新说明不存在或已撤回");
        verify(userProductReleaseStateMapper, never()).advanceSeen(anyLong(), any());
    }

    @Test
    void shouldRejectSeenForUnknownRelease() {
        when(productReleaseMapper.selectById(4L)).thenReturn(null);

        assertThatThrownBy(() -> service.markSeen(9L, 4L))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("更新说明不存在或已撤回");
    }

    @Test
    void shouldAdvanceSeenToPublishedTime() {
        LocalDateTime publishedAt = LocalDateTime.of(2026, 3, 21, 18, 0);
        when(productReleaseMapper.selectById(5L)).thenReturn(
                release(5L, "1.2.0", ProductReleaseStatus.PUBLISHED, publishedAt));

        service.markSeen(9L, 5L);

        verify(userProductReleaseStateMapper).advanceSeen(9L, publishedAt);
    }

    @Test
    void shouldReportVersionConflictOnUpdateDuplicateKey() {
        when(productReleaseMapper.selectById(7L)).thenReturn(release(7L, "1.1.0", ProductReleaseStatus.DRAFT, null));
        when(productReleaseMapper.selectCount(any())).thenReturn(0L);
        when(productReleaseMapper.update(any(), any())).thenThrow(
                new DuplicateKeyException("Duplicate entry '1.2.0' for key 'uk_product_release_version'"));

        assertThatThrownBy(() -> service.update(updateParams(7L, "1.2.0")))
                .isInstanceOf(AppException.class)
                .hasMessage("产品版本已存在");
    }

    @Test
    void shouldCountOlderUnseenWhenUserAlreadySawAnEarlierRelease() {
        LocalDateTime latest = LocalDateTime.of(2026, 3, 21, 18, 0);
        when(productReleaseMapper.selectPage(any(), any())).thenReturn(
                pageOf(release(5L, "1.2.0", ProductReleaseStatus.PUBLISHED, latest)));
        when(productReleaseChangeMapper.selectList(any())).thenReturn(List.of());
        when(userProductReleaseStateMapper.selectById(9L)).thenReturn(stateAt(9L, latest.minusDays(1)));
        when(productReleaseMapper.selectCount(any())).thenReturn(1L);

        var current = service.current(9L);

        assertThat(current.getRelease().getVersion()).isEqualTo("1.2.0");
        assertThat(current.getOlderCount()).isEqualTo(1);
    }

    @Test
    void shouldReportZeroOlderCountWhenCountIsUnavailable() {
        LocalDateTime latest = LocalDateTime.of(2026, 3, 21, 18, 0);
        when(productReleaseMapper.selectPage(any(), any())).thenReturn(
                pageOf(release(5L, "1.2.0", ProductReleaseStatus.PUBLISHED, latest)));
        when(productReleaseChangeMapper.selectList(any())).thenReturn(List.of());
        when(userProductReleaseStateMapper.selectById(9L)).thenReturn(null);
        when(productReleaseMapper.selectCount(any())).thenReturn(null);

        assertThat(service.current(9L).getOlderCount()).isZero();
    }

    @Test
    void shouldListPublishedWithoutLoadingChanges() {
        when(productReleaseMapper.selectPage(any(), any())).thenReturn(pageOf());

        PageResultBO<ProductReleaseNoticeResultBO> page = service.listPublished(new PageParamsBO(1, 10));

        assertThat(page.getTotal()).isZero();
        assertThat(page.getList()).isEmpty();
    }

    private static ProductReleaseSaveParamsBO saveParams(boolean publish) {
        return ProductReleaseSaveParamsBO.builder()
                .version("1.1.0")
                .frontendVersion("0.0.18")
                .backendVersion("1.0.52")
                .title("标题")
                .summary("摘要")
                .added(List.of("一条新增"))
                .changed(List.of())
                .fixed(List.of())
                .publish(publish)
                .build();
    }

    private static ProductReleaseUpdateParamsBO updateParams(Long id, String version) {
        return ProductReleaseUpdateParamsBO.builder()
                .id(id)
                .version(version)
                .frontendVersion("0.0.18")
                .backendVersion("1.0.52")
                .title("标题")
                .summary("摘要")
                .added(List.of("一条新增"))
                .changed(List.of())
                .fixed(List.of())
                .build();
    }

    private static UserProductReleaseState stateAt(Long userId, LocalDateTime lastSeen) {
        UserProductReleaseState state = new UserProductReleaseState();
        state.setUserId(userId);
        state.setLastSeenPublishedAt(lastSeen);
        return state;
    }

    private static ProductRelease release(Long id, String version, ProductReleaseStatus status, LocalDateTime publishedAt) {
        return ProductRelease.builder()
                .id(id)
                .version(version)
                .frontendVersion("0.0.18")
                .backendVersion("1.0.52")
                .title("标题")
                .summary("摘要")
                .status(status)
                .publishedAt(publishedAt)
                .build();
    }

    private static ProductReleaseChange change(Long releaseId) {
        return change(releaseId, ProductReleaseChangeKind.ADDED, "一条新增");
    }

    private static ProductReleaseChange change(Long releaseId, ProductReleaseChangeKind kind, String content) {
        return ProductReleaseChange.builder()
                .id(1L)
                .releaseId(releaseId)
                .kind(kind)
                .sort(0)
                .content(content)
                .build();
    }

    private static Page<ProductRelease> pageOf(ProductRelease... releases) {
        Page<ProductRelease> page = new Page<>(1, 10, false);
        page.setRecords(List.of(releases));
        page.setTotal(releases.length);
        return page;
    }
}
