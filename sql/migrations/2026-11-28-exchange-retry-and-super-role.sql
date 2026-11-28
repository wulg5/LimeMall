-- 为现有库增加积分兑换请求号，并修复旧版本可禁用超管角色留下的状态。
SET @limemall_ddl = IF(EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'sms_coupon_member' AND column_name = 'exchange_request_id'
), 'DO 0', 'ALTER TABLE sms_coupon_member
    ADD COLUMN exchange_request_id CHAR(32) CHARACTER SET ascii COLLATE ascii_bin DEFAULT NULL COMMENT ''积分兑换请求号'',
    ADD UNIQUE KEY uk_member_exchange_request (member_id, exchange_request_id)');
PREPARE limemall_migration FROM @limemall_ddl;
EXECUTE limemall_migration;
DEALLOCATE PREPARE limemall_migration;

UPDATE ums_role SET status = 1 WHERE id = 1 AND status <> 1;
