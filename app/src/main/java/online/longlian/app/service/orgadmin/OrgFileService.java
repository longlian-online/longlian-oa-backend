package online.longlian.app.service.orgadmin;

import online.longlian.app.pojo.bo.app.ProjectAttachmentDownloadResultBO;
import online.longlian.app.pojo.bo.orgadmin.OrgFileDiscardParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgFileDownloadParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgFileEntriesResultBO;
import online.longlian.app.pojo.bo.orgadmin.OrgFileBatchDownloadParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgFileBatchDownloadResultBO;
import online.longlian.app.pojo.bo.orgadmin.OrgFileListParamsBO;

public interface OrgFileService {
    int PAGE_SIZE = 50;

    OrgFileEntriesResultBO list(OrgFileListParamsBO params);

    ProjectAttachmentDownloadResultBO download(OrgFileDownloadParamsBO params);

    OrgFileBatchDownloadResultBO downloadBatch(OrgFileBatchDownloadParamsBO params);

    void discard(OrgFileDiscardParamsBO params);
}
