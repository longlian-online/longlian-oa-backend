package online.longlian.app.service.resource;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.constants.RedisConstants;
import online.longlian.app.common.enumeration.FileProcessStatus;
import online.longlian.app.common.enumeration.StorageType;
import online.longlian.app.common.util.SecurityUtil;
import online.longlian.app.mapper.FileStorageMapper;
import online.longlian.app.pojo.bo.PresignedUploadBO;
import online.longlian.app.pojo.dto.common.CreateFileReqDTO;
import online.longlian.app.pojo.entity.Resource;
import online.longlian.app.pojo.vo.common.CreateFileResVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final FileStorageMapper fileStorageMapper;
    private final StorageServiceFactory storageFactory;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${storage.type}")
    private StorageType storageType;

    @Value("${storage.local.upload-url:}")
    private String localAccessPrefix;

    @Value("${storage.cos.url-prefix:}")
    private String cosAccessPrefix;

    public CreateFileResVO create(CreateFileReqDTO createFileReqDTO) {
        // 1. 生成文件ID
        long fileId = IdWorker.getId();

        // 2. 生成存储KEY
        String storageKey = buildStorageKey(createFileReqDTO.getBizType(), fileId, createFileReqDTO.getFileExt());
        Long currentUserId = SecurityUtil.getCurrentUserId();

        // 3. 构建实体
        Resource file = Resource.builder()
                .id(fileId)
                .orgId((Long) redisTemplate.opsForValue().get(RedisConstants.CURRENT_ORG + currentUserId))
//                .storageType(storageType)
                .storageKey(storageKey)
                .fileName(createFileReqDTO.getFileName())
                .fileExt(createFileReqDTO.getFileExt())
                .fileSize(createFileReqDTO.getFileSize())
                .fileMime(createFileReqDTO.getFileMime())
                .bizType(createFileReqDTO.getBizType())
                .bizId(createFileReqDTO.getBizId())
//                .processStatus(FileProcessStatus.UN_PROCESS)
                .isReferenced((byte) 1)
                .creatorId(currentUserId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 4. 获取上传链接
        StorageService storageService = storageFactory.get(storageType);
        PresignedUploadBO uploadBO = storageService.generatePresignedUpload(file);

//        fileStorageMapper.insert(file);

        return new CreateFileResVO(fileId, uploadBO.getUploadUrl(), uploadBO.getKey(), storageType);
    }

    private String buildStorageKey(String bizType, Long fileId, String ext) {
        return bizType + "/" + fileId + "." + ext;
    }

    public String getFileAccessUrl(Long fileId) {
//        Resource fileStorage = fileStorageMapper.selectById(fileId);
//        StorageType type = fileStorage.getStorageType();
//        String key = fileStorage.getStorageKey();
//
//        return switch (type) {
//            case LOCAL -> localAccessPrefix + "/" + key;
//            case OSS -> "";
//            case COS -> cosAccessPrefix + "/" + key;
//        };
        return null;
    }
}
