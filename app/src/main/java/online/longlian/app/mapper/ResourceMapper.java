package online.longlian.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import online.longlian.app.pojo.bo.DeprecatedResourceCleanupBO;
import online.longlian.app.pojo.entity.Resource;
import online.longlian.common.enumeration.FileProcessStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 通用文件存储表 Mapper 接口
 * </p>
 *
 * @author longlian
 * @since 2026-04-17
 */
@Mapper
public interface ResourceMapper extends BaseMapper<Resource> {
    @Select("""
            SELECT id, storage_type, storage_key, cleanup_attempts
            FROM resource
            WHERE process_status = #{status}
              AND storage_cleaned_at IS NULL
              AND (cleanup_next_at IS NULL OR cleanup_next_at <= #{executeTime})
            ORDER BY id
            LIMIT #{limit}
            """)
    List<DeprecatedResourceCleanupBO> selectDeprecatedForCleanup(
            @Param("status") FileProcessStatus status,
            @Param("executeTime") LocalDateTime executeTime,
            @Param("limit") int limit
    );

    @Select("""
            SELECT id, storage_type, storage_key, cleanup_attempts
            FROM resource
            WHERE id = #{id}
              AND process_status = #{status}
              AND storage_cleaned_at IS NULL
            """)
    DeprecatedResourceCleanupBO selectDeprecatedForCleanupById(
            @Param("id") Long id,
            @Param("status") FileProcessStatus status
    );

    @Update("""
            UPDATE resource
            SET storage_cleaned_at = #{cleanedAt}, cleanup_next_at = NULL, cleanup_error = NULL
            WHERE id = #{id} AND process_status = #{status} AND storage_cleaned_at IS NULL
            """)
    int markStorageCleaned(
            @Param("id") Long id,
            @Param("status") FileProcessStatus status,
            @Param("cleanedAt") LocalDateTime cleanedAt
    );

    @Update("""
            UPDATE resource
            SET cleanup_attempts = cleanup_attempts + 1,
                cleanup_next_at = #{nextAttemptAt},
                cleanup_error = #{error}
            WHERE id = #{id} AND process_status = #{status} AND storage_cleaned_at IS NULL
            """)
    int recordCleanupFailure(
            @Param("id") Long id,
            @Param("status") FileProcessStatus status,
            @Param("error") String error,
            @Param("nextAttemptAt") LocalDateTime nextAttemptAt
    );

}
