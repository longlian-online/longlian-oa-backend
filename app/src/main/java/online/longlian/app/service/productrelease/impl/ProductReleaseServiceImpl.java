package online.longlian.app.service.productrelease.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.ProductReleaseMapper;
import online.longlian.app.mapper.UserProductReleaseStateMapper;
import online.longlian.app.pojo.bo.common.PageParamsBO;
import online.longlian.app.pojo.bo.common.PageResultBO;
import online.longlian.app.pojo.bo.productrelease.ProductReleaseBaselineResultBO;
import online.longlian.app.pojo.bo.productrelease.ProductReleaseCurrentResultBO;
import online.longlian.app.pojo.bo.productrelease.ProductReleaseDetailResultBO;
import online.longlian.app.pojo.bo.productrelease.ProductReleaseListItemResultBO;
import online.longlian.app.pojo.bo.productrelease.ProductReleaseListParamsBO;
import online.longlian.app.pojo.bo.productrelease.ProductReleaseNoticeResultBO;
import online.longlian.app.pojo.bo.productrelease.ProductReleaseSaveParamsBO;
import online.longlian.app.pojo.bo.productrelease.ProductReleaseUpdateParamsBO;
import online.longlian.app.pojo.entity.ProductRelease;
import online.longlian.app.pojo.entity.UserProductReleaseState;
import online.longlian.app.service.common.LockService;
import online.longlian.app.service.productrelease.ProductReleaseContent;
import online.longlian.app.service.productrelease.ProductReleaseRules;
import online.longlian.app.service.productrelease.ProductReleaseService;
import online.longlian.common.enumeration.ProductReleaseStatus;
import online.longlian.common.service.DistributedLockService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class ProductReleaseServiceImpl implements ProductReleaseService {

    private static final String PUBLISH_LOCK = "product-release";

    private final ProductReleaseMapper productReleaseMapper;
    private final UserProductReleaseStateMapper userProductReleaseStateMapper;
    private final LockService lockService;
    private final PlatformTransactionManager transactionManager;
    private final Clock clock;

    @Override
    public Long create(ProductReleaseSaveParamsBO params) {
        ProductReleaseContent content = ProductReleaseRules.normalize(
                params.getVersion(),
                params.getFrontendVersion(),
                params.getBackendVersion(),
                params.getTitle(),
                params.getSummary(),
                params.getContent(),
                params.isPublish()
        );
        if (params.isPublish()) {
            return withPublishLock(() -> insert(content, true));
        }
        return inTransaction(() -> insert(content, false));
    }

    @Override
    public void update(ProductReleaseUpdateParamsBO params) {
        ProductReleaseContent content = ProductReleaseRules.normalize(
                params.getVersion(),
                params.getFrontendVersion(),
                params.getBackendVersion(),
                params.getTitle(),
                params.getSummary(),
                params.getContent(),
                false
        );
        inTransaction(() -> {
            updateExisting(params.getId(), content);
            return null;
        });
    }

    @Override
    public void publish(Long id) {
        withPublishLock(() -> {
            publishExisting(id);
            return null;
        });
    }

    @Override
    public void withdraw(Long id) {
        withPublishLock(() -> {
            withdrawExisting(id);
            return null;
        });
    }

    @Override
    public ProductReleaseDetailResultBO getAdmin(Long id) {
        return toDetail(require(id));
    }

    @Override
    public PageResultBO<ProductReleaseListItemResultBO> listAdmin(ProductReleaseListParamsBO params) {
        LambdaQueryWrapper<ProductRelease> query = new LambdaQueryWrapper<ProductRelease>()
                .orderByDesc(ProductRelease::getUpdatedAt)
                .orderByDesc(ProductRelease::getId);
        if (params.getStatus() != null) {
            query.eq(ProductRelease::getStatus, params.getStatus());
        }
        Page<ProductRelease> page = productReleaseMapper.selectPage(pageOf(params.getPage()), query);
        List<ProductReleaseListItemResultBO> list = page.getRecords().stream()
                .map(release -> ProductReleaseListItemResultBO.builder()
                        .id(release.getId())
                        .version(release.getVersion())
                        .frontendVersion(release.getFrontendVersion())
                        .backendVersion(release.getBackendVersion())
                        .title(release.getTitle())
                        .status(release.getStatus())
                        .publishedAt(release.getPublishedAt())
                        .updatedAt(release.getUpdatedAt())
                        .build())
                .toList();
        return new PageResultBO<>(list, page.getTotal());
    }

    @Override
    public ProductReleaseBaselineResultBO baseline() {
        ProductRelease latest = findLatestPublished();
        if (latest == null) {
            return null;
        }
        return new ProductReleaseBaselineResultBO(latest.getFrontendVersion(), latest.getBackendVersion());
    }

    @Override
    public ProductReleaseCurrentResultBO current(Long userId) {
        ProductRelease latest = findLatestPublished();
        if (latest == null) {
            return ProductReleaseCurrentResultBO.none();
        }
        LocalDateTime seen = seenAt(userId);
        if (seen != null && !latest.getPublishedAt().isAfter(seen)) {
            return ProductReleaseCurrentResultBO.none();
        }
        return new ProductReleaseCurrentResultBO(toNotice(latest), countOlderUnseen(latest, seen));
    }

    @Override
    public PageResultBO<ProductReleaseNoticeResultBO> listPublished(PageParamsBO page) {
        Page<ProductRelease> selected = productReleaseMapper.selectPage(pageOf(page), publishedOrder());
        List<ProductReleaseNoticeResultBO> list = selected.getRecords().stream().map(this::toNotice).toList();
        return new PageResultBO<>(list, selected.getTotal());
    }

    @Override
    public void markSeen(Long userId, Long releaseId) {
        ProductRelease release = productReleaseMapper.selectById(releaseId);
        if (release == null || release.getStatus() != ProductReleaseStatus.PUBLISHED || release.getPublishedAt() == null) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "更新说明不存在或已撤回");
        }
        userProductReleaseStateMapper.advanceSeen(userId, release.getPublishedAt());
    }

    private Long insert(ProductReleaseContent content, boolean publish) {
        ensureVersionAvailable(content.version(), null);
        ProductRelease release = ProductRelease.builder()
                .id(IdWorker.getId())
                .version(content.version())
                .frontendVersion(content.frontendVersion())
                .backendVersion(content.backendVersion())
                .title(content.title())
                .summary(content.summary())
                .content(content.content())
                .status(ProductReleaseStatus.DRAFT)
                .build();
        try {
            productReleaseMapper.insert(release);
        } catch (DuplicateKeyException exception) {
            throw duplicate(exception);
        }
        if (publish) {
            markPublished(release, content);
        }
        return release.getId();
    }

    private void updateExisting(Long id, ProductReleaseContent content) {
        ProductRelease release = require(id);
        if (release.getStatus() == ProductReleaseStatus.WITHDRAWN) {
            throw new AppException(ResultCode.OPERATION_FAIL, "已撤回的产品版本不能修改");
        }
        if (release.getStatus() == ProductReleaseStatus.PUBLISHED) {
            ProductReleaseRules.requirePublishable(content);
            if (bindingChanged(release, content)) {
                throw new AppException(ResultCode.OPERATION_FAIL, "已发布的产品版本不能修改版本号或两端绑定");
            }
        }
        ensureVersionAvailable(content.version(), release.getId());
        LambdaUpdateWrapper<ProductRelease> update = new LambdaUpdateWrapper<ProductRelease>()
                .eq(ProductRelease::getId, release.getId())
                .set(ProductRelease::getTitle, content.title())
                .set(ProductRelease::getSummary, content.summary())
                .set(ProductRelease::getContent, content.content());
        if (release.getStatus() == ProductReleaseStatus.DRAFT) {
            update.set(ProductRelease::getVersion, content.version())
                    .set(ProductRelease::getFrontendVersion, content.frontendVersion())
                    .set(ProductRelease::getBackendVersion, content.backendVersion());
        }
        try {
            productReleaseMapper.update(null, update);
        } catch (DuplicateKeyException exception) {
            throw duplicate(exception);
        }
    }

    private void publishExisting(Long id) {
        ProductRelease release = require(id);
        if (release.getStatus() != ProductReleaseStatus.DRAFT) {
            throw new AppException(ResultCode.OPERATION_FAIL, "草稿才能发布");
        }
        ProductReleaseContent content = ProductReleaseRules.normalize(
                release.getVersion(),
                release.getFrontendVersion(),
                release.getBackendVersion(),
                release.getTitle(),
                release.getSummary(),
                release.getContent(),
                true
        );
        markPublished(release, content);
    }

    private void withdrawExisting(Long id) {
        ProductRelease release = require(id);
        if (release.getStatus() != ProductReleaseStatus.PUBLISHED) {
            throw new AppException(ResultCode.OPERATION_FAIL, "已发布的产品版本才能撤回");
        }
        int updated = productReleaseMapper.update(null, new LambdaUpdateWrapper<ProductRelease>()
                .eq(ProductRelease::getId, release.getId())
                .eq(ProductRelease::getStatus, ProductReleaseStatus.PUBLISHED)
                .set(ProductRelease::getStatus, ProductReleaseStatus.WITHDRAWN));
        if (updated == 0) {
            throw new AppException(ResultCode.OPERATION_FAIL, "已发布的产品版本才能撤回");
        }
    }

    private void markPublished(ProductRelease release, ProductReleaseContent content) {
        ProductRelease latest = findLatestPublished();
        if (latest != null && content.samePair(latest.getFrontendVersion(), latest.getBackendVersion())) {
            throw new AppException(ResultCode.OPERATION_FAIL, "这次没有更换前端或后端版本");
        }
        Long occupied = productReleaseMapper.selectCount(new LambdaQueryWrapper<ProductRelease>()
                .eq(ProductRelease::getStatus, ProductReleaseStatus.PUBLISHED)
                .eq(ProductRelease::getFrontendVersion, content.frontendVersion())
                .eq(ProductRelease::getBackendVersion, content.backendVersion()));
        if (occupied != null && occupied > 0) {
            throw new AppException(ResultCode.OPERATION_FAIL, "这对前端和后端版本已经发布过");
        }
        try {
            int updated = productReleaseMapper.update(null, new LambdaUpdateWrapper<ProductRelease>()
                    .eq(ProductRelease::getId, release.getId())
                    .eq(ProductRelease::getStatus, ProductReleaseStatus.DRAFT)
                    .set(ProductRelease::getStatus, ProductReleaseStatus.PUBLISHED)
                    .set(ProductRelease::getPublishedAt, LocalDateTime.now(clock)));
            if (updated == 0) {
                throw new AppException(ResultCode.OPERATION_FAIL, "草稿才能发布");
            }
        } catch (DuplicateKeyException exception) {
            throw duplicate(exception);
        }
    }

    private void ensureVersionAvailable(String version, Long exceptId) {
        LambdaQueryWrapper<ProductRelease> query = new LambdaQueryWrapper<ProductRelease>()
                .eq(ProductRelease::getVersion, version);
        if (exceptId != null) {
            query.ne(ProductRelease::getId, exceptId);
        }
        Long count = productReleaseMapper.selectCount(query);
        if (count != null && count > 0) {
            throw new AppException(ResultCode.OPERATION_FAIL, "产品版本已存在");
        }
    }

    private boolean bindingChanged(ProductRelease release, ProductReleaseContent content) {
        return !release.getVersion().equals(content.version())
                || !release.getFrontendVersion().equals(content.frontendVersion())
                || !release.getBackendVersion().equals(content.backendVersion());
    }

    private ProductRelease findLatestPublished() {
        Page<ProductRelease> page = productReleaseMapper.selectPage(new Page<>(1, 1, false), publishedOrder());
        return page.getRecords().isEmpty() ? null : page.getRecords().get(0);
    }

    private LambdaQueryWrapper<ProductRelease> publishedOrder() {
        return new LambdaQueryWrapper<ProductRelease>()
                .eq(ProductRelease::getStatus, ProductReleaseStatus.PUBLISHED)
                .orderByDesc(ProductRelease::getPublishedAt)
                .orderByDesc(ProductRelease::getId);
    }

    private int countOlderUnseen(ProductRelease latest, LocalDateTime seen) {
        LambdaQueryWrapper<ProductRelease> query = new LambdaQueryWrapper<ProductRelease>()
                .eq(ProductRelease::getStatus, ProductReleaseStatus.PUBLISHED)
                .and(before -> before
                        .lt(ProductRelease::getPublishedAt, latest.getPublishedAt())
                        .or(tied -> tied
                                .eq(ProductRelease::getPublishedAt, latest.getPublishedAt())
                                .lt(ProductRelease::getId, latest.getId())));
        if (seen != null) {
            query.gt(ProductRelease::getPublishedAt, seen);
        }
        Long count = productReleaseMapper.selectCount(query);
        return count == null ? 0 : Math.toIntExact(count);
    }

    private LocalDateTime seenAt(Long userId) {
        UserProductReleaseState state = userProductReleaseStateMapper.selectById(userId);
        return state == null ? null : state.getLastSeenPublishedAt();
    }

    private ProductRelease require(Long id) {
        ProductRelease release = productReleaseMapper.selectById(id);
        if (release == null) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "产品版本不存在");
        }
        return release;
    }

    private ProductReleaseDetailResultBO toDetail(ProductRelease release) {
        return ProductReleaseDetailResultBO.builder()
                .id(release.getId())
                .version(release.getVersion())
                .frontendVersion(release.getFrontendVersion())
                .backendVersion(release.getBackendVersion())
                .title(release.getTitle())
                .summary(release.getSummary())
                .content(release.getContent())
                .status(release.getStatus())
                .publishedAt(release.getPublishedAt())
                .updatedAt(release.getUpdatedAt())
                .build();
    }

    private ProductReleaseNoticeResultBO toNotice(ProductRelease release) {
        return ProductReleaseNoticeResultBO.builder()
                .id(release.getId())
                .version(release.getVersion())
                .title(release.getTitle())
                .summary(release.getSummary())
                .content(release.getContent())
                .publishedAt(release.getPublishedAt())
                .build();
    }

    private Page<ProductRelease> pageOf(PageParamsBO page) {
        return new Page<>(page.getPageNum(), page.getPageSize());
    }

    /**
     * 发布和撤回要先看到当前已发布版本再写入。锁包住事务，避免事务开始后才拿锁而读到锁前的快照。
     */
    private <T> T withPublishLock(Supplier<T> action) {
        try (DistributedLockService.Lock lock = lockService.tryAcquireOrThrow(PUBLISH_LOCK, 3, TimeUnit.SECONDS)) {
            return inTransaction(action);
        }
    }

    private <T> T inTransaction(Supplier<T> action) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transaction.execute(status -> action.get());
    }

    private AppException duplicate(DuplicateKeyException exception) {
        String message = exception.getMostSpecificCause().getMessage();
        if (message != null && message.contains("uk_product_release_published_pair")) {
            return new AppException(ResultCode.OPERATION_FAIL, "这对前端和后端版本已经发布过");
        }
        return new AppException(ResultCode.OPERATION_FAIL, "产品版本已存在");
    }
}
