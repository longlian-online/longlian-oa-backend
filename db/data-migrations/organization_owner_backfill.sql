-- 创建组织的成员成为该组织唯一所有者。已有所有者的组织不再改动。
UPDATE organization_member AS member
INNER JOIN organization AS org
    ON org.id = member.org_id
    AND org.creator_id = member.user_id
    AND org.deleted_at IS NULL
LEFT JOIN organization_member AS owner
    ON owner.org_id = member.org_id
    AND owner.org_role = 'ORG_OWNER'
    AND owner.deleted_at IS NULL
SET member.org_role = 'ORG_OWNER'
WHERE member.deleted_at IS NULL
  AND owner.id IS NULL
  AND member.org_role <> 'ORG_OWNER';
