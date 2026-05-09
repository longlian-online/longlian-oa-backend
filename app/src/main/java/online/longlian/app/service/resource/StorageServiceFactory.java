package online.longlian.app.service.resource;

import online.longlian.common.enumeration.StorageType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class StorageServiceFactory {

    // Spring 会自动把所有 StorageService 的实现类放入这个 Map
    private final Map<StorageType, StorageService> services;

    // 构造函数注入：将 List 转为 Map
    public StorageServiceFactory(List<StorageService> storageServiceList) {
        this.services = storageServiceList.stream()
                .collect(Collectors.toMap(StorageService::getStorageType, Function.identity()));
    }

    public StorageService get(StorageType type) {
        StorageService service = services.get(type);
        if (service == null) {
            throw new IllegalArgumentException("未找到对应的存储策略: " + type);
        }
        return service;
    }
}
