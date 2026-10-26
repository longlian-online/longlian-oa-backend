package online.longlian.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import online.longlian.app.pojo.entity.Item;
import online.longlian.app.pojo.bo.app.ItemSortUpdateBO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * <p>
 * 项目表 Mapper 接口
 * </p>
 *
 * @author longlian
 * @since 
 */
@Mapper
public interface ItemMapper extends BaseMapper<Item> {

    // 普通 Wrapper 不能在一条 UPDATE 中为各项目绑定不同的顺序值。
    @Update("""
            <script>
            UPDATE item
            SET sort = CASE id
              <foreach collection="items" item="entry">
                WHEN #{entry.id} THEN #{entry.sort}
              </foreach>
              ELSE sort END,
              updated_at = updated_at
            WHERE project_id = #{projectId} AND deleted_at IS NULL
              AND id IN
              <foreach collection="items" item="entry" open="(" separator="," close=")">
                #{entry.id}
              </foreach>
            </script>
            """)
    int updateSorts(@Param("projectId") Long projectId, @Param("items") List<ItemSortUpdateBO> items);

}
