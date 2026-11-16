package online.longlian.app.pojo.bo.app.projectworkshop;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 企划最近一次提交的摘要行，供工坊列表装配使用。
 */
@Data
public class ProjectLatestSubmissionBO {

    private Long id;

    private String lastSubmitterUsername;

    private LocalDateTime lastSubmitterAt;
}
