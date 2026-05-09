package online.longlian.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import online.longlian.app.pojo.entity.Resource;
import org.apache.ibatis.annotations.Mapper;

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

}
