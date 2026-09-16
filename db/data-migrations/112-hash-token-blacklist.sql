-- Stop old application instances before running this MySQL data migration.
-- Preserve the longest revocation when a legacy token and its digest coexist.
START TRANSACTION;

UPDATE token_blacklist AS hashed
JOIN token_blacklist AS legacy
  ON hashed.token = CONCAT('sha256:', SHA2(legacy.token, 256))
SET hashed.expired_at = GREATEST(hashed.expired_at, legacy.expired_at)
WHERE legacy.token LIKE '%.%.%';

DELETE legacy FROM token_blacklist AS legacy
JOIN token_blacklist AS hashed
  ON hashed.token = CONCAT('sha256:', SHA2(legacy.token, 256))
WHERE legacy.token LIKE '%.%.%';

UPDATE token_blacklist
SET token = CONCAT('sha256:', SHA2(token, 256))
WHERE token LIKE '%.%.%';

COMMIT;
