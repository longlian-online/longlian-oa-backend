package online.longlian.app.service.inbox.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.enumeration.InboxTargetType;
import online.longlian.app.mapper.InboxMessageMapper;
import online.longlian.app.mapper.InboxMessageReadMapper;
import online.longlian.app.mapper.OrganizationMemberMapper;
import online.longlian.app.pojo.bo.InboxMessageBO;
import online.longlian.app.pojo.entity.InboxMessage;
import online.longlian.app.pojo.entity.InboxMessageRead;
import online.longlian.app.pojo.entity.OrganizationMember;
import online.longlian.app.pojo.vo.app.InboxMessageVO;
import online.longlian.app.pojo.vo.common.PageResultVO;
import online.longlian.app.service.inbox.InboxMessageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InboxMessageServiceImpl extends ServiceImpl<InboxMessageMapper, InboxMessage> implements InboxMessageService {

    private final InboxMessageMapper inboxMessageMapper;
    private final InboxMessageReadMapper inboxMessageReadMapper;
    private final OrganizationMemberMapper organizationMemberMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendToUser(Long userId, InboxMessageBO bo) {
        InboxMessage message = InboxMessage.builder()
                .id(generateId())
                .type(bo.getType())
                .title(bo.getTitle())
                .content(bo.getContent())
                .targetType(InboxTargetType.USER)
                .targetId(userId)
                .linkType(bo.getLinkType())
                .linkValue(bo.getLinkValue())
                .relatedType(bo.getRelatedType())
                .relatedId(bo.getRelatedId())
                .createdAt(LocalDateTime.now())
                .build();
        inboxMessageMapper.insert(message);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendToOrganization(Long orgId, InboxMessageBO bo) {
        InboxMessage message = InboxMessage.builder()
                .id(generateId())
                .type(bo.getType())
                .title(bo.getTitle())
                .content(bo.getContent())
                .targetType(InboxTargetType.ORGANIZATION)
                .targetId(orgId)
                .linkType(bo.getLinkType())
                .linkValue(bo.getLinkValue())
                .relatedType(bo.getRelatedType())
                .relatedId(bo.getRelatedId())
                .createdAt(LocalDateTime.now())
                .build();
        inboxMessageMapper.insert(message);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(Long messageId, Long userId) {
        LambdaQueryWrapper<InboxMessageRead> queryWrapper = new LambdaQueryWrapper<InboxMessageRead>()
                .eq(InboxMessageRead::getMessageId, messageId)
                .eq(InboxMessageRead::getUserId, userId);
        InboxMessageRead existing = inboxMessageReadMapper.selectOne(queryWrapper);
        if (existing != null) {
            return;
        }
        InboxMessageRead readRecord = InboxMessageRead.builder()
                .id(generateId())
                .messageId(messageId)
                .userId(userId)
                .readAt(LocalDateTime.now())
                .build();
        inboxMessageReadMapper.insert(readRecord);
    }

    @Override
    public PageResultVO<InboxMessageVO> getPage(Long userId, Integer page, Integer size) {
        Page<InboxMessage> resultPage = queryMessagesForUser(userId, page, size);
        List<InboxMessage> messages = resultPage.getRecords();
        if (messages.isEmpty()) {
            return new PageResultVO<>(List.of(), 0L);
        }

        List<Long> messageIds = messages.stream().map(InboxMessage::getId).toList();
        Map<Long, LocalDateTime> readMap = getReadStatus(userId, messageIds);

        List<InboxMessageVO> voList = messages.stream()
                .map(msg -> {
                    InboxMessageVO vo = new InboxMessageVO();
                    vo.setId(msg.getId());
                    vo.setType(msg.getType());
                    vo.setTitle(msg.getTitle());
                    vo.setContent(msg.getContent());
                    vo.setLinkType(msg.getLinkType());
                    vo.setLinkValue(msg.getLinkValue());
                    vo.setRelatedType(msg.getRelatedType());
                    vo.setRelatedId(msg.getRelatedId());
                    vo.setCreatedAt(msg.getCreatedAt());
                    LocalDateTime readAt = readMap.get(msg.getId());
                    vo.setIsRead(readAt != null);
                    vo.setReadAt(readAt);
                    return vo;
                })
                .collect(Collectors.toList());

        return new PageResultVO<>(voList, resultPage.getTotal());
    }

    private Page<InboxMessage> queryMessagesForUser(Long userId, Integer page, Integer size) {
        Page<InboxMessage> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<InboxMessage> queryWrapper = new LambdaQueryWrapper<InboxMessage>()
                .and(w -> {
                    w.eq(InboxMessage::getTargetType, InboxTargetType.USER)
                     .eq(InboxMessage::getTargetId, userId);
                    List<Long> orgIds = getUserOrgIds(userId);
                    if (!orgIds.isEmpty()) {
                        w.or()
                         .eq(InboxMessage::getTargetType, InboxTargetType.ORGANIZATION)
                         .in(InboxMessage::getTargetId, orgIds);
                    }
                })
                .orderByDesc(InboxMessage::getCreatedAt);
        return inboxMessageMapper.selectPage(pageParam, queryWrapper);
    }

    private Map<Long, LocalDateTime> getReadStatus(Long userId, List<Long> messageIds) {
        LambdaQueryWrapper<InboxMessageRead> queryWrapper = new LambdaQueryWrapper<InboxMessageRead>()
                .eq(InboxMessageRead::getUserId, userId)
                .in(InboxMessageRead::getMessageId, messageIds);
        List<InboxMessageRead> readRecords = inboxMessageReadMapper.selectList(queryWrapper);
        return readRecords.stream()
                .collect(Collectors.toMap(InboxMessageRead::getMessageId, InboxMessageRead::getReadAt));
    }

    private List<Long> getUserOrgIds(Long userId) {
        LambdaQueryWrapper<OrganizationMember> queryWrapper = new LambdaQueryWrapper<OrganizationMember>()
                .eq(OrganizationMember::getUserId, userId);
        List<OrganizationMember> members = organizationMemberMapper.selectList(queryWrapper);
        return members.stream()
                .map(OrganizationMember::getOrgId)
                .collect(Collectors.toList());
    }

    private Long generateId() {
        return IdWorker.getId();
    }
}
