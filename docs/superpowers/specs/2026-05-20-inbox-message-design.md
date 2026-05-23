# 站内信系统设计文档

## 概述

站内信系统用于系统自动触发的业务通知，支持个人消息和组织广播消息，提供已读/未读状态管理。

## 数据库设计

### inbox_message（消息表）

存储消息内容，广播消息只存1条记录。

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 消息ID |
| type | varchar(50) | 消息类型（PROJECT_UPDATE等） |
| title | varchar(200) | 消息标题 |
| content | text | 消息内容 |
| target_type | tinyint | 目标类型 1-个人 2-组织 |
| target_id | bigint | 目标ID（用户ID或组织ID） |
| link_type | tinyint | 链接类型 1-站内 2-站外 |
| link_value | varchar(500) | 链接值（站内相对路径或站外URL） |
| related_type | varchar(50) | 关联业务类型 |
| related_id | bigint | 关联业务ID |
| created_at | datetime | 创建时间 |

### inbox_message_read（阅读记录表）

存储用户阅读状态，无删除行为，只有已读标记。

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键ID |
| message_id | bigint | 消息ID |
| user_id | bigint | 用户ID |
| read_at | datetime | 阅读时间 |

## 架构设计

### 枚举类

- `InboxMessageType` - 消息类型（初始：PROJECT_UPDATE）
- `InboxTargetType` - 目标类型（USER/ORGANIZATION）
- `InboxLinkType` - 链接类型（INTERNAL/EXTERNAL）

### 核心组件

1. **InboxMessageService** - 消息服务接口
   - `sendToUser()` - 发送给个人
   - `sendToOrganization()` - 发送给组织
   - `markAsRead()` - 标记已读
   - `getPage()` - 分页查询

2. **InboxMessageEvent** - Spring 事件类
   - 包含目标类型、目标ID、消息DTO

3. **InboxMessageEventListener** - 事件监听器
   - `@Async` 异步处理事件
   - 根据目标类型调用对应发送方法

4. **InboxController** - 用户端接口
   - `GET /app/inbox/messages` - 分页消息列表
   - `POST /app/inbox/{messageId}/read` - 标记已读

## 使用方式

业务代码通过 `ApplicationEventPublisher` 发布事件：

```java
eventPublisher.publishEvent(new InboxMessageEvent(
    this,
    InboxTargetType.USER,
    userId,
    InboxMessageDTO.builder()
        .type(InboxMessageType.PROJECT_UPDATE)
        .title("项目更新")
        .content("项目XXX已更新")
        .linkType(InboxLinkType.INTERNAL)
        .linkValue("/projects/123")
        .relatedType("project")
        .relatedId(projectId)
        .build()
));
```

## 文件清单

- `app/src/main/resources/manifest/migrate/v1-1-0.sql` - 数据库迁移
- `app/src/main/java/.../common/enumeration/InboxMessageType.java` - 消息类型枚举
- `app/src/main/java/.../common/enumeration/InboxTargetType.java` - 目标类型枚举
- `app/src/main/java/.../common/enumeration/InboxLinkType.java` - 链接类型枚举
- `app/src/main/java/.../common/event/InboxMessageEvent.java` - 事件类
- `app/src/main/java/.../common/event/InboxMessageEventListener.java` - 事件监听器
- `app/src/main/java/.../pojo/entity/InboxMessage.java` - 消息实体
- `app/src/main/java/.../pojo/entity/InboxMessageRead.java` - 阅读记录实体
- `app/src/main/java/.../pojo/dto/common/InboxMessageDTO.java` - 发送消息DTO
- `app/src/main/java/.../pojo/vo/app/InboxMessageVO.java` - 消息响应VO
- `app/src/main/java/.../mapper/InboxMessageMapper.java` - 消息Mapper
- `app/src/main/java/.../mapper/InboxMessageReadMapper.java` - 阅读记录Mapper
- `app/src/main/java/.../service/inbox/InboxMessageService.java` - 服务接口
- `app/src/main/java/.../service/inbox/impl/InboxMessageServiceImpl.java` - 服务实现
- `app/src/main/java/.../controller/app/InboxController.java` - 用户端控制器
