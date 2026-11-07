package online.longlian.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import online.longlian.app.pojo.entity.Item;
import online.longlian.app.pojo.bo.app.ItemSortUpdateBO;
import online.longlian.app.pojo.bo.orgadmin.OrgFileTypeItemBO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
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

    @Select("""
            SELECT i.id AS item_id,
                   i.title AS item_title,
                   p.id AS project_id,
                   p.title AS project_title
            FROM item i
            INNER JOIN project p ON p.id = i.project_id AND p.deleted_at IS NULL
            WHERE i.deleted_at IS NULL
              AND p.org_id = #{orgId}
              AND p.type_id = #{typeId}
            ORDER BY p.title ASC, p.id ASC, i.sort ASC, i.id ASC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<OrgFileTypeItemBO> selectByProjectType(@Param("orgId") Long orgId,
                                                 @Param("typeId") Long typeId,
                                                 @Param("offset") long offset,
                                                 @Param("limit") int limit);

}
