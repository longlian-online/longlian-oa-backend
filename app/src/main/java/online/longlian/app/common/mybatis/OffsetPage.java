package online.longlian.app.common.mybatis;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 显式偏移分页：覆盖 IPage#offset()，让分页插件以传入 offset 计算取数窗口，
 * 用于各页条数不一致的分页场景（如首页含创建占位卡、首页少取一条）。
 */
public class OffsetPage<T> extends Page<T> {

    /** 起点（不小于 0 的绝对偏移）；其余行为与传统 Page 一致 */
    private final long windowOffset;

    public OffsetPage(long size, long offset) {
        super(1, size);
        this.windowOffset = Math.max(0, offset);
    }

    @Override
    public long offset() {
        return windowOffset;
    }
}
