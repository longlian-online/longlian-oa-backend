package online.longlian.app.service.impl;

import online.longlian.app.pojo.entity.Resource;
import online.longlian.app.mapper.ResourceMapper;
import online.longlian.app.service.ResourceService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 资源存储表 服务实现类
 * </p>
 *
 * @author longlian
 * @since 2026-02-04
 */
@Service
public class ResourceServiceImpl extends ServiceImpl<ResourceMapper, Resource> implements ResourceService {

}
