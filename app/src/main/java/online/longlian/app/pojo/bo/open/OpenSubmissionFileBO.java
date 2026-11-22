package online.longlian.app.pojo.bo.open;

/**
 * 开放面任务提交附件的元信息：无读取链接，读取链接走按次签发接口。
 */
public record OpenSubmissionFileBO(String fileId, String name, String size) {
}
