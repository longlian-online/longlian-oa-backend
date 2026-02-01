package online.longlian.app.pojo.dto;

import lombok.Data;

@Data
public class NotificationReqDTO {

    /** 接收人（邮箱 / 手机号 / QQ号） */
    private String receiver;

    /** 标题 */
    private String title;

    /** 内容 */
    private String content;
}