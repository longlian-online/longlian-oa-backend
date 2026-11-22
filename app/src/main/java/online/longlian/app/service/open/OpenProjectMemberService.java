package online.longlian.app.service.open;

import online.longlian.app.pojo.bo.open.OpenPageResultBO;
import online.longlian.app.pojo.bo.open.OpenProjectMemberBO;
import online.longlian.app.pojo.bo.open.OpenProjectMemberListParamsBO;

/**
 * 开放面企划成员查询：userId 键集游标，无 updatedSince（成员表 updated_at 未核实，不承诺增量语义）。
 */
public interface OpenProjectMemberService {

    OpenPageResultBO<OpenProjectMemberBO> listProjectMembers(OpenProjectMemberListParamsBO params);
}
