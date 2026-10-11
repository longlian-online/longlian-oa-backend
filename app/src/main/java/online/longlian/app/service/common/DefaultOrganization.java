package online.longlian.app.service.common;

/**
 * 用户保存的默认组织。组织当前不可用时仍返回已保存的组织 ID，角色为空。
 * 不参与请求鉴权。
 */
public record DefaultOrganization(Long orgId, String orgRole) {
}
