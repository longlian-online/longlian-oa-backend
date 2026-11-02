package online.longlian.app.service.productrelease;

public record ProductReleaseContent(
        String version,
        String frontendVersion,
        String backendVersion,
        String title,
        String summary,
        String content
) {
    public boolean samePair(String frontendVersion, String backendVersion) {
        return this.frontendVersion.equals(frontendVersion) && this.backendVersion.equals(backendVersion);
    }
}
