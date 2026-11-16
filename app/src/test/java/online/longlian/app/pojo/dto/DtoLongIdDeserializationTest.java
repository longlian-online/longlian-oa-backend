package online.longlian.app.pojo.dto;

import com.alibaba.fastjson2.JSON;
import online.longlian.app.pojo.dto.app.OrgIdDTO;
import online.longlian.app.pojo.dto.app.ProjectItemNodeCreateDTO;
import online.longlian.app.pojo.dto.orgadmin.TaskTemplateNodeDTO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 请求 DTO 的 Long 型 ID 不依赖序列化注解，字符串和数字都能绑定为 Long。
 */
class DtoLongIdDeserializationTest {

    @Test
    void shouldReadStringIdAsLong() {
        OrgIdDTO dto = JSON.parseObject("{\"orgId\":\"123\"}", OrgIdDTO.class);

        assertThat(dto.getOrgId()).isEqualTo(123L);
    }

    @Test
    void shouldReadNumericIdAsLong() {
        OrgIdDTO dto = JSON.parseObject("{\"orgId\":123}", OrgIdDTO.class);

        assertThat(dto.getOrgId()).isEqualTo(123L);
    }

    @Test
    void shouldReadNestedStringIdsAsLong() {
        ProjectItemNodeCreateDTO dto = JSON.parseObject("""
                {"baseTaskId":"9","sort":1,"parallelSort":1,"assigneeId":"7"}
                """, ProjectItemNodeCreateDTO.class);

        assertThat(dto.getBaseTaskId()).isEqualTo(9L);
        assertThat(dto.getAssigneeId()).isEqualTo(7L);
    }

    @Test
    void shouldReadOrgAdminStringIdAsLong() {
        TaskTemplateNodeDTO dto = JSON.parseObject("{\"baseTaskId\":\"9\",\"sort\":1}", TaskTemplateNodeDTO.class);

        assertThat(dto.getBaseTaskId()).isEqualTo(9L);
    }
}
