-- =====================================================================
-- 青柠商城管理系统（LimeMall）数据库初始化脚本
-- 数据库：lime_mall    字符集：utf8mb4    引擎：InnoDB
-- 说明：本脚本可重复执行（先删后建），包含结构定义与基础演示数据。
--       管理员/会员密码为占位符 'INIT'，由后端启动时使用 BCrypt 加密初始化。
--       订单等交易数据由后端 DemoDataInitializer 首次启动时生成，保证日期相对当前时间。
-- =====================================================================

CREATE DATABASE IF NOT EXISTS lime_mall DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE lime_mall;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------------------
-- 1. ums_admin 后台管理员表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `ums_admin`;
CREATE TABLE `ums_admin` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username`    VARCHAR(64)  NOT NULL                COMMENT '登录账号',
  `password`    VARCHAR(100) NOT NULL                COMMENT '密码（BCrypt 加密）',
  `nickname`    VARCHAR(64)  DEFAULT NULL            COMMENT '昵称',
  `avatar`      VARCHAR(500) DEFAULT NULL            COMMENT '头像URL',
  `email`       VARCHAR(100) DEFAULT NULL            COMMENT '邮箱',
  `phone`       VARCHAR(20)  DEFAULT NULL            COMMENT '手机号',
  `status`      TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：1启用 0禁用',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='后台管理员表';

-- ---------------------------------------------------------------------
-- 2. ums_role 角色表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `ums_role`;
CREATE TABLE `ums_role` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name`        VARCHAR(64)  NOT NULL COMMENT '角色名称',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '角色描述',
  `sort`        INT          NOT NULL DEFAULT 0 COMMENT '排序',
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='后台角色表';

-- ---------------------------------------------------------------------
-- 3. ums_menu 菜单权限表
--    type: 0目录 1菜单 2按钮；perm 为接口权限标识，与后端 @PreAuthorize 对应
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `ums_menu`;
CREATE TABLE `ums_menu` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `parent_id`   BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '父级ID（0为顶级）',
  `name`        VARCHAR(64)  NOT NULL COMMENT '名称',
  `path`        VARCHAR(200) NOT NULL DEFAULT '' COMMENT '前端路由地址',
  `icon`        VARCHAR(64)  DEFAULT NULL COMMENT '图标标识',
  `perm`        VARCHAR(100) NOT NULL DEFAULT '' COMMENT '权限标识',
  `type`        TINYINT      NOT NULL DEFAULT 1 COMMENT '类型：0目录 1菜单 2按钮',
  `sort`        INT          NOT NULL DEFAULT 0 COMMENT '排序（同级内降序显示）',
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单权限表';

-- ---------------------------------------------------------------------
-- 4. ums_admin_role 管理员-角色关联表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `ums_admin_role`;
CREATE TABLE `ums_admin_role` (
  `admin_id` BIGINT UNSIGNED NOT NULL COMMENT '管理员ID',
  `role_id`  BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`admin_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员角色关联表';

-- ---------------------------------------------------------------------
-- 5. ums_role_menu 角色-菜单关联表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `ums_role_menu`;
CREATE TABLE `ums_role_menu` (
  `role_id` BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
  `menu_id` BIGINT UNSIGNED NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (`role_id`, `menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

-- ---------------------------------------------------------------------
-- 6. ums_member 商城会员表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `ums_member`;
CREATE TABLE `ums_member` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_no`       VARCHAR(20)  DEFAULT NULL COMMENT '会员业务编号（对外展示，随机不可遍历）',
  `username`      VARCHAR(64)  NOT NULL COMMENT '会员账号',
  `password`      VARCHAR(100) NOT NULL COMMENT '密码（BCrypt 加密）',
  `nickname`      VARCHAR(64)  DEFAULT NULL COMMENT '昵称',
  `phone`         VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
  `avatar`        VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
  `gender`        TINYINT      NOT NULL DEFAULT 0 COMMENT '性别：0未知 1男 2女',
  `birthday`      DATE         DEFAULT NULL COMMENT '生日',
  `level`         TINYINT      NOT NULL DEFAULT 1 COMMENT '会员等级：1普通 2银卡 3金卡 4钻石',
  `points`        INT          NOT NULL DEFAULT 0 COMMENT '积分（站内货币，可兑换优惠券）',
  `growth`        INT          NOT NULL DEFAULT 0 COMMENT '成长值（只增不减，决定会员等级）',
  `status`        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1正常 0禁用',
  `register_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_no` (`user_no`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_phone` (`phone`),
  KEY `idx_register_time` (`register_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商城会员表';

-- ---------------------------------------------------------------------
-- 7. pms_brand 品牌表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `pms_brand`;
CREATE TABLE `pms_brand` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name`        VARCHAR(64)  NOT NULL COMMENT '品牌名称',
  `first_letter` CHAR(1)     DEFAULT NULL COMMENT '首字母',
  `logo`        VARCHAR(500) DEFAULT NULL COMMENT '品牌LOGO',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '品牌介绍',
  `sort`        INT          NOT NULL DEFAULT 0 COMMENT '排序',
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1显示 0隐藏',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品品牌表';

-- ---------------------------------------------------------------------
-- 8. pms_category 商品分类表（两级分类）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `pms_category`;
CREATE TABLE `pms_category` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `parent_id`   BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '父级ID（0为一级分类）',
  `name`        VARCHAR(64)  NOT NULL COMMENT '分类名称',
  `level`       TINYINT      NOT NULL DEFAULT 1 COMMENT '层级：1一级 2二级',
  `icon`        VARCHAR(500) DEFAULT NULL COMMENT '图标',
  `sort`        INT          NOT NULL DEFAULT 0 COMMENT '排序',
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1显示 0隐藏',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

-- ---------------------------------------------------------------------
-- 9. pms_product 商品SPU表
--    stock 为SKU库存汇总冗余字段；delete_status 为逻辑删除标记
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `pms_product`;
CREATE TABLE `pms_product` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name`           VARCHAR(200) NOT NULL COMMENT '商品名称',
  `sub_title`      VARCHAR(255) DEFAULT NULL COMMENT '副标题/卖点',
  `category_id`    BIGINT UNSIGNED NOT NULL COMMENT '分类ID（二级分类）',
  `brand_id`       BIGINT UNSIGNED DEFAULT NULL COMMENT '品牌ID',
  `main_pic`       VARCHAR(500) DEFAULT NULL COMMENT '主图URL',
  `album`          VARCHAR(1000) DEFAULT NULL COMMENT '画册图片URL（逗号分隔）',
  `price`          DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '展示价（取SKU最低价）',
  `original_price` DECIMAL(10,2) DEFAULT NULL COMMENT '划线原价',
  `stock`          INT          NOT NULL DEFAULT 0 COMMENT '总库存（SKU汇总）',
  `sales`          INT          NOT NULL DEFAULT 0 COMMENT '累计销量',
  `unit`           VARCHAR(16)  NOT NULL DEFAULT '件' COMMENT '单位',
  `detail_html`    TEXT         COMMENT '商品详情（富文本）',
  `sort`           INT          NOT NULL DEFAULT 0 COMMENT '排序',
  `publish_status` TINYINT      NOT NULL DEFAULT 0 COMMENT '上架状态：1上架 0下架',
  `delete_status`  TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除 0未删除',
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category_id`),
  KEY `idx_brand` (`brand_id`),
  KEY `idx_publish_status` (`publish_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品SPU表';

-- ---------------------------------------------------------------------
-- 10. pms_sku 商品SKU表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `pms_sku`;
CREATE TABLE `pms_sku` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `product_id`  BIGINT UNSIGNED NOT NULL COMMENT '所属商品ID',
  `sku_code`    VARCHAR(64)  NOT NULL COMMENT 'SKU编码',
  `specs`       VARCHAR(255) NOT NULL COMMENT '规格JSON，如[{"key":"颜色","value":"云雾白"}]',
  `price`       DECIMAL(10,2) NOT NULL COMMENT 'SKU售价',
  `stock`       INT          NOT NULL DEFAULT 0 COMMENT 'SKU库存',
  `sales`       INT          NOT NULL DEFAULT 0 COMMENT 'SKU销量',
  `pic`         VARCHAR(500) DEFAULT NULL COMMENT 'SKU图片',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sku_code` (`sku_code`),
  KEY `idx_product` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品SKU表';

-- ---------------------------------------------------------------------
-- 11. oms_order 订单表
--     状态机：0待付款 → 1待发货 → 2已发货 → 3已完成；0/1 → 4已关闭（取消/退款）
--     收货信息为下单时快照，避免会员资料变更影响历史订单
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `oms_order`;
CREATE TABLE `oms_order` (
  `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_sn`         VARCHAR(32)  NOT NULL COMMENT '订单编号',
  `member_id`        BIGINT UNSIGNED NOT NULL COMMENT '会员ID',
  `total_amount`     DECIMAL(10,2) NOT NULL COMMENT '商品总额',
  `coupon_amount`    DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '优惠券抵扣金额',
  `freight_amount`   DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '运费',
  `pay_amount`       DECIMAL(10,2) NOT NULL COMMENT '应付金额 = 商品总额 - 优惠 + 运费',
  `coupon_id`        BIGINT UNSIGNED DEFAULT NULL COMMENT '使用的优惠券ID',
  `status`           TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0待付款 1待发货 2已发货 3已完成 4已关闭',
  `receiver_name`    VARCHAR(64)  NOT NULL COMMENT '收货人姓名',
  `receiver_phone`   VARCHAR(20)  NOT NULL COMMENT '收货人电话',
  `receiver_address` VARCHAR(255) NOT NULL COMMENT '收货地址',
  `note`             VARCHAR(500) DEFAULT NULL COMMENT '买家留言',
  `logistics_company` VARCHAR(64) DEFAULT NULL COMMENT '物流公司',
  `logistics_sn`     VARCHAR(64)  DEFAULT NULL COMMENT '物流单号',
  `pay_time`         DATETIME     DEFAULT NULL COMMENT '支付时间',
  `delivery_time`    DATETIME     DEFAULT NULL COMMENT '发货时间',
  `receive_time`     DATETIME     DEFAULT NULL COMMENT '确认收货时间',
  `close_time`       DATETIME     DEFAULT NULL COMMENT '关闭时间',
  `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
  `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_sn` (`order_sn`),
  KEY `idx_member` (`member_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- ---------------------------------------------------------------------
-- 12. oms_order_item 订单明细表（商品信息快照）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `oms_order_item`;
CREATE TABLE `oms_order_item` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_id`     BIGINT UNSIGNED NOT NULL COMMENT '订单ID',
  `product_id`   BIGINT UNSIGNED NOT NULL COMMENT '商品ID',
  `product_name` VARCHAR(200) NOT NULL COMMENT '商品名称（快照）',
  `product_pic`  VARCHAR(500) DEFAULT NULL COMMENT '商品主图（快照）',
  `sku_id`       BIGINT UNSIGNED NOT NULL COMMENT 'SKU ID',
  `sku_specs`    VARCHAR(255) DEFAULT NULL COMMENT 'SKU规格（快照）',
  `price`        DECIMAL(10,2) NOT NULL COMMENT '商品单价（快照）',
  `quantity`     INT          NOT NULL COMMENT '购买数量',
  `item_amount`  DECIMAL(10,2) NOT NULL COMMENT '明细小计 = 单价 × 数量',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';

-- ---------------------------------------------------------------------
-- 13. oms_order_return 退款申请表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `oms_order_return`;
CREATE TABLE `oms_order_return` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `order_id`    BIGINT UNSIGNED NOT NULL COMMENT '订单ID',
  `order_sn`    VARCHAR(32)  NOT NULL COMMENT '订单编号',
  `member_id`   BIGINT UNSIGNED NOT NULL COMMENT '会员ID',
  `reason`      VARCHAR(255) NOT NULL COMMENT '退款原因',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '问题描述',
  `amount`      DECIMAL(10,2) NOT NULL COMMENT '退款金额',
  `status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '审核状态：0待审核 1已通过 2已拒绝',
  `handle_note` VARCHAR(255) DEFAULT NULL COMMENT '审核备注',
  `handle_time` DATETIME     DEFAULT NULL COMMENT '审核时间',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款申请表';

-- ---------------------------------------------------------------------
-- 14. sms_coupon 优惠券表
--     type：1满减券（amount为减免金额） 2折扣券（amount为折扣，如9.5表示95折）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `sms_coupon`;
CREATE TABLE `sms_coupon` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name`          VARCHAR(100) NOT NULL COMMENT '优惠券名称',
  `type`          TINYINT      NOT NULL DEFAULT 1 COMMENT '类型：1满减券 2折扣券',
  `amount`        DECIMAL(10,2) NOT NULL COMMENT '满减金额 / 折扣（9.5=95折）',
  `min_amount`    DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '使用门槛（订单满X元可用）',
  `total_count`   INT          NOT NULL DEFAULT 0 COMMENT '发行总量',
  `receive_count` INT          NOT NULL DEFAULT 0 COMMENT '已领取数量',
  `used_count`    INT          NOT NULL DEFAULT 0 COMMENT '已使用数量',
  `per_limit`     INT          NOT NULL DEFAULT 1 COMMENT '每人限领数量',
  `start_time`    DATETIME     NOT NULL COMMENT '有效期开始',
  `end_time`      DATETIME     NOT NULL COMMENT '有效期结束',
  `status`        TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：1进行中 0已停止',
  `points_price`  INT          NOT NULL DEFAULT 0 COMMENT '积分兑换价（0=不参与积分兑换；100积分=1元面额）',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券表';

-- ---------------------------------------------------------------------
-- 15. sms_coupon_member 会员优惠券表（领取记录）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `sms_coupon_member`;
CREATE TABLE `sms_coupon_member` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `coupon_id`    BIGINT UNSIGNED NOT NULL COMMENT '优惠券ID',
  `member_id`    BIGINT UNSIGNED NOT NULL COMMENT '会员ID',
  `status`       TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0未使用 1已使用 2已过期',
  `order_id`     BIGINT UNSIGNED DEFAULT NULL COMMENT '使用订单ID',
  `receive_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',
  `use_time`     DATETIME     DEFAULT NULL COMMENT '使用时间',
  PRIMARY KEY (`id`),
  KEY `idx_member` (`member_id`),
  KEY `idx_coupon` (`coupon_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员优惠券表';

-- ---------------------------------------------------------------------
-- 16. ums_chat_message 客服聊天消息表
--     from_type：1会员 2管理员；读取历史时按接收方标记已读
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `ums_chat_message`;
CREATE TABLE `ums_chat_message` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `session_id`  BIGINT UNSIGNED DEFAULT NULL COMMENT '会话ID',
  `member_id`   BIGINT UNSIGNED NOT NULL COMMENT '会员ID（会话归属）',
  `admin_id`    BIGINT UNSIGNED DEFAULT NULL COMMENT '接待管理员ID（会员消息为空）',
  `from_type`   TINYINT      NOT NULL COMMENT '发送方：1会员 2管理员',
  `content`     VARCHAR(1000) NOT NULL COMMENT '消息内容（纯文本）',
  `is_read`     TINYINT      NOT NULL DEFAULT 0 COMMENT '已读：0否 1是',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  PRIMARY KEY (`id`),
  KEY `idx_session` (`session_id`),
  KEY `idx_member` (`member_id`),
  KEY `idx_member_read` (`member_id`, `from_type`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客服聊天消息表';

-- ---------------------------------------------------------------------
-- 16.1 ums_chat_session 客服会话表（会话制 + 一对一分派）
--     每个会员同一时间最多一个进行中会话；agent_id 为空=待接入
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `ums_chat_session`;
CREATE TABLE `ums_chat_session` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `member_id`   BIGINT UNSIGNED NOT NULL COMMENT '会员ID',
  `agent_id`    BIGINT UNSIGNED DEFAULT NULL COMMENT '接待客服管理员ID（NULL=待接入）',
  `status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0进行中 1已结束',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '会话创建时间',
  `assign_time` DATETIME     DEFAULT NULL COMMENT '接入时间',
  `end_time`    DATETIME     DEFAULT NULL COMMENT '结束时间',
  PRIMARY KEY (`id`),
  KEY `idx_member_status` (`member_id`, `status`),
  KEY `idx_agent_status` (`agent_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客服会话表';

-- ---------------------------------------------------------------------
-- 17. pms_comment 商品评价表
--     唯一约束 (order_id, product_id)：同一订单同一商品仅可评价一次
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `pms_comment`;
CREATE TABLE `pms_comment` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `product_id`      BIGINT UNSIGNED NOT NULL COMMENT '商品ID',
  `order_id`        BIGINT UNSIGNED NOT NULL COMMENT '订单ID（评价资格来源）',
  `member_id`       BIGINT UNSIGNED NOT NULL COMMENT '会员ID',
  `member_nickname` VARCHAR(64) DEFAULT NULL COMMENT '会员昵称（快照）',
  `member_level`    TINYINT NOT NULL DEFAULT 1 COMMENT '会员等级（快照）',
  `rating`          TINYINT NOT NULL COMMENT '评分 1-5',
  `content`         VARCHAR(500) NOT NULL COMMENT '评价内容（纯文本）',
  `create_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评价时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_product` (`order_id`, `product_id`),
  KEY `idx_product` (`product_id`),
  KEY `idx_member` (`member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品评价表';

-- ---------------------------------------------------------------------
-- 18. cms_banner 首页轮播图（后台可配）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `cms_banner`;
CREATE TABLE `cms_banner` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tag`         VARCHAR(32)  NOT NULL DEFAULT '' COMMENT '角标文字',
  `title`       VARCHAR(100) NOT NULL COMMENT '主标题',
  `subtitle`    VARCHAR(255) DEFAULT NULL COMMENT '副标题',
  `cta_text`    VARCHAR(32)  DEFAULT '立即查看' COMMENT '按钮文字',
  `link_url`    VARCHAR(255) NOT NULL DEFAULT '/shop' COMMENT '跳转地址',
  `bg_from`     VARCHAR(16)  NOT NULL DEFAULT '#0b3d33' COMMENT '渐变起始色',
  `bg_to`       VARCHAR(16)  NOT NULL DEFAULT '#0d9488' COMMENT '渐变结束色',
  `image1`      VARCHAR(500) DEFAULT NULL COMMENT '拼贴图1',
  `image2`      VARCHAR(500) DEFAULT NULL COMMENT '拼贴图2',
  `sort`        INT          NOT NULL DEFAULT 0 COMMENT '排序',
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1上架 0下架',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='首页轮播图';

-- ---------------------------------------------------------------------
-- 19. cms_notice 商城公告
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `cms_notice`;
CREATE TABLE `cms_notice` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `content`     VARCHAR(255) NOT NULL COMMENT '公告内容',
  `sort`        INT          NOT NULL DEFAULT 0 COMMENT '排序',
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1显示 0隐藏',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商城公告';

-- ---------------------------------------------------------------------
-- 20. ums_points_log 积分批次账本（12 个月滚动过期，消费按批先进先出）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `ums_points_log`;
CREATE TABLE `ums_points_log` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `member_id`   BIGINT UNSIGNED NOT NULL COMMENT '会员ID',
  `amount`      INT          NOT NULL COMMENT '批次剩余积分（获取为正；流水为变动量）',
  `type`        TINYINT      NOT NULL DEFAULT 1 COMMENT '类型：1获取批次 2消费流水 3过期流水',
  `remark`      VARCHAR(255) DEFAULT NULL COMMENT '备注（订单号/兑换说明）',
  `expire_time` DATETIME     DEFAULT NULL COMMENT '批次过期时间（获取批次专属）',
  `status`      TINYINT      NOT NULL DEFAULT 0 COMMENT '批次状态：0有效 1已过期/已核销',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_member` (`member_id`, `type`, `status`),
  KEY `idx_expire` (`status`, `expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分批次账本';

-- ---------------------------------------------------------------------
-- 21. sms_flash_sale 限时秒杀（SKU 级秒杀价，下单时服务端强制生效）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `sms_flash_sale`;
CREATE TABLE `sms_flash_sale` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name`        VARCHAR(100) NOT NULL COMMENT '活动名称',
  `sku_id`      BIGINT UNSIGNED NOT NULL COMMENT '参与秒杀的SKU（唯一）',
  `flash_price` DECIMAL(10,2) NOT NULL COMMENT '秒杀价',
  `per_user_limit` INT        NOT NULL DEFAULT 1 COMMENT '每人限购数量',
  `start_time`  DATETIME     NOT NULL COMMENT '开始时间',
  `end_time`    DATETIME     NOT NULL COMMENT '结束时间',
  `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sku` (`sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='限时秒杀';

-- ---------------------------------------------------------------------
-- 22. sys_admin_log 后台操作审计日志（AOP 自动记录写操作）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_admin_log`;
CREATE TABLE `sys_admin_log` (
  `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `admin_id`       BIGINT UNSIGNED DEFAULT NULL COMMENT '管理员ID',
  `admin_username` VARCHAR(64)  DEFAULT NULL COMMENT '管理员账号',
  `module`         VARCHAR(64)  DEFAULT NULL COMMENT '模块（控制器）',
  `action`         VARCHAR(64)  DEFAULT NULL COMMENT '动作（方法）',
  `method`         VARCHAR(10)  DEFAULT NULL COMMENT 'HTTP方法',
  `uri`            VARCHAR(255) DEFAULT NULL COMMENT '请求路径',
  `params`         VARCHAR(1000) DEFAULT NULL COMMENT '请求参数（敏感字段已脱敏）',
  `ip`             VARCHAR(64)  DEFAULT NULL COMMENT '来源IP',
  `status`         TINYINT      NOT NULL DEFAULT 1 COMMENT '结果：1成功 0失败',
  `error_msg`      VARCHAR(500) DEFAULT NULL COMMENT '失败原因',
  `cost_ms`        INT          DEFAULT NULL COMMENT '耗时毫秒',
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_admin` (`admin_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='后台操作审计日志';

-- ---------------------------------------------------------------------
-- 23. sys_config 系统参数配置（后台可视化维护，如支付宝支付参数；DB 优先于环境变量）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `cfg_key`     VARCHAR(100) NOT NULL COMMENT '配置键',
  `cfg_value`   VARCHAR(2000) DEFAULT NULL COMMENT '配置值',
  `remark`      VARCHAR(255) DEFAULT NULL COMMENT '说明',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_key` (`cfg_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统参数配置（后台可视化维护）';

-- =====================================================================
-- 基础数据
-- =====================================================================

-- 管理员（密码占位符 INIT，由后端启动时 BCrypt 加密为 admin123）
INSERT INTO `ums_admin` (`id`,`username`,`password`,`nickname`,`email`,`phone`,`status`) VALUES
(1,'admin','INIT','超级管理员','admin@limemall.com','13800000001',1),
(2,'yunying','INIT','运营小云','yunying@limemall.com','13800000002',1),
(3,'kefu','INIT','客服小柠','kefu@limemall.com','13800000003',1),
(4,'kefu2','INIT','客服小柚','kefu2@limemall.com','13800000004',1);

-- 角色
INSERT INTO `ums_role` (`id`,`name`,`description`,`sort`,`status`) VALUES
(1,'超级管理员','拥有系统全部权限',100,1),
(2,'运营专员','负责商品、订单、会员与营销管理',80,1),
(3,'客服专员','负责订单处理与退款审核',60,1);

-- 管理员-角色关联
INSERT INTO `ums_admin_role` (`admin_id`,`role_id`) VALUES (1,1),(2,2),(3,3),(4,3);

-- 菜单权限
INSERT INTO `ums_menu` (`id`,`parent_id`,`name`,`path`,`icon`,`perm`,`type`,`sort`,`status`) VALUES
(1,0,'数据看板','/dashboard','Odometer','dashboard:view',1,100,1),
(10,0,'商品管理','/product','Goods','',0,90,1),
(11,10,'商品列表','/product/list','','pms:product:list',1,99,1),
(111,11,'新增商品','','','pms:product:create',2,99,1),
(112,11,'编辑商品','','','pms:product:update',2,98,1),
(113,11,'上下架','','','pms:product:publish',2,97,1),
(114,11,'删除商品','','','pms:product:delete',2,96,1),
(12,10,'品牌管理','/product/brand','','pms:brand:list',1,98,1),
(121,12,'新增品牌','','','pms:brand:create',2,99,1),
(122,12,'编辑品牌','','','pms:brand:update',2,98,1),
(123,12,'删除品牌','','','pms:brand:delete',2,97,1),
(13,10,'分类管理','/product/category','','pms:category:list',1,97,1),
(131,13,'新增分类','','','pms:category:create',2,99,1),
(132,13,'编辑分类','','','pms:category:update',2,98,1),
(133,13,'删除分类','','','pms:category:delete',2,97,1),
(61,10,'评价管理','/product/comment','ChatLineSquare','pms:comment:list',1,96,1),
(611,61,'删除评价','','','pms:comment:delete',2,99,1),
(20,0,'订单管理','/order','List','',0,80,1),
(21,20,'订单列表','/order/list','','oms:order:list',1,99,1),
(211,21,'订单发货','','','oms:order:deliver',2,99,1),
(212,21,'关闭订单','','','oms:order:close',2,98,1),
(22,20,'退款审核','/order/return','','oms:return:list',1,98,1),
(221,22,'审核退款','','','oms:return:handle',2,99,1),
(30,0,'会员管理','/member/list','User','ums:member:list',1,70,1),
(301,30,'启用/禁用会员','','','ums:member:update',2,99,1),
(40,0,'营销管理','/marketing','Present','',0,60,1),
(41,40,'优惠券管理','/marketing/coupon','','sms:coupon:list',1,99,1),
(411,41,'新建优惠券','','','sms:coupon:create',2,99,1),
(412,41,'编辑优惠券','','','sms:coupon:update',2,98,1),
(413,41,'上线下线','','','sms:coupon:publish',2,97,1),
(50,0,'系统管理','/system','Setting','',0,50,1),
(51,50,'管理员管理','/system/admin','','ums:admin:list',1,99,1),
(511,51,'新增管理员','','','ums:admin:create',2,99,1),
(512,51,'编辑管理员','','','ums:admin:update',2,98,1),
(513,51,'重置密码','','','ums:admin:reset',2,97,1),
(514,51,'删除管理员','','','ums:admin:delete',2,96,1),
(52,50,'角色管理','/system/role','','ums:role:list',1,98,1),
(521,52,'新增角色','','','ums:role:create',2,99,1),
(522,52,'编辑角色','','','ums:role:update',2,98,1),
(523,52,'分配权限','','','ums:role:assign',2,97,1),
(524,52,'删除角色','','','ums:role:delete',2,96,1),
(53,50,'个人中心','/system/profile','','',1,0,1),
(55,50,'支付配置','/system/pay-config','Wallet','sys:config:list',1,97,1),
(551,55,'保存配置','','','sys:config:edit',2,99,1),
(60,0,'客服聊天','/admin/chat','ChatDotRound','ums:chat:list',1,65,1),
(601,60,'回复消息','','','ums:chat:reply',2,99,1),
(42,40,'内容管理','/marketing/content','Picture','cms:content:list',1,98,1),
(421,42,'编辑内容','','','cms:content:edit',2,99,1),
(43,40,'秒杀管理','/marketing/flash','Timer','sms:flash:list',1,97,1),
(431,43,'编辑秒杀','','','sms:flash:edit',2,99,1),
(432,43,'删除秒杀','','','sms:flash:delete',2,98,1),
(54,50,'操作日志','/system/log','Tickets','sys:log:list',1,96,1);

-- 管理端统一路由前缀：前台主站为 /shop，管理端为 /admin/**
UPDATE `ums_menu` SET `path` = CONCAT('/admin', `path`) WHERE `path` LIKE '/%' AND `path` NOT LIKE '/admin/%';

-- 角色-菜单关联：超级管理员拥有全部权限（聊天回复除外——监管视角只读，职责分离）
INSERT INTO `ums_role_menu` (`role_id`,`menu_id`) SELECT 1, id FROM `ums_menu` WHERE `perm` <> 'ums:chat:reply';
-- 运营专员
INSERT INTO `ums_role_menu` (`role_id`,`menu_id`) VALUES
(2,1),(2,10),(2,11),(2,111),(2,112),(2,113),(2,114),(2,12),(2,121),(2,122),(2,123),(2,13),(2,131),(2,132),(2,133),(2,61),(2,611),
(2,20),(2,21),(2,211),(2,212),(2,22),(2,221),(2,30),(2,301),(2,40),(2,41),(2,411),(2,412),(2,413),(2,60),(2,53),
(2,42),(2,421),(2,43),(2,431),(2,432);
-- 客服专员
INSERT INTO `ums_role_menu` (`role_id`,`menu_id`) VALUES
(3,1),(3,20),(3,21),(3,22),(3,221),(3,30),(3,53),(3,60),(3,601);

-- 品牌
INSERT INTO `pms_brand` (`id`,`name`,`first_letter`,`description`,`sort`,`status`) VALUES
(1,'青柠自营','Q','平台自营品牌，品质与售后双重保障',100,1),
(2,'云岚数码','Y','专注智能数码与周边配件',90,1),
(3,'拾光电纸','S','电子阅读与便携办公设备',80,1),
(4,'北辰家居','B','北欧风家居生活电器',70,1),
(5,'素见美妆','S','温和植萃护肤品牌',60,1),
(6,'简茶饮品','J','新式茶饮与咖啡冲泡',50,1),
(7,'星野运动','X','轻户外运动装备',40,1),
(8,'沐野食品','M','休闲零食与健康食集',30,1),
(9,'沐光服饰','M','基础款通勤服饰',20,1),
(10,'山临生活','S','厨房与日用器物',10,1);

-- 商品分类（两级）
INSERT INTO `pms_category` (`id`,`parent_id`,`name`,`level`,`sort`,`status`) VALUES
(1,0,'数码家电',1,100,1),
(11,1,'手机数码',2,99,1),
(12,1,'智能穿戴',2,98,1),
(13,1,'家用电器',2,97,1),
(2,0,'服饰鞋包',1,90,1),
(21,2,'男装',2,99,1),
(22,2,'女装',2,98,1),
(23,2,'箱包',2,97,1),
(3,0,'食品生鲜',1,80,1),
(31,3,'休闲零食',2,99,1),
(32,3,'饮料冲调',2,98,1),
(4,0,'美妆个护',1,70,1),
(41,4,'面部护肤',2,99,1),
(42,4,'香氛洗护',2,98,1),
(5,0,'家居生活',1,60,1),
(51,5,'家纺床品',2,99,1),
(52,5,'厨房用品',2,98,1);

-- 商品 SPU
INSERT INTO `pms_product` (`id`,`name`,`sub_title`,`category_id`,`brand_id`,`main_pic`,`album`,`price`,`original_price`,`stock`,`sales`,`unit`,`detail_html`,`sort`,`publish_status`) VALUES
(1,'云岚 无线降噪耳机 Pro','40dB 深度降噪 · 40小时续航 · LDAC 高清音频',11,2,'/img/products/p01.svg','/img/products/p01.svg',399.00,499.00,0,0,'副','<p>【青柠优选】云岚无线降噪耳机 Pro，40dB 深度主动降噪，40 小时综合续航，支持 LDAC 高清音频传输。</p><p>包装清单：耳机 ×1、充电线 ×1、说明书 ×1。</p>',100,1),
(2,'云岚 65W 氮化镓充电器','小体积大功率 · 双口盲插 · 智能温控',11,2,'/img/products/p02.svg','/img/products/p02.svg',129.00,159.00,0,0,'个','<p>65W 大功率氮化镓快充，体积比传统充电器小 35%，支持 PD/QC 多协议，出差旅行一充多用。</p>',99,1),
(3,'拾光 墨水屏阅读器 6.8英寸','300PPI 墨水屏 · 冷暖双色温 · 32GB 大存储',11,3,'/img/products/p03.svg','/img/products/p03.svg',1099.00,1299.00,0,0,'台','<p>6.8 英寸 300PPI 高清墨水屏，类纸阅读体验；冷暖双色温前光，白天黑夜皆宜。</p>',98,1),
(4,'云岚 智能手环 5 代','血氧心率监测 · 14 天续航 · 50 米防水',12,2,'/img/products/p04.svg','/img/products/p04.svg',199.00,249.00,0,0,'只','<p>全天候心率与血氧监测，支持 100+ 运动模式，14 天超长续航，5ATM 游泳级防水。</p>',97,1),
(5,'云岚 机械键盘 87 键','三模连接 · 热插拔轴体 · PBT 键帽',11,2,'/img/products/p05.svg','/img/products/p05.svg',299.00,369.00,0,0,'把','<p>87 键紧凑布局，支持蓝牙/2.4G/有线三模连接，全键热插拔，PBT 双色键帽耐磨不打油。</p>',96,1),
(6,'拾光 便携显示器 15.6 英寸','1080P IPS · 内置电池 · 一线连',11,3,'/img/products/p06.svg','/img/products/p06.svg',899.00,1099.00,0,0,'台','<p>15.6 英寸 1080P IPS 便携屏，内置 8000mAh 电池，Type-C 一线直连，出差副屏首选。</p>',95,1),
(7,'山临 电热水壶 1.7L','304 不锈钢 · 双层防烫 · 快速沸腾',13,10,'/img/products/p07.svg','/img/products/p07.svg',89.00,129.00,0,0,'只','<p>1.7L 大容量，304 食品级不锈钢内胆，双层防烫壶身，1800W 快速沸腾，自动断电保护。</p>',94,1),
(8,'北辰 空气炸锅 4.5L','无油低脂 · 可视窗口 · 智能菜单',13,4,'/img/products/p08.svg','/img/products/p08.svg',299.00,399.00,0,0,'台','<p>4.5L 大容量空气炸锅，360° 热风循环，无油也可酥脆；可视窗口与智能菜单小白友好。</p>',93,1),
(9,'北辰 手持挂烫机','20 秒速热 · 蒸汽大熨烫 · 差旅便携',13,4,'/img/products/p09.svg','/img/products/p09.svg',159.00,219.00,0,0,'台','<p>20 秒快速出蒸汽，平烫挂烫二合一，折叠收纳出差便携，除皱除螨一举两得。</p>',92,1),
(10,'云岚 桌面循环小风扇','静音循环 · 三档风力 · 可拆洗扇叶',13,2,'/img/products/p10.svg','/img/products/p10.svg',79.00,99.00,0,0,'台','<p>涡轮循环送风，三档静音风力，扇叶可拆卸水洗，办公室桌面清凉伴侣。</p>',91,1),
(11,'沐光 纯棉基础 T 恤','260g 重磅纯棉 · 舒适透气 · 男女同款',21,9,'/img/products/p11.svg','/img/products/p11.svg',59.00,79.00,0,0,'件','<p>260g 重磅精梳棉，亲肤透气不易变形，基础百搭款，男女同款。</p>',90,1),
(12,'沐光 休闲直筒牛仔裤','弹力舒适 · 直筒修身 · 四季百搭',21,9,'/img/products/p12.svg','/img/products/p12.svg',159.00,199.00,0,0,'条','<p>微弹舒适面料，直筒修身剪裁，经典五袋设计，四季通勤百搭。</p>',89,1),
(13,'沐光 针织开衫','柔软亲肤 · 简约通勤 · 三季可穿',22,9,'/img/products/p13.svg','/img/products/p13.svg',129.00,169.00,0,0,'件','<p>柔糯针织面料，轻盈亲肤，简约版型通勤休闲皆宜，春秋外搭首选。</p>',88,1),
(14,'星野 速干运动 T 恤','吸湿排汗 · 轻量透气 · 运动版型',21,7,'/img/products/p14.svg','/img/products/p14.svg',89.00,119.00,0,0,'件','<p>专业速干面料，吸湿排汗快干，运动版型不束缚，跑步健身皆宜。</p>',87,1),
(15,'星野 防风冲锋衣','防泼水 · 三合一可拆 · 户外通勤两用',21,7,'/img/products/p15.svg','/img/products/p15.svg',399.00,499.00,0,0,'件','<p>防泼水面料，抓绒内胆可拆卸三合一设计，登山露营与城市通勤两相宜。</p>',86,1),
(16,'沐光 帆布托特包','大容量 · 加厚帆布 · 通勤购物两用',23,9,'/img/products/p16.svg','/img/products/p16.svg',69.00,89.00,0,0,'个','<p>加厚帆布大容量托特包，可装 14 寸笔记本，通勤购物两相宜。</p>',85,1),
(17,'星野 轻量跑鞋','单只 210g · 回弹中底 · 透气网面',22,7,'/img/products/p17.svg','/img/products/p17.svg',259.00,329.00,0,0,'双','<p>单只仅 210g 轻量设计，高回弹中底缓震，一体织透气网面，日常跑步训练首选。</p>',84,1),
(18,'沐野 每日坚果礼盒 30 包','六种坚果 · 科学配比 · 独立小包装',31,8,'/img/products/p18.svg','/img/products/p18.svg',89.00,119.00,0,0,'盒','<p>巴旦木、腰果、核桃仁等六种坚果科学配比，每日一包，独立包装锁鲜。</p>',83,1),
(19,'沐野 手撕面包 1kg','柔软拉丝 · 早代餐 · 整箱囤货',31,8,'/img/products/p19.svg','/img/products/p19.svg',29.90,39.90,0,0,'箱','<p>柔软拉丝口感，奶香浓郁，早餐代餐下午茶皆宜，整箱实惠装。</p>',82,1),
(20,'沐野 香辣小鱼干 200g','香辣过瘾 · 独立包装 · 追剧零食',31,8,'/img/products/p20.svg','/img/products/p20.svg',19.90,25.90,0,0,'袋','<p>精选小银鱼，香辣入味，独立小包装，追剧下酒小零食。</p>',81,1),
(21,'简茶 冷萃乌龙茶包','0 糖 0 卡 · 冷泡热泡皆宜 · 20 包装',32,6,'/img/products/p21.svg','/img/products/p21.svg',39.90,49.90,0,0,'盒','<p>原叶乌龙茶包，冷水浸泡 4 小时即可享用，0 糖 0 卡，清爽解腻。</p>',80,1),
(22,'简茶 桂花乌龙 500ml×6','桂花香气 · 低糖配方 · 6 瓶整箱',32,6,'/img/products/p22.svg','/img/products/p22.svg',29.90,35.90,0,0,'箱','<p>桂花与乌龙茶的经典搭配，淡雅香气低糖不腻，6 瓶整箱装。</p>',79,1),
(23,'简茶 挂耳咖啡 10 包','中深烘焙 · 现磨锁鲜 · 手冲体验',32,6,'/img/products/p23.svg','/img/products/p23.svg',45.00,59.00,0,0,'盒','<p>中深烘焙阿拉比卡豆，氮气锁鲜独立包装，三分钟享受现磨手冲体验。</p>',78,1),
(24,'素见 氨基酸洁面乳 120g','温和不刺激 · 绵密泡沫 · 敏肌可用',41,5,'/img/products/p24.svg','/img/products/p24.svg',69.00,89.00,0,0,'支','<p>氨基酸表活温和清洁，绵密泡沫深层洁净不紧绷，敏感肌可用。</p>',77,1),
(25,'素见 烟酰胺精华 30ml','提亮肤色 · 淡化痘印 · 清爽不粘腻',41,5,'/img/products/p25.svg','/img/products/p25.svg',129.00,169.00,0,0,'瓶','<p>5% 烟酰胺复配传明酸，提亮肤色淡化痘印，清爽质地吸收快。</p>',76,1),
(26,'素见 香氛护手霜 50ml','滋润不黏腻 · 便携装 · 三款香型',42,5,'/img/products/p26.svg','/img/products/p26.svg',35.00,45.00,0,0,'支','<p>乳木果油滋润配方，清爽不黏腻，白茶/青柠/鼠尾草三款香型可选。</p>',75,1),
(27,'山临 陶瓷马克杯 350ml','哑光釉面 · 简约器型 · 微波炉可用',52,10,'/img/products/p27.svg','/img/products/p27.svg',39.00,49.00,0,0,'只','<p>高温烧制哑光釉面，350ml 日常容量，微波炉/洗碗机可用。</p>',74,1),
(28,'山临 不粘平底锅 26cm','麦饭石涂层 · 少油不粘 · 燃气电磁通用',52,10,'/img/products/p28.svg','/img/products/p28.svg',119.00,159.00,0,0,'口','<p>26cm 麦饭石不粘涂层，少油烹饪更健康，燃气灶电磁炉通用。</p>',73,1),
(29,'北辰 全棉四件套 1.8m','60 支长绒棉 · 亲肤柔软 · 简约色系',51,4,'/img/products/p29.svg','/img/products/p29.svg',299.00,399.00,0,0,'套','<p>60 支长绒棉面料，亲肤柔软透气，被套 200×230cm 适配 1.8m 床。</p>',72,1);

-- 商品 SKU
INSERT INTO `pms_sku` (`product_id`,`sku_code`,`specs`,`price`,`stock`) VALUES
(1,'SKU-0001-01','[{"key":"颜色","value":"云雾白"}]',399.00,220),
(1,'SKU-0001-02','[{"key":"颜色","value":"曜石黑"}]',399.00,180),
(2,'SKU-0002-01','[{"key":"接口","value":"单C口"}]',129.00,400),
(2,'SKU-0002-02','[{"key":"接口","value":"双C口"}]',149.00,350),
(3,'SKU-0003-01','[{"key":"存储","value":"16GB"}]',1099.00,120),
(3,'SKU-0003-02','[{"key":"存储","value":"32GB"}]',1199.00,90),
(4,'SKU-0004-01','[{"key":"颜色","value":"曜石黑"}]',199.00,300),
(4,'SKU-0004-02','[{"key":"颜色","value":"青柠绿"}]',199.00,280),
(4,'SKU-0004-03','[{"key":"颜色","value":"蜜桃橙"}]',209.00,150),
(5,'SKU-0005-01','[{"key":"轴体","value":"青轴"}]',299.00,160),
(5,'SKU-0005-02','[{"key":"轴体","value":"茶轴"}]',319.00,140),
(5,'SKU-0005-03','[{"key":"轴体","value":"红轴"}]',319.00,130),
(6,'SKU-0006-01','[{"key":"版本","value":"标准版"}]',899.00,100),
(6,'SKU-0006-02','[{"key":"版本","value":"触控版"}]',1099.00,80),
(7,'SKU-0007-01','[{"key":"颜色","value":"奶白"}]',89.00,500),
(7,'SKU-0007-02','[{"key":"颜色","value":"薄荷绿"}]',89.00,420),
(8,'SKU-0008-01','[{"key":"版本","value":"机械款"}]',299.00,200),
(8,'SKU-0008-02','[{"key":"版本","value":"智能款"}]',359.00,150),
(9,'SKU-0009-01','[{"key":"颜色","value":"象牙白"}]',159.00,260),
(10,'SKU-0010-01','[{"key":"颜色","value":"雪松绿"}]',79.00,380),
(10,'SKU-0010-02','[{"key":"颜色","value":"奶白"}]',79.00,350),
(11,'SKU-0011-01','[{"key":"颜色","value":"白色"},{"key":"尺码","value":"M"}]',59.00,300),
(11,'SKU-0011-02','[{"key":"颜色","value":"白色"},{"key":"尺码","value":"L"}]',59.00,280),
(11,'SKU-0011-03','[{"key":"颜色","value":"黑色"},{"key":"尺码","value":"M"}]',59.00,260),
(11,'SKU-0011-04','[{"key":"颜色","value":"黑色"},{"key":"尺码","value":"L"}]',59.00,240),
(12,'SKU-0012-01','[{"key":"尺码","value":"30"}]',159.00,120),
(12,'SKU-0012-02','[{"key":"尺码","value":"31"}]',159.00,140),
(12,'SKU-0012-03','[{"key":"尺码","value":"32"}]',159.00,130),
(12,'SKU-0012-04','[{"key":"尺码","value":"33"}]',159.00,90),
(13,'SKU-0013-01','[{"key":"颜色","value":"燕麦色"},{"key":"尺码","value":"M"}]',129.00,150),
(13,'SKU-0013-02','[{"key":"颜色","value":"燕麦色"},{"key":"尺码","value":"L"}]',129.00,130),
(13,'SKU-0013-03','[{"key":"颜色","value":"雾霾蓝"},{"key":"尺码","value":"M"}]',139.00,110),
(14,'SKU-0014-01','[{"key":"颜色","value":"黑色"},{"key":"尺码","value":"M"}]',89.00,220),
(14,'SKU-0014-02','[{"key":"颜色","value":"黑色"},{"key":"尺码","value":"L"}]',89.00,200),
(14,'SKU-0014-03','[{"key":"颜色","value":"灰色"},{"key":"尺码","value":"M"}]',89.00,180),
(15,'SKU-0015-01','[{"key":"颜色","value":"军绿"},{"key":"尺码","value":"M"}]',399.00,100),
(15,'SKU-0015-02','[{"key":"颜色","value":"军绿"},{"key":"尺码","value":"L"}]',399.00,90),
(15,'SKU-0015-03','[{"key":"颜色","value":"深蓝"},{"key":"尺码","value":"L"}]',419.00,80),
(16,'SKU-0016-01','[{"key":"颜色","value":"米白"}]',69.00,260),
(16,'SKU-0016-02','[{"key":"颜色","value":"墨绿"}]',69.00,220),
(17,'SKU-0017-01','[{"key":"尺码","value":"41"}]',259.00,110),
(17,'SKU-0017-02','[{"key":"尺码","value":"42"}]',259.00,120),
(17,'SKU-0017-03','[{"key":"尺码","value":"43"}]',259.00,100),
(18,'SKU-0018-01','[{"key":"规格","value":"30包"}]',89.00,300),
(19,'SKU-0019-01','[{"key":"口味","value":"原味"}]',29.90,500),
(19,'SKU-0019-02','[{"key":"口味","value":"奶香"}]',29.90,460),
(20,'SKU-0020-01','[{"key":"口味","value":"微辣"}]',19.90,600),
(20,'SKU-0020-02','[{"key":"口味","value":"中辣"}]',19.90,580),
(21,'SKU-0021-01','[{"key":"规格","value":"20包"}]',39.90,350),
(21,'SKU-0021-02','[{"key":"规格","value":"40包"}]',69.90,250),
(22,'SKU-0022-01','[{"key":"规格","value":"6瓶整箱"}]',29.90,400),
(23,'SKU-0023-01','[{"key":"风味","value":"经典拼配"}]',45.00,320),
(23,'SKU-0023-02','[{"key":"风味","value":"果香拼配"}]',49.00,280),
(24,'SKU-0024-01','[{"key":"规格","value":"120g"}]',69.00,300),
(25,'SKU-0025-01','[{"key":"规格","value":"30ml"}]',129.00,220),
(26,'SKU-0026-01','[{"key":"香型","value":"白茶"}]',35.00,350),
(26,'SKU-0026-02','[{"key":"香型","value":"青柠"}]',35.00,380),
(26,'SKU-0026-03','[{"key":"香型","value":"鼠尾草"}]',35.00,300),
(27,'SKU-0027-01','[{"key":"颜色","value":"奶白"}]',39.00,400),
(27,'SKU-0027-02','[{"key":"颜色","value":"薄荷绿"}]',39.00,360),
(27,'SKU-0027-03','[{"key":"颜色","value":"鹅黄"}]',39.00,320),
(28,'SKU-0028-01','[{"key":"尺寸","value":"26cm"}]',119.00,180),
(29,'SKU-0029-01','[{"key":"尺寸","value":"1.5m床"}]',269.00,120),
(29,'SKU-0029-02','[{"key":"尺寸","value":"1.8m床"}]',299.00,140);

-- 商品总库存由 SKU 汇总计算（冗余字段维护）
UPDATE `pms_product` p
SET p.stock = (SELECT IFNULL(SUM(s.stock),0) FROM `pms_sku` s WHERE s.product_id = p.id);

-- 限时秒杀种子（按 SKU 编码定位，避免依赖自增ID）
INSERT INTO `sms_flash_sale` (`name`,`sku_id`,`flash_price`,`start_time`,`end_time`,`status`)
SELECT '今日秒杀 · 云岚降噪耳机', id, 299.00, NOW(), DATE_ADD(NOW(), INTERVAL 3 DAY), 1
FROM `pms_sku` WHERE `sku_code` = 'SKU-0001-01' LIMIT 1;
INSERT INTO `sms_flash_sale` (`name`,`sku_id`,`flash_price`,`start_time`,`end_time`,`status`)
SELECT '今日秒杀 · 拾光墨水屏阅读器', id, 899.00, NOW(), DATE_ADD(NOW(), INTERVAL 3 DAY), 1
FROM `pms_sku` WHERE `sku_code` = 'SKU-0003-01' LIMIT 1;

-- 会员（密码占位符 INIT，由后端启动时 BCrypt 加密为 123456）
INSERT INTO `ums_member` (`id`,`username`,`password`,`nickname`,`phone`,`gender`,`birthday`,`level`,`points`,`status`,`register_time`) VALUES
(1,'m001','INIT','林晚','13800010001',2,'1998-03-12',3,1280,1,DATE_SUB(NOW(), INTERVAL 210 DAY)),
(2,'m002','INIT','陈屿','13800010002',1,'1995-07-25',2,860,1,DATE_SUB(NOW(), INTERVAL 186 DAY)),
(3,'m003','INIT','苏念','13800010003',2,'2000-01-08',4,2150,1,DATE_SUB(NOW(), INTERVAL 173 DAY)),
(4,'m004','INIT','周野','13800010004',1,'1997-11-30',1,220,1,DATE_SUB(NOW(), INTERVAL 158 DAY)),
(5,'m005','INIT','沈青','13800010005',2,'1999-05-16',2,640,1,DATE_SUB(NOW(), INTERVAL 143 DAY)),
(6,'m006','INIT','顾北','13800010006',1,'1996-09-02',3,1330,1,DATE_SUB(NOW(), INTERVAL 128 DAY)),
(7,'m007','INIT','许南','13800010007',2,'2001-02-21',1,180,1,DATE_SUB(NOW(), INTERVAL 116 DAY)),
(8,'m008','INIT','江迟','13800010008',1,'1994-12-09',2,910,1,DATE_SUB(NOW(), INTERVAL 104 DAY)),
(9,'m009','INIT','温言','13800010009',2,'1998-08-14',1,350,1,DATE_SUB(NOW(), INTERVAL 96 DAY)),
(10,'m010','INIT','白露','13800010010',2,'2000-10-01',3,1560,1,DATE_SUB(NOW(), INTERVAL 88 DAY)),
(11,'m011','INIT','程时','13800010011',1,'1993-04-18',2,720,0,DATE_SUB(NOW(), INTERVAL 80 DAY)),
(12,'m012','INIT','陆遥','13800010012',1,'1997-06-27',1,260,1,DATE_SUB(NOW(), INTERVAL 71 DAY)),
(13,'m013','INIT','何夕','13800010013',2,'1999-09-05',2,830,1,DATE_SUB(NOW(), INTERVAL 63 DAY)),
(14,'m014','INIT','秦川','13800010014',1,'1995-01-23',1,140,1,DATE_SUB(NOW(), INTERVAL 55 DAY)),
(15,'m015','INIT','夏至','13800010015',2,'2002-06-11',1,90,1,DATE_SUB(NOW(), INTERVAL 47 DAY)),
(16,'m016','INIT','温叙','13800010016',2,'1998-12-30',2,680,1,DATE_SUB(NOW(), INTERVAL 38 DAY)),
(17,'m017','INIT','简宁','13800010017',2,'2001-03-07',1,110,1,DATE_SUB(NOW(), INTERVAL 29 DAY)),
(18,'m018','INIT','宋辞','13800010018',1,'1996-07-19',1,200,1,DATE_SUB(NOW(), INTERVAL 21 DAY)),
(19,'m019','INIT','傅斯','13800010019',1,'1994-10-26',2,540,1,DATE_SUB(NOW(), INTERVAL 13 DAY)),
(20,'m020','INIT','洛栖','13800010020',2,'2000-04-03',1,60,1,DATE_SUB(NOW(), INTERVAL 5 DAY));

-- 成长值按等级基线初始化（成长值只增不减，决定等级）
UPDATE `ums_member` SET `growth` = CASE `level` WHEN 4 THEN 15000 WHEN 3 THEN 5000 WHEN 2 THEN 1000 ELSE 0 END;

-- 生成会员业务编号（对外展示，随机 10 位不可遍历）
UPDATE `ums_member` SET `user_no` = CONCAT('U', UPPER(SUBSTRING(MD5(CONCAT(`id`, RAND(), NOW())), 1, 10)))
WHERE `user_no` IS NULL;

-- 会员积分与等级一致性校准（金卡/钻石会员积分需高于对应门槛）
UPDATE `ums_member` SET `points` = 2600 WHERE `id` = 1;
UPDATE `ums_member` SET `points` = 5200 WHERE `id` = 3;
UPDATE `ums_member` SET `points` = 2350 WHERE `id` = 6;
UPDATE `ums_member` SET `points` = 2420 WHERE `id` = 10;

-- 历史积分迁移为账本批次（12 个月后过期）
INSERT INTO `ums_points_log` (`member_id`, `amount`, `type`, `remark`, `expire_time`)
SELECT `id`, `points`, 1, '历史积分迁移', DATE_ADD(NOW(), INTERVAL 12 MONTH)
FROM `ums_member` WHERE `points` > 0;

-- 优惠券
INSERT INTO `sms_coupon` (`id`,`name`,`type`,`amount`,`min_amount`,`total_count`,`receive_count`,`used_count`,`per_limit`,`start_time`,`end_time`,`status`) VALUES
(1,'新人见面礼 满100减10',1,10.00,100.00,1000,0,0,1,DATE_SUB(NOW(), INTERVAL 30 DAY),DATE_ADD(NOW(), INTERVAL 60 DAY),1),
(2,'会员专享 满300减40',1,40.00,300.00,500,0,0,1,DATE_SUB(NOW(), INTERVAL 15 DAY),DATE_ADD(NOW(), INTERVAL 45 DAY),1),
(3,'全场 95 折券',2,9.50,0.00,300,0,0,1,DATE_SUB(NOW(), INTERVAL 10 DAY),DATE_ADD(NOW(), INTERVAL 30 DAY),1),
(4,'大额满减 满500减80',1,80.00,500.00,200,0,0,2,DATE_SUB(NOW(), INTERVAL 5 DAY),DATE_ADD(NOW(), INTERVAL 25 DAY),1),
(5,'春日限定 满200减30（已结束）',1,30.00,200.00,100,0,0,1,DATE_SUB(NOW(), INTERVAL 60 DAY),DATE_SUB(NOW(), INTERVAL 20 DAY),0),
(6,'积分兑 满50减5',1,5.00,50.00,10000,0,0,5,DATE_SUB(NOW(), INTERVAL 1 DAY),DATE_ADD(NOW(), INTERVAL 180 DAY),1),
(7,'积分兑 无门槛减3',1,3.00,0.00,10000,0,0,10,DATE_SUB(NOW(), INTERVAL 1 DAY),DATE_ADD(NOW(), INTERVAL 180 DAY),1);

-- 积分兑换价（100 积分 = 1 元面额；0 表示不参与积分兑换）
UPDATE `sms_coupon` SET `points_price` = 1000 WHERE `id` = 1;
UPDATE `sms_coupon` SET `points_price` = 4000 WHERE `id` = 2;
UPDATE `sms_coupon` SET `points_price` = 8000 WHERE `id` = 4;
UPDATE `sms_coupon` SET `points_price` = 500  WHERE `id` = 6;
UPDATE `sms_coupon` SET `points_price` = 300  WHERE `id` = 7;

-- 首页轮播图（后台可配置）
INSERT INTO `cms_banner` (`tag`,`title`,`subtitle`,`cta_text`,`link_url`,`bg_from`,`bg_to`,`image1`,`image2`,`sort`,`status`) VALUES
('数码焕新','轻装出行 · 桌面焕新','充电器 / 阅读器 / 键鼠周边，精选好物满 99 包邮','立即选购','/shop/products?categoryId=1','#0b3d33','#0d9488','/img/products/p02.svg','/img/products/p03.svg',100,1),
('积分商城','积分当钱花','100 积分抵 1 元，全场优惠券持续兑换中','去兑换','/shop/points','#7c2d12','#d97706','/img/products/p18.svg','/img/products/p21.svg',90,1),
('会员权益','金卡 / 钻石全场免运费','购物累计成长值，等级自动升级，积分加速获取','查看会员权益','/shop/member','#1e1b4b','#6366f1','/img/products/p17.svg','/img/products/p26.svg',80,1);

-- 商城公告
INSERT INTO `cms_notice` (`content`,`sort`,`status`) VALUES
('全场满 99 元包邮，金卡及以上会员无门槛免运费',100,1),
('支持 7 天无理由退款，退款审核 24 小时内处理',90,1),
('客服在线 9:00 - 21:00，点击右下角悬浮窗咨询',80,1);

SET FOREIGN_KEY_CHECKS = 1;

-- 初始化完成
