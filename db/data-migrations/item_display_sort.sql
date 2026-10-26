-- 存量项目保持最新创建优先；只回填新增字段的哨兵值，不覆盖已经维护的顺序。
UPDATE item i
JOIN (
  SELECT id,
         ROW_NUMBER() OVER (
           PARTITION BY project_id ORDER BY created_at DESC, id DESC
         ) AS rn
  FROM item
) ranked ON ranked.id = i.id
SET i.sort = ranked.rn, i.updated_at = i.updated_at
WHERE i.sort = 0;
