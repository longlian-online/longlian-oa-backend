-- 只从有效创建者成员回填所有者；异常组织必须先人工处理，不能任意选择管理员。
SET @organization_owner_errors = (
  SELECT JSON_ARRAYAGG(JSON_OBJECT(
    'org_id', org_id, 'creator_id', creator_id, 'owners', owners,
    'enabled_owners', enabled_owners, 'enabled_creators', enabled_creators,
    'invalid_roles', invalid_roles))
  FROM (
    SELECT o.id AS org_id, o.creator_id,
      SUM(CASE WHEN BINARY m.org_role = 'ORG_OWNER' THEN 1 ELSE 0 END) AS owners,
      SUM(CASE WHEN BINARY m.org_role = 'ORG_OWNER' AND m.status = 1
        AND u.status = 1 THEN 1 ELSE 0 END) AS enabled_owners,
      SUM(CASE WHEN m.user_id = o.creator_id AND m.status = 1 AND u.status = 1
        AND BINARY m.org_role IN ('ORG_ADMIN', 'ORG_USER') THEN 1 ELSE 0 END) AS enabled_creators,
      SUM(CASE WHEN m.id IS NOT NULL AND BINARY m.org_role NOT IN
        ('ORG_OWNER', 'ORG_ADMIN', 'ORG_USER') THEN 1 ELSE 0 END) AS invalid_roles
    FROM organization o
    LEFT JOIN organization_member m ON m.org_id = o.id AND m.deleted_at IS NULL
    LEFT JOIN user u ON u.id = m.user_id AND u.deleted_at IS NULL
    WHERE o.deleted_at IS NULL
    GROUP BY o.id, o.creator_id
    HAVING owners > 1 OR (owners = 1 AND enabled_owners <> 1)
      OR (owners = 0 AND enabled_creators <> 1) OR invalid_roles > 0
  ) AS invalid_organizations
);
SET @organization_member_errors = (
  SELECT JSON_ARRAYAGG(JSON_OBJECT('org_id', org_id, 'user_id', user_id, 'memberships', memberships))
  FROM (
    SELECT m.org_id, m.user_id, COUNT(*) AS memberships
    FROM organization_member m JOIN organization o ON o.id = m.org_id AND o.deleted_at IS NULL
    WHERE m.deleted_at IS NULL
    GROUP BY m.org_id, m.user_id HAVING COUNT(*) > 1
  ) AS duplicate_memberships
);
SELECT @organization_owner_errors AS organization_owner_errors,
  @organization_member_errors AS organization_member_errors;
SET @owner_validation_sql = IF(
  @organization_owner_errors IS NOT NULL OR @organization_member_errors IS NOT NULL,
  'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''Organization owner backfill blocked; inspect organization_owner_errors and organization_member_errors''',
  'DO 0'
);
PREPARE owner_validation FROM @owner_validation_sql;
EXECUTE owner_validation;
DEALLOCATE PREPARE owner_validation;

UPDATE organization_member m
JOIN organization o ON o.id = m.org_id AND o.creator_id = m.user_id AND o.deleted_at IS NULL
SET m.org_role = 'ORG_OWNER'
WHERE m.deleted_at IS NULL AND m.status = 1
  AND NOT EXISTS (
    SELECT 1 FROM organization_member owner
    WHERE owner.org_id = o.id AND owner.deleted_at IS NULL AND BINARY owner.org_role = 'ORG_OWNER'
  );
