package online.longlian.app.service.impl;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshSessionStoreTest {

    @Test
    void shouldRotateCurrentReplayPreviousWithinGraceAndRevokeAfterwards() {
        RefreshSessionRecord session = session("current", "previous", 100L);

        assertThat(RefreshSessionStore.decide(session, "current", 100, null))
                .isEqualTo(RefreshSessionStore.RefreshDecision.ROTATE);
        assertThat(RefreshSessionStore.decide(session, "previous", 100, null))
                .isEqualTo(RefreshSessionStore.RefreshDecision.REPLAY);
        assertThat(RefreshSessionStore.decide(session, "previous", 101, null))
                .isEqualTo(RefreshSessionStore.RefreshDecision.REVOKE);
        assertThat(RefreshSessionStore.decide(session, "unknown", 50, null))
                .isEqualTo(RefreshSessionStore.RefreshDecision.REJECT);
    }

    @Test
    void shouldRevokeSessionsCreatedBeforePasswordChange() {
        RefreshSessionRecord session = session("current", null, null);

        assertThat(RefreshSessionStore.decide(session, "current", 50, 10L))
                .isEqualTo(RefreshSessionStore.RefreshDecision.REVOKE);
        assertThat(RefreshSessionStore.decide(session, "current", 50, 9L))
                .isEqualTo(RefreshSessionStore.RefreshDecision.ROTATE);
    }

    @Test
    void shouldRevokePreviousTokenThatHasNoSuccessor() {
        RefreshSessionRecord session = session("current", "previous", 100L);
        session.setSuccessorAccess(null);

        assertThat(RefreshSessionStore.decide(session, "previous", 50, null))
                .isEqualTo(RefreshSessionStore.RefreshDecision.REVOKE);
    }

    private static RefreshSessionRecord session(String current, String previous, Long validUntil) {
        RefreshSessionRecord session = new RefreshSessionRecord();
        session.setCreatedAt(10L);
        session.setCurrentHash(current);
        session.setPreviousHash(previous);
        session.setPreviousValidUntil(validUntil);
        session.setSuccessorAccess("access");
        session.setSuccessorRefresh("refresh");
        return session;
    }
}
