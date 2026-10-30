package online.longlian.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import online.longlian.app.pojo.entity.UserProductReleaseState;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * <p>
 * 用户产品版本已读位置 Mapper 接口
 * </p>
 *
 * @author longlian
 * @since 
 */
@Mapper
public interface UserProductReleaseStateMapper extends BaseMapper<UserProductReleaseState> {

    /**
     * 已读时间只允许前进。插入冲突时要用数据库里的原值和本次时间取较晚者，Wrapper 表达不了这条upsert。
     */
    @Insert("""
            INSERT INTO user_product_release_state (user_id, last_seen_published_at)
            VALUES (#{userId}, #{publishedAt})
            ON DUPLICATE KEY UPDATE last_seen_published_at = GREATEST(last_seen_published_at, #{publishedAt})
            """)
    int advanceSeen(@Param("userId") Long userId, @Param("publishedAt") LocalDateTime publishedAt);
}
