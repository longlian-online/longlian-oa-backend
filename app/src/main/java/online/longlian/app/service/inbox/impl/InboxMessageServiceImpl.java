package online.longlian.app.service.inbox.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.enumeration.InboxTargetType;
import online.longlian.app.mapper.InboxMessageMapper;
import online.longlian.app.mapper.InboxMessageReadMapper;
import online.longlian.app.mapper.OrganizationMemberMapper;
import online.longlian.app.pojo.dto.common.InboxMessageDTO;
import online.longlian.app.pojo.entity.InboxMessage;
import online.longlian.app.pojo.entity.InboxMessageRead;
import online.longlian.app.pojo.entity.OrganizationMember;
import online.longlian.app.pojo.vo.app.InboxMessageVO;
import online.longlian.app.pojo.vo.common.PageResultVO;
import online.longlian.app.service.inbox.InboxMessageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InboxMessageServiceImpl extends ServiceImpl<InboxMessageMapper, InboxMessage> implements InboxMessageService {

    private final InboxMessageMapper inboxMessageMapper;
    private final InboxMessageReadMapper inboxMessageReadMapper;
    private final OrganizationMemberMapper organizationMemberMapper;
    private final Clock clock;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendToUser(Long userId, InboxMessageDTO dto) {
        InboxMessage message = InboxMessage.builder()
                .id(generateId())
                .type(dto.getType())
                .title(dto.getTitle())
                .content(dto.getContent())
                .targetType(InboxTargetType.USER)
                .targetId(userId)
                .linkType(dto.getLinkType())
                .linkValue(dto.getLinkValue())
                .relatedType(dto.getRelatedType())
                .relatedId(dto.getRelatedId())
                .createdAt(LocalDateTime.now(clock))
                .build();
        inboxMessageMapper.insert(message);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendToOrganization(Long orgId, InboxMessageDTO dto) {
        InboxMessage message = InboxMessage.builder()
                .id(generateId())
                .type(dto.getType())
                .title(dto.getTitle())
                .content(dto.getContent())
                .targetType(InboxTargetType.ORGANIZATION)
                .targetId(orgId)
                .linkType(dto.getLinkType())
                .linkValue(dto.getLinkValue())
                .relatedType(dto.getRelatedType())
                .relatedId(dto.getRelatedId())
                .createdAt(LocalDateTime.now(clock))
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
                .readAt(LocalDateTime.now(clock))
                .build();
        inboxMessageReadMapper.insert(readRecord);
    }

    @Override
    public PageResultVO<InboxMessageVO> getPage(Long userId, Integer page, Integer size) {
        List<InboxMessage> messages = getMessagesForUser(userId, page, size);
        if (messages.isEmpty()) {
            return new PageResultVO<>(List.of(), 0L);
        }

        Long total = countMessagesForUser(userId);
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

        return new PageResultVO<>(voList, total);
    }

    private List<InboxMessage> getMessagesForUser(Long userId, Integer page, Integer size) {
        Page<InboxMessage> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<InboxMessage> queryWrapper = new LambdaQueryWrapper<InboxMessage>()
                .and(wrapper -> wrapper
                        .eq(InboxMessage::getTargetType, InboxTargetType.USER)
                        .eq(InboxMessage::getTargetId, userId)
                        .or()
                        .eq(InboxMessage::getTargetType, InboxTargetType.ORGANIZATION)
                        .in(InboxMessage::getTargetId, getUserOrgIds(userId))
                )
                .orderByDesc(InboxMessage::getCreatedAt);
        Page<InboxMessage> resultPage = inboxMessageMapper.selectPage(pageParam, queryWrapper);
        return resultPage.getRecords();
    }

    private Long countMessagesForUser(Long userId) {
        LambdaQueryWrapper<InboxMessage> queryWrapper = new LambdaQueryWrapper<InboxMessage>()
                .and(wrapper -> wrapper
                        .eq(InboxMessage::getTargetType, InboxTargetType.USER)
                        .eq(InboxMessage::getTargetId, userId)
                        .or()
                        .eq(InboxMessage::getTargetType, InboxTargetType.ORGANIZATION)
                        .in(InboxMessage::getTargetId, getUserOrgIds(userId))
                );
        return inboxMessageMapper.selectCount(queryWrapper);
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
        return System.currentTimeMillis();
    }
}
