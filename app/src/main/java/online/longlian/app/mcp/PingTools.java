package online.longlian.app.mcp;

import org.springaicommunity.mcp.annotation.McpTool;
import org.springframework.stereotype.Component;

@Component
public class PingTools {

    @McpTool(name = "ping", description = "检查龙联 OA MCP 工具调用是否可用，返回 pong；不查询或修改业务数据。",
            annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false,
                    idempotentHint = true, openWorldHint = false))
    public String ping() {
        return "pong";
    }
}
