package online.longlian.app.service.app.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.entity.User;
import online.longlian.common.enumeration.InboxNotificationKind;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserNotificationPreferenceServiceImplTest {
    @Mock UserMapper userMapper;
    UserNotificationPreferenceServiceImpl service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), User.class);
        service = new UserNotificationPreferenceServiceImpl(userMapper);
    }

    @Test
    void shouldSkipQueryWhenNobodyIsAsked() {
        assertThat(service.disabledKinds(List.of())).isEmpty();
        assertThat(service.disabledKinds(null)).isEmpty();
        verifyNoInteractions(userMapper);
    }

    @Test
    void shouldReturnOnlyUsersWithADisabledKind() {
        when(userMapper.selectList(any())).thenReturn(List.of(
                User.builder().id(3L).config("{\"notificationKinds\":{\"JOIN_APPLICATION\":false}}").build(),
                User.builder().id(4L).config("{}").build()));

        assertThat(service.disabledKinds(List.of(3L, 4L, 3L)))
                .containsOnlyKeys(3L)
                .extractingByKey(3L)
                .isEqualTo(java.util.Set.of(InboxNotificationKind.JOIN_APPLICATION));
    }
}
