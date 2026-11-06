package online.longlian.app.service.resource.impl;

import lombok.RequiredArgsConstructor;
import online.longlian.app.pojo.bo.common.ActivatedResource;
import online.longlian.app.pojo.bo.common.TaskDraftFileListParamsBO;
import online.longlian.app.pojo.bo.common.TaskDraftFileResultBO;
import online.longlian.app.service.resource.ResourceService;
import online.longlian.app.service.resource.TaskDraftResourceService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TaskDraftResourceServiceImpl implements TaskDraftResourceService {

    private final ResourceService resourceService;

    @Override
    public Map<String, TaskDraftFileResultBO> getTaskDraftFiles(TaskDraftFileListParamsBO params) {
        Map<String, TaskDraftFileResultBO> files = new LinkedHashMap<>();
        Map<Long, ActivatedResource> activatedFiles = resourceService.findActivated(
                "task_draft", params.getDraftId(), params.getOrgId(), params.getFileIds());
        for (Long fileId : params.getFileIds()) {
            ActivatedResource resource = activatedFiles.get(fileId);
            if (resource == null) {
                files.put(fileId.toString(), TaskDraftFileResultBO.builder()
                        .fileId(fileId.toString()).fileName("附件不可用").availability("unavailable").build());
                continue;
            }
            var download = resourceService.signDownload(resource);
            files.put(fileId.toString(), TaskDraftFileResultBO.builder()
                    .fileId(fileId.toString())
                    .fileName(resource.fileName())
                    .fileSize(resource.fileSize())
                    .fileMime(resource.fileMime())
                    .availability("available")
                    .readUrl(download.readUrl())
                    .expiresAt(download.expiresAt())
                    .build());
        }
        return files;
    }
}
