package online.longlian.app.service.user.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import online.longlian.app.mapper.OrganizationMapper;
import online.longlian.app.pojo.entity.Organization;
import online.longlian.app.service.user.OrganizationService;
import org.springframework.stereotype.Service;

@Service
public class OrganizationServiceImpl extends ServiceImpl<OrganizationMapper, Organization> implements OrganizationService {
}
