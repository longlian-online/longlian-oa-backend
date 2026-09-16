package online.longlian.app.pojo.bo.common;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class TokenRevocationSnapshotBOTest {
    @Test
    void shouldMergeEntriesAndKeepLongestExpiry() {
        TokenRevocationSnapshotBO snapshot = TokenRevocationSnapshotBO.builder().build();

        snapshot.merge("sha256:first", 100L);
        snapshot.merge("sha256:first", 50L);
        snapshot.merge("sha256:first", 200L);
        snapshot.merge("sha256:second", 300L);

        assertThat(snapshot.getEntries())
                .containsExactly(
                        TokenRevocationEntryBO.builder().key("sha256:first").expiredAtMillis(200L).build(),
                        TokenRevocationEntryBO.builder().key("sha256:second").expiredAtMillis(300L).build());
    }

    @Test
    void shouldValidateSupportedEntryFormats() {
        TokenRevocationSnapshotBO snapshot = TokenRevocationSnapshotBO.builder()
                .entries(List.of(
                        TokenRevocationEntryBO.builder().key("sha256:token").expiredAtMillis(100L).build(),
                        TokenRevocationEntryBO.builder().key("before:100").expiredAtMillis(100L).build()))
                .build();

        assertThat(snapshot.isValid()).isTrue();
    }

    @Test
    void shouldRejectInvalidEntryValues() {
        TokenRevocationEntryBO unsupported = TokenRevocationEntryBO.builder()
                .key("legacy-token").expiredAtMillis(100L).build();
        TokenRevocationEntryBO missingKey = TokenRevocationEntryBO.builder()
                .expiredAtMillis(100L).build();

        assertThat(TokenRevocationSnapshotBO.builder().entries(List.of(unsupported)).build().isValid()).isFalse();
        assertThat(TokenRevocationSnapshotBO.builder().entries(List.of(missingKey)).build().isValid()).isFalse();
        assertThat(TokenRevocationSnapshotBO.builder().entries(Collections.singletonList(null)).build()
                .isValid()).isFalse();
        assertThat(TokenRevocationSnapshotBO.builder().entries(null).build().isValid()).isFalse();
    }
}
