# 青柠商城管理系统 · CentOS 服务器部署指南

> 目标：把本地开发完成的 LimeMall 部署到 CentOS（7.9 / 8 / Stream 9 均可）云服务器，通过 Nginx 对外提供访问。
> 架构：Nginx（80 端口，静态前端 + 反向代理）→ Spring Boot（18080，仅本机监听）→ MySQL（3306，仅本机监听）。

## 一、环境准备

服务器需要安装：

| 软件 | 版本要求 | 用途 |
|------|----------|------|
| JDK | 21（必须） | 运行后端 |
| MySQL | 5.7+ / 8.0 | 数据库 |
| Nginx | 1.18+ | 静态站点 + 反向代理 |

> 前端构建可以在**本地 Windows** 完成（`npm run build` 生成 `dist/`），服务器不需要安装 Node。

### 1.1 安装 JDK 21

```bash
# 方式 A：yum 安装（若源提供）
sudo yum install -y java-21-openjdk-headless

# 方式 B：手动安装 Temurin（推荐，版本可控）
cd /opt
sudo curl -LO https://mirrors.tuna.tsinghua.edu.cn/Adoptium/21/jdk/x64/linux/OpenJDK21U-jdk_x64_linux_hotspot_21.0.6_7.tar.gz
sudo tar -zxvf OpenJDK21U-jdk_x64_linux_hotspot_21.0.6_7.tar.gz
sudo mv jdk-21* /opt/jdk-21
# 验证
/opt/jdk-21/bin/java -version
```

### 1.2 安装 MySQL 并导入数据

```bash
sudo yum install -y mysql-server   # 或按官方源安装 MySQL 8
sudo systemctl enable --now mysqld
sudo mysql_secure_installation     # 设置 root 密码（下文以 YOUR_DB_PASSWORD 代指）
```

上传 `sql/init.sql` 并导入：

```bash
mysql -uroot -p < /tmp/init.sql
# 验证
mysql -uroot -p -e "SHOW TABLES FROM lime_mall;"
```

> 安全建议：为应用创建专用账号（可选，课设可跳过）
> ```sql
> CREATE USER 'limemall'@'127.0.0.1' IDENTIFIED BY 'YOUR_APP_DB_PASSWORD';
> GRANT ALL PRIVILEGES ON lime_mall.* TO 'limemall'@'127.0.0.1';
> FLUSH PRIVILEGES;
> ```

### 1.3 安装 Nginx

```bash
sudo yum install -y nginx
sudo systemctl enable --now nginx
```

## 二、部署后端服务

### 2.1 上传构建产物

在**本地**构建 jar（或直接使用 `backend/target/limemall-backend.jar`）：

```bash
cd backend
set JAVA_HOME=D:\program\Java\jdk-21
mvn -s maven-settings.xml -DskipTests package
```

上传到服务器：

```bash
# 在本地 Windows 执行（scp 为 Git Bash 自带）
scp backend/target/limemall-backend.jar root@YOUR_SERVER_IP:/opt/limemall/
```

服务器上建目录：

```bash
sudo mkdir -p /opt/limemall/uploads
```

### 2.2 注册 systemd 服务

```bash
sudo vi /etc/systemd/system/limemall-backend.service
```

写入以下内容（按需修改密码与密钥）：

```ini
[Unit]
Description=LimeMall Backend (Spring Boot)
After=network.target mysqld.service

[Service]
Type=simple
WorkingDirectory=/opt/limemall
Environment=MYSQL_HOST=127.0.0.1
Environment=MYSQL_PORT=3306
Environment=MYSQL_DB=lime_mall
Environment=MYSQL_USER=root
Environment=MYSQL_PASSWORD=YOUR_DB_PASSWORD
# JWT 密钥（>=32 字节，务必改成随机字符串）
Environment=JWT_SECRET=请替换为你自己的至少32位随机密钥字符串
# 上传目录（注意是可写目录）
Environment=LIME_UPLOAD_DIR=/opt/limemall/uploads
# 应用端口（默认 18080，可按需修改）
Environment=SERVER_PORT=18080
# 未支付订单自动关闭时限（分钟，默认 30）
Environment=ORDER_PAY_TIMEOUT_MINUTES=30
# 跨域白名单（同源部署可保持默认；如需前端独立域访问后端，填入前端 https 域名逗号分隔）
Environment=CORS_ALLOWED_ORIGINS=https://你的前端域名
ExecStart=/opt/jdk-21/bin/java -Xms256m -Xmx512m -jar /opt/limemall/limemall-backend.jar
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```

启动并查看日志：

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now limemall-backend
sudo journalctl -u limemall-backend -f
# 看到 "Started LimeMallApplication" 即成功；首次启动会自动初始化密码与演示订单
```

> 18080 端口不需要对公网开放（Nginx 在本机反代即可，无需改防火墙）。

## 三、部署前端静态站点

### 3.1 本地构建

```bash
cd frontend
npm run build          # 生成 dist/
```

### 3.2 上传并配置 Nginx

```bash
# 本地执行
scp -r frontend/dist root@YOUR_SERVER_IP:/opt/limemall/
```

服务器上替换 Nginx 配置：

```bash
sudo vi /etc/nginx/conf.d/limemall.conf
```

写入：

```nginx
server {
    listen 80;
    server_name YOUR_SERVER_IP;   # 或你的域名

    # 前端静态资源（Vue3 打出的 dist）
    root /opt/limemall/dist;
    index index.html;

    # SPA 路由回退（/login、/shop 等前端路由）
    location / {
        try_files $uri $uri/ /index.html;
    }

    # 后端接口：/api/** -> Spring Boot（注意 proxy_pass 末尾斜杠会去掉 /api 前缀）
    location /api/ {
        proxy_pass http://127.0.0.1:18080/;
        # 同源部署：剥离 Origin 头，避免浏览器对 POST 请求携带的 Origin 被后端 CORS 校验误拦
        proxy_set_header Origin "";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        client_max_body_size 10m;
    }

    # 上传图片直接由 Nginx 提供（比反代更高效）
    location /uploads/ {
        alias /opt/limemall/uploads/;
    }

    # 客服 WebSocket 实时通道（协议升级头；令牌走查询参数，由后端握手鉴权）
    location /ws/ {
        proxy_pass http://127.0.0.1:18080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_read_timeout 3600s;
    }
}
```

> **CORS 零维护说明**：以上同源反代配置已通过 `proxy_set_header Origin ""` 剥离 Origin 头，
> 后端 CORS 白名单不会参与校验——**无论绑定域名、IP 还是端口，公网部署都无需维护跨域配置**；
> 仅当你把前端独立部署到另一个域名并让浏览器跨域直连后端时才需要设置 `CORS_ALLOWED_ORIGINS`。

检查并重载：

```bash
sudo nginx -t
sudo systemctl reload nginx
```

### 3.3 开放防火墙

```bash
sudo firewall-cmd --permanent --add-service=http
sudo firewall-cmd --reload
# 若使用云服务器安全组，还需在控制台放行 80 端口
```

## 四、验证

1. 浏览器访问 `http://YOUR_SERVER_IP/` → 商城前台主站；`http://YOUR_SERVER_IP/admin/login` → 后台管理（admin / admin123）
2. 前台会员登录（m001 / 123456 或现场注册），验证下单、客服聊天、会员中心
3. 健康检查：`curl http://127.0.0.1:18080/health` 返回 `"status":"UP","db":"UP"`（也可经域名访问检验反代链路）
3. 后台改一张商品图 → 前台可见（验证上传目录与 /uploads 映射）
4. 前台下单 → 后台看板与订单列表实时出现新订单

## 五、常见问题

| 现象 | 排查 |
|------|------|
| 后端启动报数据库连接失败 | 确认 MySQL 已启动、库 `lime_mall` 已导入、`MYSQL_PASSWORD` 环境变量正确（`journalctl -u limemall-backend` 查看日志） |
| 页面能开但接口 502 | 后端未启动或端口不对：`curl http://127.0.0.1:18080/portal/public/home` 在服务器本机验证 |
| 登录后刷新 404 | Nginx 缺少 `try_files ... /index.html` 回退 |
| 上传图片后 404 | 检查 `/opt/limemall/uploads` 存在且 Nginx `alias` 路径尾斜杠一致；SELinux 开启时可 `chmod 755 /opt/limemall/uploads` 或临时 `setenforce 0` 验证 |
| 中文乱码 | 数据库导入时未使用 utf8mb4：用 `--default-character-set=utf8mb4` 重新导入 init.sql |
| 时间差 8 小时 | 服务器时区：`sudo timedatectl set-timezone Asia/Shanghai` |

## 六、升级与备份

```bash
# 升级后端：上传新 jar 后重启
sudo systemctl restart limemall-backend

# 备份数据库
mysqldump -uroot -p --default-character-set=utf8mb4 lime_mall > /opt/limemall/backup_$(date +%F).sql
```

---

## 七、上线前安全清单（必读）

- [ ] **修改全部演示口令**：admin/yunying/kefu/kefu2（个人中心改密）与会员 m001~m020；
- [ ] 更换 `JWT_SECRET` 环境变量为随机 32+ 位字符串；
- [ ] 生产建议关闭 Swagger：`SPRINGDOC_API_DOCS_ENABLED=false` / `SPRINGDOC_SWAGGER_UI_ENABLED=false`；
- [ ] 为站点启用 HTTPS（证书 + 80→443 跳转；cloudflared 隧道天然 wss/https）；
- [ ] 如前后端分域部署，配置 `CORS_ALLOWED_ORIGINS`（同源反代无需配置）；
- [ ] 数据库账号最小权限、`/opt/limemall/uploads` 目录权限收敛；
- [ ] 定期备份数据库（见第八节）。

## 八、可选：接入支付宝沙箱真实支付

**推荐方式：部署完成后登录管理端 → 系统管理 → 支付配置**，在页面上粘贴 APPID / 应用私钥 / 支付宝公钥并打开启用开关，保存立即生效（配置存于 `sys_config` 表，密钥仅脱敏展示）。

以下环境变量方式作为**自动化部署兜底**（后台配置优先于环境变量）：

```bash
# /etc/systemd/system/limemall-backend.service 的 [Service] 段追加：
Environment=ALIPAY_APP_ID=你的沙箱APPID
Environment=ALIPAY_PRIVATE_KEY=应用私钥Base64（单行，无头尾）
Environment=ALIPAY_PUBLIC_KEY=支付宝公钥Base64（单行，无头尾）
Environment=ALIPAY_NOTIFY_URL=https://你的域名/api/portal/pay/alipay/notify
Environment=ALIPAY_RETURN_URL=https://你的域名/shop/orders
```

```bash
systemctl daemon-reload && systemctl restart limemall-backend
```

注意事项：
- `ALIPAY_NOTIFY_URL` 必须公网可访问（本系统 Nginx 已将 `/api/` 反代到后端，直接使用 https 域名即可）；
- 沙箱买家账号在开放平台「沙箱账号」页面获取，付款后异步通知通常数秒内到达；
- 未配置以上变量时，前台支付自动回退为模拟支付（本地演示不受影响）；
- 验签失败或金额不符的回调一律拒绝并记录日志（可在 `journalctl -u limemall-backend` 查看）。
