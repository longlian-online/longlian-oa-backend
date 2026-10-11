-- 停止旧应用并完成 organization_owner.sql 回填后，经审核显式执行；不由 Atlas 自动删除索引。
-- 旧索引包含软删除历史，会阻止重新入组；有效成员唯一性由新应用的组织行锁维护。
SET @owner_migration_complete = (SELECT COUNT(*) FROM data_migration WHERE filename = 'organization_owner.sql');
SET @membership_release_sql = IF(@owner_migration_complete = 1,
  'ALTER TABLE organization_member DROP INDEX IF EXISTS uk_org_member_user',
  'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''Complete organization_owner.sql before removing the legacy membership index'''
);
PREPARE membership_release FROM @membership_release_sql;
EXECUTE membership_release;
DEALLOCATE PREPARE membership_release;
