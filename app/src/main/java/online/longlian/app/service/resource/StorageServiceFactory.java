package online.longlian.app.service.resource;

import online.longlian.app.common.enumeration.ResourceStorageType;
import online.longlian.app.service.resource.impl.CosStorageService;
import online.longlian.app.service.resource.impl.LocalStorageService;
import org.springframework.stereotype.Component;

@Component
public class StorageServiceFactory {

    private final LocalStorageService localStorageService;
    private final CosStorageService cosStorageService;

    public StorageServiceFactory(LocalStorageService localStorageService, CosStorageService cosStorageService) {
        this.localStorageService = localStorageService;
        this.cosStorageService = cosStorageService;
    }

    public StorageService get(ResourceStorageType type) {
        return switch (type) {
            case Local -> localStorageService;
            case COS -> cosStorageService;
        };
    }
}
