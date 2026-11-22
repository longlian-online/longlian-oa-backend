package online.longlian.app.pojo.bo.open;

/**
 * 开放面任务提交的单个字段值：text 字段填 text，file 字段填 file，另一个为 null。
 * 复用站内详情的展示结构，附件只给文件标识、名称与大小，不含读取链接（按次另行签发）。
 */
public record OpenSubmissionFieldBO(String key, String label, String type, String text, OpenSubmissionFileBO file) {
}
