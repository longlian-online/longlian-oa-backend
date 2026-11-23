package online.longlian.app.pojo.bo.open;

import online.longlian.common.enumeration.ProjectStatus;

import java.time.LocalDateTime;

/**
 * 开放面企划条目：不含创建人头像、不含管理视角字段；封面与缩略图为按次签名的读取链接。
 */
public record OpenProjectBO(Long id, String title, String alias, String coverUrl, String thumbnailUrl,
                            ProjectStatus status, boolean creator, LocalDateTime createdAt, LocalDateTime updatedAt) {
}
