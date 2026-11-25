# 青柠商城管理系统（LimeMall）

一个完整的电商管理系统课程设计作品：**后台管理端 + 商城前台**，覆盖商品（SPU/SKU）、订单、会员、营销（优惠券）、系统权限（RBAC）与数据看板。

## 技术栈

| 层 | 技术 |
|----|------|
| 后端 | JDK 21 · Spring Boot 3.3 · MyBatis-Plus 3.5 · Spring Security + JWT · MySQL 5.7+ |
| 前端 | Vue 3 · Vite 5 · Element Plus · Pinia · Vue Router · ECharts |
| 接口文档 | SpringDoc OpenAPI（Swagger UI） |

## 功能清单

**后台管理端（`/admin/login`，独立入口）**

- 数据看板：今日订单/成交额、会员与商品统计、待办提醒（待发货/待退款）、30 天销售趋势、订单状态分布、分类销售占比、热销 TOP10、最新订单
- 商品管理：商品 SPU/SKU 两级维护、分类树、品牌管理、上下架、SKU 库存调整、图片上传（富文本入库前消毒）
- 订单管理：多条件查询、订单详情（状态步骤条）、发货、关闭订单（自动回补库存/优惠券）、**未支付订单超时自动关闭**
- 退款审核：通过后自动关闭订单、回补库存销量、恢复优惠券
- 客服聊天：会话列表（未读数）、实时对话、快捷回复
- 会员管理：查询、启用/禁用（禁用后令牌即时失效）、等级与积分展示
- 营销管理：优惠券创建/编辑/上下线、领取与使用记录
- 系统管理：管理员 CRUD、角色管理与菜单/按钮级权限分配（RBAC）、个人中心改密

**商城前台（主站 `/`，自动进入 `/shop`）**

- 首页（分类、热销榜、新品）、商品搜索与分类浏览、商品详情（SKU 选择）
- 购物车（localStorage 持久化）、下单结算（收货信息 + 优惠券抵扣 + 满 99 包邮）
- 模拟支付、取消订单、确认收货、申请退款；**待付款订单显示支付倒计时，超时自动取消**
- 领券中心与我的优惠券
- **会员中心**：等级权益、积分与升级进度（消费累计积分，金卡 2 倍/钻石 3 倍，自动升级）
- **在线客服**：右下角悬浮窗与客服实时对话
- 会员注册与登录（独立于后台的身份体系）

## 目录结构

```
mall-course-design/
├── backend/            # Spring Boot 后端（单体工程）
│   ├── src/main/java/com/limemall/
│   │   ├── common/     # 统一响应、异常处理、分页
│   │   ├── config/     # 安全、跨域、MyBatis-Plus、OpenAPI、Jackson
│   │   ├── security/   # JWT 工具与过滤器
│   │   ├── entity/     # 15 张表实体
│   │   ├── mapper/     # 数据访问层（含看板统计 SQL）
│   │   ├── dto/ vo/    # 请求/响应模型
│   │   ├── service/    # 业务层
│   │   ├── controller/ # 管理端 + 前台接口
│   │   └── init/       # 密码初始化与演示订单生成
│   ├── pom.xml
│   └── maven-settings.xml   # 阿里云镜像加速
├── frontend/           # Vue3 前端
│   └── src/
│       ├── api/        # 按域拆分的接口封装
│       ├── layout/     # 后台布局 + 商城布局
│       ├── stores/     # 用户/购物车状态
│       ├── views/      # 后台页面 + shop 前台页面
│       └── router/     # 路由与登录守卫
├── sql/init.sql        # 建库建表 + 演示数据（可重复执行）
├── docs/               # 数据库设计、CentOS 部署指南
├── uploads/            # 商品图片上传目录（运行时生成）
├── start-backend.bat   # 一键启动后端（Windows）
└── start-frontend.bat  # 一键启动前端（Windows）
```

## 本地启动（Windows）

> 前置：MySQL 5.7+ 已启动（本机使用小皮面板）、JDK 21、Node 18+、Maven 3.9+

1. **导入数据库**（一次性）
   ```bash
   mysql -uroot -p < sql/init.sql
   ```
   默认连接 `127.0.0.1:3306`，账号 `root/123456`（可用环境变量 `MYSQL_USER` / `MYSQL_PASSWORD` 覆盖）。

2. **启动后端**（18080）
   ```bash
   start-backend.bat
   ```
   或手动：
   ```bash
   cd backend
   set JAVA_HOME=D:\program\Java\jdk-21
   mvn -s maven-settings.xml -DskipTests package
   "%JAVA_HOME%\bin\java" -jar target/limemall-backend.jar
   ```
   首次启动自动完成：管理员/会员密码 BCrypt 初始化、生成 60 条近 30 天演示订单（看板立即有数据）。

3. **启动前端**（5173）
   ```bash
   start-frontend.bat
   ```
   或手动：`cd frontend && npm install && npm run dev`

4. **访问**
   - 商城前台（主站）：http://localhost:5173/ （自动进入 `/shop`）
   - 后台管理（独立入口）：http://localhost:5173/admin/login
   - 接口文档：http://localhost:18080/swagger-ui.html
   - `localhost` 与 `127.0.0.1` 均可访问（Vite 已双栈监听）

> 端口说明：后端默认 **18080**（避开 Burp/Yakit 等安全工具默认占用的 8080），
> 可用环境变量 `SERVER_PORT` 覆盖；前端开发端口 5173。

## 演示账号

| 端 | 账号 | 密码 | 角色 |
|----|------|------|------|
| 后台 | admin | admin123 | 超级管理员（全部权限） |
| 后台 | yunying | admin123 | 运营专员（商品/订单/会员/营销） |
| 后台 | kefu | admin123 | 客服专员（订单/退款审核） |
| 前台 | m001 ~ m020 | 123456 | 商城会员 |

## 设计要点

- **防超卖**：下单扣库存使用条件更新 `UPDATE ... SET stock = stock - ? WHERE id = ? AND stock >= ?`，影响行数为 0 即库存不足回滚，不依赖分布式锁。
- **订单状态机**：`0待付款 → 1待发货 → 2已发货 → 3已完成；0/1 → 4已关闭`，所有流转使用「条件更新（CAS）」实现，并发操作（重复支付/重复审核/双端同时关单）只有一方生效；关闭/退款时同事务回补库存、回退销量、恢复优惠券。
- **超时关单**：定时任务每分钟扫描超过支付时限（默认 30 分钟，`ORDER_PAY_TIMEOUT_MINUTES` 可配）的未支付订单自动关闭并回补库存，防止恶意锁单。
- **会员成长体系**：消费 1 元累计 1 积分（银卡 ×1、金卡 ×2、钻石 ×3），积分达 500/2000/5000 自动升级，规则集中在 `MemberLevels` 单一事实来源。
- **金额计算**：统一 `DECIMAL(10,2)`；`实付 = 商品总额 - 优惠 + 运费`，满 99 包邮；折扣券按订单金额比例计算并封顶。
- **RBAC**：菜单/按钮权限落库（`ums_menu.perm`），后端接口 `@PreAuthorize` 校验、前端按权限渲染，三级角色（超管/运营/客服）演示数据齐备。
- **双端认证隔离**：JWT 载荷 `type=ADMIN/MEMBER`，后台与前台令牌互不通用；登录连续失败 5 次锁定 10 分钟（内存限流）。
- **安全措施**：BCrypt 密码、参数化 SQL、越权校验（会员订单归属检查）、上传文件名服务端 UUID 生成、富文本入库前消毒（去 script/事件属性）、分页大小上限 100。
- **客服聊天**：消息纯文本存储与渲染（天然防 XSS），前端 5 秒轮询拉取，Nginx 反代零配置，无需额外 WebSocket 网关。

## 文档

- 数据库设计说明：[docs/database-design.md](docs/database-design.md)
- CentOS 服务器部署指南：[docs/deploy-centos.md](docs/deploy-centos.md)

## 协作说明

- 仓库已按「数据库 → 后端 → 前端 → 文档」分阶段提交，便于分工与过程答辩。
- 后续提交请每位成员配置自己的 git 身份（`git config user.name / user.email`，不使用 `--global`），形成真实的协作记录。
