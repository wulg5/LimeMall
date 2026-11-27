-- 保留后台隐藏的评价记录，并释放停用或已删除客服遗留的会话。
SET @limemall_ddl = IF(EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'pms_comment' AND column_name = 'hidden'
), 'DO 0', 'ALTER TABLE pms_comment
    ADD COLUMN hidden TINYINT NOT NULL DEFAULT 0 COMMENT ''0公开，1后台隐藏'' AFTER content');
PREPARE limemall_migration FROM @limemall_ddl;
EXECUTE limemall_migration;
DEALLOCATE PREPARE limemall_migration;

UPDATE ums_chat_session s
LEFT JOIN ums_admin a ON a.id = s.agent_id
SET s.agent_id = NULL, s.assign_time = NULL
WHERE s.status = 0 AND s.agent_id IS NOT NULL AND (a.id IS NULL OR a.status <> 1);
