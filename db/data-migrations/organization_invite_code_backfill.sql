-- 把仍写在一次性密码和两张扩展表里的组织邀请，收成统一邀请码记录。
-- 记录 ID 沿用原 otp.id，已提交的入组申请仍能按 otp_id 找到同一条邀请。
INSERT INTO organization_invite_code (
    id,
    code,
    biz_type,
    org_id,
    creator_id,
    invitee_user_id,
    expired_at,
    accepted,
    accepted_at,
    org_member_id,
    created_at,
    updated_at
)
SELECT
    otp.id,
    otp.code,
    otp.biz_type,
    ext.org_id,
    otp.creator_id,
    ext.invited_user_id,
    otp.expired_at,
    IF(otp.status = 1 OR otp.used_at IS NOT NULL, 1, 0),
    otp.used_at,
    NULL,
    otp.created_at,
    IFNULL(otp.updated_at, otp.created_at)
FROM one_time_password AS otp
INNER JOIN organization_create_otp AS ext
    ON ext.otp_id = otp.id
    AND ext.deleted_at IS NULL
WHERE otp.biz_type = 1
  AND NOT EXISTS (
      SELECT 1 FROM organization_invite_code AS existing WHERE existing.id = otp.id
  );

INSERT INTO organization_invite_code (
    id,
    code,
    biz_type,
    org_id,
    creator_id,
    invitee_user_id,
    expired_at,
    accepted,
    accepted_at,
    org_member_id,
    created_at,
    updated_at
)
SELECT
    otp.id,
    otp.code,
    otp.biz_type,
    ext.org_id,
    otp.creator_id,
    ext.invited_user_id,
    otp.expired_at,
    IF(otp.status = 1 OR otp.used_at IS NOT NULL, 1, 0),
    otp.used_at,
    ext.org_member_id,
    otp.created_at,
    IFNULL(otp.updated_at, otp.created_at)
FROM one_time_password AS otp
INNER JOIN organization_join_otp AS ext
    ON ext.otp_id = otp.id
    AND ext.deleted_at IS NULL
WHERE otp.biz_type = 2
  AND NOT EXISTS (
      SELECT 1 FROM organization_invite_code AS existing WHERE existing.id = otp.id
  );
