-- 已有的加入组织邀请码按一次使用处理，避免可空上限被当成不限次数。
UPDATE `one_time_password`
SET `max_uses` = 1
WHERE `biz_type` = 2
  AND `max_uses` IS NULL;
