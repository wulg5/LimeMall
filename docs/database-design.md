# 青柠商城管理系统（LimeMall）数据库设计说明

> 数据库：`lime_mall`　字符集：`utf8mb4`　排序规则：`utf8mb4_general_ci`　存储引擎：`InnoDB`
> 初始化脚本：`sql/init.sql`（可重复执行）

## 一、设计原则

1. **模块化前缀**：按业务域拆分表前缀——`ums_`（用户与权限）、`pms_`（商品）、`oms_`（订单）、`sms_`（营销），与业界主流电商系统（如 mall）的命名惯例一致，便于团队分工与查找。
2. **统一规范**：所有表使用 `InnoDB`（支持事务与行级锁）+ `utf8mb4`（完整支持中文与 Emoji）；主键统一 `BIGINT UNSIGNED AUTO_INCREMENT`；金额统一 `DECIMAL(10,2)`（禁止浮点数，避免精度丢失）；状态字段统一 `TINYINT` 并在注释中枚举含义。
3. **逻辑删除**：商品 SPU 采用 `delete_status` 逻辑删除（订单明细需引用历史商品），其余业务表在服务层校验引用关系后物理删除。
4. **快照字段**：订单明细（`oms_order_item`）保存商品名称、图片、规格、单价快照；订单主表保存收货信息快照。商品改价或会员修改资料不影响历史订单。
5. **不使用物理外键**：互联网电商系统惯例，关联一致性由服务层事务保证。避免级联删除风险与外键检查带来的写入开销，同时便于分库分表演进。

## 二、实体关系（ER 概览）

```
ums_role  ──< ums_role_menu >──  ums_menu
    │
    └──< ums_admin_role >──  ums_admin

ums_member ──< oms_order ──< oms_order_item
     │             │
     │             └──< oms_order_return
     │
     ├──< sms_coupon_member >──  sms_coupon
     │
     └──< ums_chat_message（客服会话）

pms_category ──< pms_product ──< pms_sku
pms_brand    ──< pms_product
```

- 管理员与角色：多对多（`ums_admin_role`）
- 角色与菜单权限：多对多（`ums_role_menu`）
- 会员与订单：一对多；订单与订单明细：一对多；订单与退款申请：一对多
- 会员与优惠券：多对多（`sms_coupon_member` 领取记录，含状态与使用订单）
- 分类（两级树）与商品：一对多；品牌与商品：一对多；商品（SPU）与 SKU：一对多

## 三、表清单（15 张）

| # | 表名 | 说明 | 关键设计点 |
|---|------|------|-----------|
| 1 | `ums_admin` | 后台管理员 | 用户名唯一索引；密码 BCrypt 存储 |
| 2 | `ums_role` | 角色 | 支撑 RBAC 的数据载体 |
| 3 | `ums_menu` | 菜单权限 | `type` 区分目录/菜单/按钮；`perm` 与后端接口 `@PreAuthorize` 一一对应 |
| 4 | `ums_admin_role` | 管理员-角色 | 联合主键，天然去重 |
| 5 | `ums_role_menu` | 角色-菜单 | 联合主键 |
| 6 | `ums_member` | 商城会员 | 用户名/手机号唯一；**成长值（growth，只增不减）决定等级，积分（points）为可消耗货币**；注册时间索引支撑"今日新增会员"统计 |
| 7 | `pms_brand` | 品牌 | 名称、首字母、排序、状态 |
| 8 | `pms_category` | 商品分类 | 两级树（`parent_id` + `level`），区别于无限级设计——电商运营实际只需两级，简化查询 |
| 9 | `pms_product` | 商品 SPU | `stock` 为 SKU 汇总冗余（列表页免 JOIN 聚合）；`publish_status` 上下架；`delete_status` 逻辑删除 |
| 10 | `pms_sku` | 商品 SKU | `specs` 保存规格 JSON（如颜色+尺码组合）；`sku_code` 唯一索引防重 |
| 11 | `oms_order` | 订单主表 | 状态机 `0待付款→1待发货→2已发货→3已完成`，`0/1→4已关闭`；收货信息快照；金额字段 `pay_amount = total_amount - coupon_amount + freight_amount` 恒等式 |
| 12 | `oms_order_item` | 订单明细 | 商品快照字段；`idx_order` 支撑详情联查 |
| 13 | `oms_order_return` | 退款申请 | 状态 `0待审核/1已通过/2已拒绝`；审核动作与订单状态联动（通过则订单关闭、库存回补） |
| 14 | `sms_coupon` | 优惠券 | `type` 区分满减券/折扣券；`receive_count/used_count` 计数冗余；`per_limit` 每人限领；`points_price` 支持积分商城兑换（0=不可兑换） |
| 15 | `sms_coupon_member` | 会员优惠券 | 领取记录 + 使用状态 + 关联订单，支撑"我的优惠券"与核销追溯 |
| 16 | `ums_chat_message` | 客服聊天消息 | `from_type` 区分发送方（会员/管理员）；读历史即按接收方标记已读；纯文本存储防 XSS |
| 17 | `pms_comment` | 商品评价 | `(order_id, product_id)` 唯一键防重复评价；昵称/等级快照；评分聚合支撑列表与详情星级 |

## 四、关键业务建模说明

### 4.1 SPU/SKU 双表模型
SPU（Standard Product Unit）描述"商品是什么"，SKU（Stock Keeping Unit）描述"具体可售单元"。
例：商品「云岚 无线降噪耳机 Pro」为 SPU，颜色「云雾白 / 曜石黑」为 2 个 SKU。
- 下单锁定的是 **SKU 库存**，非 SPU 库存；
- SPU 的 `stock`/`price` 为冗余汇总字段（SUM 库存 / MIN 价格），避免列表页 N 次聚合查询，写 SKU 时同步维护。

### 4.2 订单状态机

```
                支付                发货                确认收货
 0 待付款 ──────────→ 1 待发货 ──────────→ 2 已发货 ──────────→ 3 已完成
    │                    │
    │ 取消               │ 管理员关闭 / 退款通过
    └────────→ 4 已关闭 ←┘
```

- 状态流转在服务层使用显式状态机校验（非法流转抛出业务异常）；
- 关闭订单时 **回补 SKU 库存** 并 **恢复优惠券** 为未使用状态（同一事务内完成）。

### 4.3 库存并发安全
下单扣减库存使用 **条件更新**（`UPDATE pms_sku SET stock = stock - ? WHERE id = ? AND stock >= ?`），
利用 InnoDB 行锁 + 影响行数判断实现无超卖，不依赖悲观锁 `SELECT FOR UPDATE`，也不引入分布式锁——符合单库课设场景下的最简正确方案。

### 4.4 优惠券模型
- 满减券：`type=1`，`amount` 为减免金额，订单需满 `min_amount` 可用；
- 折扣券：`type=2`，`amount` 为折扣（如 9.5 表示 95 折），按 `total_amount × amount / 10` 计算抵扣；
- 领取动作对 `sms_coupon.receive_count` 做条件更新（`receive_count < total_count`）防超发；
- 每人限领校验 `sms_coupon_member` 中该会员该券的领取条数。

### 4.5 会员成长体系（双账户模型）
- **成长值（growth）**：消费 1 元 = 1 成长值，**只增不减**，决定会员等级
  （普通 0 / 银卡 1000 / 金卡 5000 / 钻石 15000）——积分消费不会导致掉级；
- **积分（points）**：站内货币，按等级倍率获取（普通/银卡 100%、金卡 120%、钻石 150%），
  用于积分商城兑换优惠券（固定 **100 积分 = 1 元面额**）；
- 等级权益：金卡及以上**全场免运费**、积分加速；规则集中在 `MemberLevels` 单一事实来源；
- 防通胀设计：获取速率（1 元 = 1 分）与兑换率（100:1）恒定，积分消耗渠道明确，返现率封顶 1%。

### 4.6 订单超时关单
- 定时任务每 60 秒扫描 `status=0 且 create_time < NOW() - 支付时限` 的订单；
- 单笔关单在独立事务中执行：条件更新状态（`eq status=0` 防并发）→ 回补 SKU/SPU 库存 → 回退销量 → 恢复优惠券；
- 支付时限由 `ORDER_PAY_TIMEOUT_MINUTES` 配置（默认 30 分钟），与前台倒计时展示口径一致。

## 五、索引设计要点

| 表 | 索引 | 用途 |
|----|------|------|
| `oms_order` | `uk_order_sn`、`idx_status`、`idx_create_time`、`idx_member` | 单号精确查 / 状态筛选 / 时间趋势统计 / 会员订单列表 |
| `pms_product` | `idx_category`、`idx_brand`、`idx_publish_status` | 分类/品牌筛选与上架商品查询 |
| `pms_sku` | `uk_sku_code`、`idx_product` | 编码防重 / 按商品联查 |
| `ums_member` | `uk_username`、`uk_phone`、`idx_register_time` | 登录/手机唯一 / 注册趋势统计 |
| `sms_coupon_member` | `idx_member`、`idx_coupon` | 我的优惠券 / 券的领取记录 |

## 六、演示数据说明

- 商品/品牌/分类/会员/优惠券/菜单权限为 `init.sql` 静态种子数据；
- 管理员与会员密码为占位符 `INIT`，后端首次启动时用 BCrypt 加密为 `admin123` / `123456`（避免在 SQL 中出现明文或固定哈希）；
- 订单及明细由后端 `DemoDataInitializer` 首次启动生成（日期相对当前时间滚动 30 天），保证看板趋势图任何时间演示都有数据。
