package online.longlian.app.service.productrelease.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.mapper.ProductReleaseChangeMapper;
import online.longlian.app.mapper.ProductReleaseMapper;
import online.longlian.app.mapper.UserProductReleaseStateMapper;
import online.longlian.app.pojo.entity.ProductRelease;
import online.longlian.app.pojo.entity.ProductReleaseChange;
import online.longlian.app.pojo.entity.UserProductReleaseState;
import online.longlian.app.service.common.LockService;
import online.longlian.common.enumeration.ProductReleaseChangeKind;
import online.longlian.common.enumeration.ProductReleaseStatus;
import online.longlian.common.service.DistributedLockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import static org.mockito.ArgumentMatchers.eq;
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

    @BeforeEach
    void setUp() {
        lenient().when(lockService.tryAcquireOrThrow(eq("product-release"), eq(3L), eq(TimeUnit.SECONDS)))
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
    void shouldRejectPublishWhenPairMatchesLatest() {
        ProductRelease draft = release(8L, "1.1.0", ProductReleaseStatus.DRAFT, null);
        when(productReleaseMapper.selectById(8L)).thenReturn(draft);
        when(productReleaseChangeMapper.selectList(any())).thenReturn(List.of(change(8L)));
        when(productReleaseMapper.selectPage(any(), any())).thenReturn(pageOf(release(3L, "1.0.0", ProductReleaseStatus.PUBLISHED, LocalDateTime.now(CLOCK))));

        assertThatThrownBy(() -> service.publish(8L))
                .isInstanceOf(AppException.class)
                .hasMessage("这次没有更换前端或后端版本");
        verify(productReleaseMapper, never()).update(any(), any());
    }

    @Test
    void shouldNotMoveSeenCursorBackwardOrOntoWithdrawnRelease() {
        ProductRelease withdrawn = release(4L, "1.0.0", ProductReleaseStatus.WITHDRAWN, LocalDateTime.now(CLOCK));
        when(productReleaseMapper.selectById(4L)).thenReturn(withdrawn);

        assertThatThrownBy(() -> service.markSeen(9L, 4L))
                .isInstanceOf(AppException.class)
                .hasMessage("更新说明不存在或已撤回");
        verify(userProductReleaseStateMapper, never()).advanceSeen(anyLong(), any());
    }

    @Test
    void shouldAdvanceSeenToPublishedTime() {
        LocalDateTime publishedAt = LocalDateTime.of(2026, 3, 21, 18, 0);
        when(productReleaseMapper.selectById(5L)).thenReturn(release(5L, "1.2.0", ProductReleaseStatus.PUBLISHED, publishedAt));

        service.markSeen(9L, 5L);

        verify(userProductReleaseStateMapper).advanceSeen(9L, publishedAt);
    }

    @Test
    void shouldHideCurrentReleaseOnceSeenAtItsPublishTime() {
        LocalDateTime publishedAt = LocalDateTime.of(2026, 3, 21, 18, 0);
        when(productReleaseMapper.selectPage(any(), any())).thenReturn(pageOf(release(5L, "1.2.0", ProductReleaseStatus.PUBLISHED, publishedAt)));
        UserProductReleaseState state = new UserProductReleaseState();
        state.setUserId(9L);
        state.setLastSeenPublishedAt(publishedAt);
        when(userProductReleaseStateMapper.selectById(9L)).thenReturn(state);

        assertThat(service.current(9L).getRelease()).isNull();
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
        return ProductReleaseChange.builder()
                .id(1L)
                .releaseId(releaseId)
                .kind(ProductReleaseChangeKind.ADDED)
                .sort(0)
                .content("一条新增")
                .build();
    }

    private static Page<ProductRelease> pageOf(ProductRelease release) {
        Page<ProductRelease> page = new Page<>(1, 1, false);
        page.setRecords(List.of(release));
        return page;
    }
}
