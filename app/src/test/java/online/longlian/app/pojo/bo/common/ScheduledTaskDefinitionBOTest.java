package online.longlian.app.pojo.bo.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScheduledTaskDefinitionBOTest {

    @Test
    void shouldRejectMissingTaskName() {
        assertThatThrownBy(() -> new ScheduledTaskDefinitionBO(null, "清理资源", null, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("任务标识不能为空");
        assertThatThrownBy(() -> new ScheduledTaskDefinitionBO(" ", "清理资源", null, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("任务标识不能为空");
    }

    @Test
    void shouldRejectMissingDescription() {
        assertThatThrownBy(() -> new ScheduledTaskDefinitionBO("cleanup", null, null, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("必须填写描述");
        assertThatThrownBy(() -> new ScheduledTaskDefinitionBO("cleanup", " ", null, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("必须填写描述");
    }

    @Test
    void shouldAllowEnglishDescription() {
        ScheduledTaskDefinitionBO definition = new ScheduledTaskDefinitionBO(
                "cleanup", "resource cleanup", null, false);

        assertThat(definition.getLabel()).isEqualTo("resource cleanup（cleanup）");
    }

    @Test
    void shouldRequireCronWhenAutomaticSchedulingIsEnabled() {
        assertThatThrownBy(() -> new ScheduledTaskDefinitionBO("cleanup", "清理资源", null, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("必须填写 Cron 表达式");
        assertThatThrownBy(() -> new ScheduledTaskDefinitionBO("cleanup", "清理资源", " ", true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("必须填写 Cron 表达式");
    }

    @Test
    void shouldRejectInvalidCronWhenAutomaticSchedulingIsEnabled() {
        assertThatThrownBy(() -> new ScheduledTaskDefinitionBO("cleanup", "清理资源", "invalid", true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cron 表达式无效")
                .hasMessageContaining("cleanup");
    }

    @Test
    void shouldProvideReadableLabelForManualOnlyTask() {
        ScheduledTaskDefinitionBO definition = new ScheduledTaskDefinitionBO("cleanup", "清理资源", null, false);

        assertThat(definition.getLabel()).isEqualTo("清理资源（cleanup）");
        assertThat(definition.getCronExpression()).isNull();
        assertThat(definition.isEnabled()).isFalse();
    }
}
