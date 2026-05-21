package online.longlian.app.service.inbox;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import online.longlian.app.common.enumeration.InboxLinkType;
import online.longlian.app.common.enumeration.InboxMessageType;
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
import online.longlian.app.service.inbox.impl.InboxMessageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 站内信服务单元测试
 *
 * <p>使⽤ Mockito 模拟 Mapper 层，纯逻辑验证 Service 层⾏为，不依赖数据库环境。
 * 覆盖 sendToUser / sendToOrganization / markAsRead / getPage 四个接⼝⽅法的全部场景。</p>
 */
@ExtendWith(MockitoExtension.class)
class InboxMessageServiceTest {

    @Mock
    private InboxMessageMapper inboxMessageMapper;

    @Mock
    private InboxMessageReadMapper inboxMessageReadMapper;

    @Mock
    private OrganizationMemberMapper organizationMemberMapper;

    /** 固定时钟，使 createdAt / readAt 等时间字段可断⾔ */
    private final Clock clock = Clock.fixed(Instant.parse("2026-05-22T10:00:00Z"), ZoneId.systemDefault());

    private InboxMessageServiceImpl inboxMessageService;

    @Captor
    private ArgumentCaptor<InboxMessage> messageCaptor;

    @Captor
    private ArgumentCaptor<InboxMessageRead> readCaptor;

    @BeforeEach
    void setUp() {
        inboxMessageService = new InboxMessageServiceImpl(
                inboxMessageMapper, inboxMessageReadMapper, organizationMemberMapper, clock
        );
    }

    /** 构造通⽤的 InboxMessageBO，各测试可按需覆写 */
    private static InboxMessageBO createBO() {
        return InboxMessageBO.builder()
                .type(InboxMessageType.PROJECT_UPDATE)
                .title("Test Title")
                .content("Test Content")
                .linkType(InboxLinkType.INTERNAL)
                .linkValue("/project/1")
                .relatedType("project")
                .relatedId(1L)
                .build();
    }

    // ==================== sendToUser ====================

    @Test
    void shouldSendToUserInsertUserTargetedMessage() {
        // 验证个⼈消息的 targetType=USER、targetId=接收⼈ ID，所有字段正确映射
        InboxMessageBO bo = createBO();

        inboxMessageService.sendToUser(100L, bo);

        verify(inboxMessageMapper).insert(messageCaptor.capture());
        InboxMessage message = messageCaptor.getValue();
        assertEquals(InboxTargetType.USER, message.getTargetType());
        assertEquals(100L, message.getTargetId());
        assertEquals(InboxMessageType.PROJECT_UPDATE, message.getType());
        assertEquals("Test Title", message.getTitle());
        assertEquals("Test Content", message.getContent());
        assertEquals(InboxLinkType.INTERNAL, message.getLinkType());
        assertEquals("/project/1", message.getLinkValue());
        assertEquals("project", message.getRelatedType());
        assertEquals(1L, message.getRelatedId());
        assertNotNull(message.getCreatedAt());
    }

    // ==================== sendToOrganization ====================

    @Test
    void shouldSendToOrganizationInsertOrganizationTargetedMessage() {
        // 验证组织消息的 targetType=ORGANIZATION、targetId=组织 ID
        InboxMessageBO bo = createBO();

        inboxMessageService.sendToOrganization(200L, bo);

        verify(inboxMessageMapper).insert(messageCaptor.capture());
        InboxMessage message = messageCaptor.getValue();
        assertEquals(InboxTargetType.ORGANIZATION, message.getTargetType());
        assertEquals(200L, message.getTargetId());
    }

    // ==================== markAsRead ====================

    @Test
    void shouldMarkAsReadInsertReadRecordWhenNotRead() {
        // ⾸次阅读，selectOne 返回 null 应插⼊阅读记录
        when(inboxMessageReadMapper.selectOne(any())).thenReturn(null);

        inboxMessageService.markAsRead(1L, 100L);

        verify(inboxMessageReadMapper).insert(readCaptor.capture());
        InboxMessageRead read = readCaptor.getValue();
        assertEquals(1L, read.getMessageId());
        assertEquals(100L, read.getUserId());
        assertNotNull(read.getReadAt());
    }

    @Test
    void shouldSkipInsertWhenMessageAlreadyRead() {
        // 已读消息，selectOne 返回记录不应重复插⼊
        InboxMessageRead existing = InboxMessageRead.builder()
                .id(999L).messageId(1L).userId(100L)
                .readAt(LocalDateTime.now(clock))
                .build();
        when(inboxMessageReadMapper.selectOne(any())).thenReturn(existing);

        inboxMessageService.markAsRead(1L, 100L);

        verify(inboxMessageReadMapper, never()).insert(any());
    }

    // ==================== getPage ====================

    @Test
    void shouldReturnEmptyPageWhenNoMessages() {
        // ⽤户没有消息时返回空列表，total=0
        Page<InboxMessage> page = new Page<>(1, 10);
        page.setRecords(List.of());
        when(organizationMemberMapper.selectList(any())).thenReturn(List.of());
        when(inboxMessageMapper.selectPage(any(), any())).thenReturn(page);

        PageResultVO<InboxMessageVO> result = inboxMessageService.getPage(100L, 1, 10);

        assertTrue(result.getList().isEmpty());
        assertEquals(0L, result.getTotal());
    }

    @Test
    void shouldReturnUserMessagesWithDefaultUnreadStatus() {
        // ⽤户有个⼈消息时正确返回，未读状态下 isRead=false、readAt=null
        Long userId = 100L;
        InboxMessage msg = InboxMessage.builder()
                .id(1L).type(InboxMessageType.PROJECT_UPDATE)
                .title("Title").content("Content")
                .targetType(InboxTargetType.USER).targetId(userId)
                .linkType(InboxLinkType.INTERNAL).linkValue("/path")
                .relatedType("project").relatedId(10L)
                .createdAt(LocalDateTime.now(clock))
                .build();

        Page<InboxMessage> page = new Page<>(1, 10);
        page.setRecords(List.of(msg));

        when(organizationMemberMapper.selectList(any())).thenReturn(List.of());
        when(inboxMessageMapper.selectPage(any(), any())).thenReturn(page);
        when(inboxMessageMapper.selectCount(any())).thenReturn(1L);
        when(inboxMessageReadMapper.selectList(any())).thenReturn(List.of());

        PageResultVO<InboxMessageVO> result = inboxMessageService.getPage(userId, 1, 10);

        assertEquals(1, result.getList().size());
        assertEquals(1L, result.getTotal());
        InboxMessageVO vo = result.getList().getFirst();
        assertEquals(1L, vo.getId());
        assertEquals(InboxMessageType.PROJECT_UPDATE, vo.getType());
        assertEquals("Title", vo.getTitle());
        assertEquals("Content", vo.getContent());
        assertEquals(InboxLinkType.INTERNAL, vo.getLinkType());
        assertEquals("/path", vo.getLinkValue());
        assertEquals("project", vo.getRelatedType());
        assertEquals(10L, vo.getRelatedId());
        assertFalse(vo.getIsRead());
        assertNull(vo.getReadAt());
    }

    @Test
    void shouldIncludeOrganizationMessagesWhenUserHasOrg() {
        // ⽤户加⼊组织后，消息列表包含该组织的站内信
        Long userId = 100L;
        Long orgId = 200L;

        OrganizationMember member = OrganizationMember.builder()
                .id(1L).userId(userId).orgId(orgId).build();
        InboxMessage orgMsg = InboxMessage.builder()
                .id(2L).type(InboxMessageType.PROJECT_UPDATE)
                .title("Org Msg").content("Org Content")
                .targetType(InboxTargetType.ORGANIZATION).targetId(orgId)
                .createdAt(LocalDateTime.now(clock))
                .build();

        Page<InboxMessage> page = new Page<>(1, 10);
        page.setRecords(List.of(orgMsg));

        when(organizationMemberMapper.selectList(any())).thenReturn(List.of(member));
        when(inboxMessageMapper.selectPage(any(), any())).thenReturn(page);
        when(inboxMessageMapper.selectCount(any())).thenReturn(1L);
        when(inboxMessageReadMapper.selectList(any())).thenReturn(List.of());

        PageResultVO<InboxMessageVO> result = inboxMessageService.getPage(userId, 1, 10);

        assertEquals(1, result.getList().size());
        assertEquals("Org Msg", result.getList().getFirst().getTitle());
    }

    @Test
    void shouldMapReadStatusCorrectlyAcrossMessages() {
        // 多条消息各⾃独⽴映射已读/未读状态，不应相互⼲扰
        Long userId = 100L;
        LocalDateTime readAt = LocalDateTime.now(clock);

        InboxMessage msg1 = InboxMessage.builder().id(1L).title("Unread")
                .targetType(InboxTargetType.USER).targetId(userId)
                .type(InboxMessageType.PROJECT_UPDATE).content("C1")
                .createdAt(readAt.minusHours(1)).build();
        InboxMessage msg2 = InboxMessage.builder().id(2L).title("Read")
                .targetType(InboxTargetType.USER).targetId(userId)
                .type(InboxMessageType.PROJECT_UPDATE).content("C2")
                .createdAt(readAt).build();

        Page<InboxMessage> page = new Page<>(1, 10);
        page.setRecords(List.of(msg1, msg2));

        InboxMessageRead readRecord = InboxMessageRead.builder()
                .id(99L).messageId(2L).userId(userId).readAt(readAt).build();

        when(organizationMemberMapper.selectList(any())).thenReturn(List.of());
        when(inboxMessageMapper.selectPage(any(), any())).thenReturn(page);
        when(inboxMessageMapper.selectCount(any())).thenReturn(2L);
        when(inboxMessageReadMapper.selectList(any())).thenReturn(List.of(readRecord));

        PageResultVO<InboxMessageVO> result = inboxMessageService.getPage(userId, 1, 10);

        assertEquals(2, result.getList().size());
        assertFalse(result.getList().get(0).getIsRead());
        assertNull(result.getList().get(0).getReadAt());
        assertTrue(result.getList().get(1).getIsRead());
        assertEquals(readAt, result.getList().get(1).getReadAt());
    }
}
