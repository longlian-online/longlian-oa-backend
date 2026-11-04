package online.longlian.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import online.longlian.app.pojo.entity.ProjectAttachment;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 企划附件关联表（文件元信息复用 resource） Mapper 接口
 * </p>
 *
 * @author longlian
 * @since 
 */
@Mapper
public interface ProjectAttachmentMapper extends BaseMapper<ProjectAttachment> {

}
