package online.longlian.app.service.app;

import online.longlian.app.pojo.bo.app.ProjectAttachmentCreateParamsBO;
import online.longlian.app.pojo.bo.app.ProjectAttachmentDeleteParamsBO;
import online.longlian.app.pojo.bo.app.ProjectAttachmentDownloadParamsBO;
import online.longlian.app.pojo.bo.app.ProjectAttachmentDownloadResultBO;
import online.longlian.app.pojo.bo.app.ProjectAttachmentListParamsBO;
import online.longlian.app.pojo.bo.app.ProjectAttachmentResultBO;

import java.util.List;

/**
 * 用户端企划附件服务接口。
 * <p>
 * 附件文件元信息复用通用 {@code resource} 表，本服务只维护企划与文件的关联：
 * <ol>
 *   <li><b>附件列表</b>：按上传时间新到旧返回，仅输出已授权文件的元信息，不签发读取链接</li>
 *   <li><b>绑定附件</b>：仅企划负责人可操作，把当前上传人未绑定的 {@code project_attachment} 上传绑定到企划</li>
 *   <li><b>删除附件</b>：仅企划负责人可操作，逻辑删除关联并把文件送入资源保留期</li>
 *   <li><b>下载附件</b>：点击时按原文件名新取签名链接</li>
 * </ol>
 */
public interface ProjectAttachmentService {

    /**
     * 查询企划附件列表，仅组织内可见的企划可访问。
     *
     * @param params 包含企划 ID 和组织 ID 的查询参数
     * @return 按上传时间新到旧的附件元信息列表
     */
    List<ProjectAttachmentResultBO> getProjectAttachments(ProjectAttachmentListParamsBO params);

    /**
     * 把已上传文件绑定为企划附件，仅企划负责人可操作。
     *
     * @param params 包含企划 ID、文件 ID、操作人和组织 ID 的绑定参数
     */
    void createProjectAttachment(ProjectAttachmentCreateParamsBO params);

    /**
     * 逻辑删除企划附件，并把对应文件送入资源保留期，仅企划负责人可操作。
     *
     * @param params 包含企划 ID、附件 ID、操作人和组织 ID 的删除参数
     */
    void deleteProjectAttachment(ProjectAttachmentDeleteParamsBO params);

    /**
     * 获取附件下载链接，仅组织内可见的企划可访问，每次调用重新签发。
     *
     * @param params 包含企划 ID、附件 ID 和组织 ID 的下载参数
     * @return 原文件名读取地址及签名到期时间（Unix 秒）
     */
    ProjectAttachmentDownloadResultBO getProjectAttachmentDownload(ProjectAttachmentDownloadParamsBO params);
}
