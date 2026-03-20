package online.longlian.app.service.user.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import online.longlian.app.mapper.ProjectMapper;
import online.longlian.app.pojo.entity.Project;
import online.longlian.app.service.user.ProjectService;
import org.springframework.stereotype.Service;

@Service
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, Project>  implements ProjectService {
}
