package online.longlian.app.pojo.bo.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScheduledTaskDefinitionTest {

    @Test
    void shouldRejectMissingTaskName() {
        assertThatThrownBy(() -> new ScheduledTaskDefinition(null, "清理资源", null, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("任务标识不能为空");
        assertThatThrownBy(() -> new ScheduledTaskDefinition(" ", "清理资源", null, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("任务标识不能为空");
    }

    @Test
    void shouldRejectMissingDescription() {
        assertThatThrownBy(() -> new ScheduledTaskDefinition("cleanup", null, null, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("必须填写描述");
        assertThatThrownBy(() -> new ScheduledTaskDefinition("cleanup", " ", null, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("必须填写描述");
    }

    @Test
    void shouldAllowEnglishDescription() {
        ScheduledTaskDefinition definition = new ScheduledTaskDefinition(
                "cleanup", "resource cleanup", null, false);

        assertThat(definition.getLabel()).isEqualTo("resource cleanup（cleanup）");
    }

    @Test
    void shouldRequireCronWhenAutomaticSchedulingIsEnabled() {
        assertThatThrownBy(() -> new ScheduledTaskDefinition("cleanup", "清理资源", null, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("必须填写 Cron 表达式");
        assertThatThrownBy(() -> new ScheduledTaskDefinition("cleanup", "清理资源", " ", true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("必须填写 Cron 表达式");
    }

    @Test
    void shouldRejectInvalidCronWhenAutomaticSchedulingIsEnabled() {
        assertThatThrownBy(() -> new ScheduledTaskDefinition("cleanup", "清理资源", "invalid", true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cron 表达式无效")
                .hasMessageContaining("cleanup");
    }

    @Test
    void shouldProvideReadableLabelForManualOnlyTask() {
        ScheduledTaskDefinition definition = new ScheduledTaskDefinition("cleanup", "清理资源", null, false);

        assertThat(definition.getLabel()).isEqualTo("清理资源（cleanup）");
        assertThat(definition.getCronExpression()).isNull();
        assertThat(definition.isEnabled()).isFalse();
    }
}
