package online.longlian.app.service.app;

import online.longlian.app.pojo.bo.app.McpTaskQueryParamsBO;
import online.longlian.app.pojo.bo.app.McpTaskQueryResultBO;

public interface McpTaskQueryService {

    McpTaskQueryResultBO.TaskList getProjectTasks(McpTaskQueryParamsBO params);

    McpTaskQueryResultBO.TaskContext getTask(McpTaskQueryParamsBO params);
}
