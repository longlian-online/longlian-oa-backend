package online.longlian.app.service.resource;

import cn.hutool.core.util.IdUtil;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.constants.RedisConstants;
import online.longlian.app.common.enumeration.FileProcessStatus;
import online.longlian.app.common.enumeration.StorageType;
import online.longlian.app.common.util.ThreadLocalUtil;
import online.longlian.app.mapper.FileStorageMapper;
import online.longlian.app.pojo.bo.PresignedUploadBO;
import online.longlian.app.pojo.dto.CreateFileReqDTO;
import online.longlian.app.pojo.entity.FileStorage;
import online.longlian.app.pojo.vo.CreateFileResVO;
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
        long fileId = IdUtil.getSnowflakeNextId();

        // 2. 生成存储KEY
        String storageKey = buildStorageKey(createFileReqDTO.getBizType(), fileId, createFileReqDTO.getFileExt());

        // 3. 构建实体
        FileStorage file = FileStorage.builder()
                .id(fileId)
                .orgId((Long) redisTemplate.opsForValue().get(RedisConstants.CURRENT_ORG + ThreadLocalUtil.getUserBO().getId()))
                .storageType(storageType)
                .storageKey(storageKey)
                .fileName(createFileReqDTO.getFileName())
                .fileExt(createFileReqDTO.getFileExt())
                .fileSize(createFileReqDTO.getFileSize())
                .fileMime(createFileReqDTO.getFileMime())
                .bizType(createFileReqDTO.getBizType())
                .bizId(createFileReqDTO.getBizId())
                .processStatus(FileProcessStatus.UN_PROCESS)
                .isReferenced(1)
                .creatorId(ThreadLocalUtil.getUserBO().getId())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 4. 获取上传链接
        StorageService storageService = storageFactory.get(storageType);
        PresignedUploadBO uploadBO = storageService.generatePresignedUpload(file);

        fileStorageMapper.insert(file);

        return new CreateFileResVO(
                String.valueOf(fileId),
                uploadBO.getUploadUrl(),
                uploadBO.getKey(),
                storageType
        );
    }

    private String buildStorageKey(String bizType, Long fileId, String ext) {
        return bizType + "/" + fileId + "." + ext;
    }

    public String getFileAccessUrl(Long fileId) {
        FileStorage fileStorage = fileStorageMapper.selectById(fileId);
        StorageType type = fileStorage.getStorageType();
        String key = fileStorage.getStorageKey();

        return switch (type) {
            case LOCAL -> localAccessPrefix + "/" + key;
            case OSS -> "";
            case COS -> cosAccessPrefix + "/" + key;
        };
    }
}