package online.longlian.app.service.orgadmin.file;

import online.longlian.app.pojo.bo.app.project.ProjectAttachmentDownloadResultBO;
import online.longlian.app.pojo.bo.orgadmin.file.OrgFileDiscardParamsBO;
import online.longlian.app.pojo.bo.orgadmin.file.OrgFileDownloadParamsBO;
import online.longlian.app.pojo.bo.orgadmin.file.OrgFileEntriesResultBO;
import online.longlian.app.pojo.bo.orgadmin.file.OrgFileExpansionParamsBO;
import online.longlian.app.pojo.bo.orgadmin.file.OrgFileExpansionResultBO;
import online.longlian.app.pojo.bo.orgadmin.file.OrgFileListParamsBO;

public interface OrgFileService {
    int PAGE_SIZE = 50;

    OrgFileEntriesResultBO list(OrgFileListParamsBO params);

    ProjectAttachmentDownloadResultBO download(OrgFileDownloadParamsBO params);

    OrgFileExpansionResultBO expand(OrgFileExpansionParamsBO params);

    void discard(OrgFileDiscardParamsBO params);
}
