package online.longlian.app.service.productrelease;

import java.util.List;

public record ProductReleaseContent(
        String version,
        String frontendVersion,
        String backendVersion,
        String title,
        String summary,
        List<String> added,
        List<String> changed,
        List<String> fixed
) {
    public boolean hasChange() {
        return !added.isEmpty() || !changed.isEmpty() || !fixed.isEmpty();
    }

    public boolean samePair(String frontendVersion, String backendVersion) {
        return this.frontendVersion.equals(frontendVersion) && this.backendVersion.equals(backendVersion);
    }
}
