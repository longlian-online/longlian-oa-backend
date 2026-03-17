package online.longlian.app.service.resource;

import lombok.RequiredArgsConstructor;
import online.longlian.app.common.enumeration.StorageType;
import online.longlian.app.service.resource.impl.CosStorageService;
import online.longlian.app.service.resource.impl.LocalStorageService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StorageServiceFactory {

    private final LocalStorageService localStorageService;
    private final CosStorageService cosStorageService;

    public StorageService get(StorageType type) {
        return switch (type) {
            case LOCAL -> localStorageService;
            case OSS -> null;
            case COS -> cosStorageService;
        };
    }
}
