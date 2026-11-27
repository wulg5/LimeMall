-- 为已有的 LimeMall 数据库增加结算重试、支付查单和原始积分流水字段。
-- 首次部署到已初始化的库时执行一次。新库的 init.sql 已包含这些字段。

SET @limemall_ddl = IF(EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'oms_order' AND column_name = 'checkout_request_id'
), 'DO 0', 'ALTER TABLE oms_order
    ADD COLUMN checkout_request_id CHAR(32) DEFAULT NULL COMMENT ''结算请求号'',
    ADD COLUMN payment_check_time DATETIME DEFAULT NULL COMMENT ''已发起支付的最近查单时间'',
    ADD UNIQUE KEY uk_member_checkout_request (member_id, checkout_request_id),
    ADD KEY idx_payment_check (status, payment_check_time, id)');
PREPARE limemall_migration FROM @limemall_ddl;
EXECUTE limemall_migration;
DEALLOCATE PREPARE limemall_migration;

SET @limemall_ddl = IF(EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'ums_points_log' AND column_name = 'earned_amount'
), 'DO 0', 'ALTER TABLE ums_points_log
    ADD COLUMN earned_amount INT DEFAULT NULL COMMENT ''获取批次的原始入账积分''');
PREPARE limemall_migration FROM @limemall_ddl;
EXECUTE limemall_migration;
DEALLOCATE PREPARE limemall_migration;

-- 只回填没有扣减流水的会员。其他历史批次的原始入账值无法由剩余值可靠推导。
UPDATE ums_points_log AS batch
LEFT JOIN (
    SELECT DISTINCT member_id FROM ums_points_log WHERE type IN (2, 3, 4)
) AS debit ON debit.member_id = batch.member_id
SET batch.earned_amount = batch.amount
WHERE batch.earned_amount IS NULL AND debit.member_id IS NULL
    AND batch.type = 1 AND batch.status = 0 AND batch.amount > 0;
