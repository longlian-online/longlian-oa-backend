package online.longlian.app.pojo.bo.app.project;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.longlian.app.pojo.bo.orgadmin.projecttype.ProjectTypeCustomFieldResultBO;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectTypeInfoResultBO {

    private Long id;

    private String name;

    private List<ProjectTypeCustomFieldResultBO> customFields;
}
