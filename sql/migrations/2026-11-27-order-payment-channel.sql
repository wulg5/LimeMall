-- 为已有订单库增加原支付渠道绑定。先运行结算与积分迁移。
CREATE TABLE IF NOT EXISTS alipay_channel_profile (
    id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL PRIMARY KEY,
    channel_config TEXT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付宝订单对应的历史渠道配置';

SET @limemall_ddl = IF(EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'oms_order' AND column_name = 'payment_profile_id'
), 'DO 0', 'ALTER TABLE oms_order
    ADD COLUMN payment_profile_id VARCHAR(64) DEFAULT NULL COMMENT ''原支付渠道配置编号''');
PREPARE limemall_migration FROM @limemall_ddl;
EXECUTE limemall_migration;
DEALLOCATE PREPARE limemall_migration;
