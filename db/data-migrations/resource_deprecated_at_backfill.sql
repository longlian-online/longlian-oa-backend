-- 曾绑定后已废弃、尚未物理删除的资源，用最后更新时间作为保留期起点。
-- 这些行不会被未绑定清理改写，updated_at 就是进入废弃状态的时刻。
UPDATE resource
SET deprecated_at = updated_at
WHERE deleted_at IS NULL
  AND process_status = 2
  AND biz_id <> 0
  AND storage_cleaned_at IS NULL
  AND deprecated_at IS NULL;
