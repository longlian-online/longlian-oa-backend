package online.longlian.app.service.open;

import lombok.RequiredArgsConstructor;
import online.longlian.app.common.util.OpenCursorCodec;
import online.longlian.app.pojo.bo.app.project.ProjectMemberKeysetParamsBO;
import online.longlian.app.pojo.bo.app.project.ProjectMemberKeysetResultBO;
import online.longlian.app.pojo.bo.open.OpenCursorBO;
import online.longlian.app.pojo.bo.open.OpenPageResultBO;
import online.longlian.app.pojo.bo.open.OpenProjectMemberBO;
import online.longlian.app.pojo.bo.open.OpenProjectMemberListParamsBO;
import online.longlian.app.service.app.project.ProjectService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 开放面企划成员查询。
 * 跨模块只读（standard #9）：开放面是跨领域的查询域，键集游标在本域编码，
 * 成员口径（可见性、过滤、统计）全部走企划域 {@link ProjectService}，本域不写业务表。
 */
@Service
@RequiredArgsConstructor
public class OpenProjectMemberServiceImpl implements OpenProjectMemberService {

    private final ProjectService projectService;

    @Override
    public OpenPageResultBO<OpenProjectMemberBO> listProjectMembers(OpenProjectMemberListParamsBO params) {
        OpenCursorBO cursor = params.cursor();
        Long afterUserId = cursor == null ? null : cursor.afterId();
        ProjectMemberKeysetResultBO page = projectService.getProjectMembersKeyset(
                new ProjectMemberKeysetParamsBO(params.projectId(), params.orgId(), afterUserId, params.limit()));
        List<OpenProjectMemberBO> items = OpenProjectMemberBO.from(page.list());
        if (!page.hasMore() || page.lastUserId() == null) {
            return OpenPageResultBO.of(items, null, false);
        }
        // 成员按 userId 键集，游标不带时间分量
        String nextCursor = OpenCursorCodec.encode(new OpenCursorBO(page.lastUserId(), null));
        return OpenPageResultBO.of(items, nextCursor, true);
    }
}
