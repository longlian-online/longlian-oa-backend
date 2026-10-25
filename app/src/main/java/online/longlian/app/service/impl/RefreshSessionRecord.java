package online.longlian.app.service.impl;

import lombok.Data;

@Data
public class RefreshSessionRecord {
    private long userId;
    private long createdAt;
    private String currentHash;
    private String previousHash;
    private Long previousValidUntil;
    private String successorAccess;
    private String successorRefresh;
}
