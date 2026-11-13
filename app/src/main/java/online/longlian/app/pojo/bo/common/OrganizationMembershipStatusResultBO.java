package online.longlian.app.pojo.bo.common;

public record OrganizationMembershipStatusResultBO(boolean userEnabled, boolean organizationEnabled,
                                                    boolean memberEnabled) {
    public boolean enabled() {
        return userEnabled && organizationEnabled && memberEnabled;
    }
}
