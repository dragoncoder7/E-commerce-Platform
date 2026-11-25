# 生产服务器部署

当前项目部署为两个前端、一个网关、两个业务服务和基础依赖：

```text
浏览器
  ├─ shop.example.com ─┐
  └─ erp.example.com  ─┴─ 宿主机 Nginx → store-web/admin-web
                                      → Gateway → store-service / erp-service
                                                        ├─ MySQL
                                                        ├─ Redis
                                                        ├─ Nacos
                                                        └─ media volume
```

商城服务和 ERP 服务是不同 Spring Boot 进程，分别使用 `store` 和 `erp` JWT 密钥。浏览器只访问 Nginx；MySQL、Redis、Nacos、ERP 和商城服务不直接暴露公网端口。

## 服务器准备

建议 Linux Ubuntu 22.04/24.04，至少 4 CPU、8 GB 内存、40 GB 可用磁盘。安装 Docker Engine、Docker Compose v2 和宿主机 Nginx。代码、Docker 数据目录和媒体卷放在有备份策略的磁盘上。当前提交尚未push；服务器需先收到完整代码，可使用受控文件传输或之后自行推送分支。

```bash
git clone <repository-url> /opt/ecommerce
cd /opt/ecommerce
cp server/deploy/.env.example server/deploy/.env
chmod 600 server/deploy/.env
```

编辑 `server/deploy/.env`，四个密钥必须改成随机值：

```bash
openssl rand -base64 32   # DB/MYSQL 密码可用
openssl rand -hex 32      # ERP_JWT_SECRET
openssl rand -hex 32      # STORE_JWT_SECRET，必须与ERP不同
```

生产邮件默认关闭。需要邮箱注册时，再设置 `STORE_MAIL_MODE=resend`、`RESEND_API_KEY` 和已验证域名的 `STORE_MAIL_FROM`。不要把这些值放入前端环境变量。

## 首次启动

```bash
cd /opt/ecommerce/server/deploy
# 启动 MySQL、Redis、Nacos
sudo docker compose --env-file .env -f compose.yml up -d mysql redis nacos
# 执行 Flyway 迁移；只执行一次，后续重复运行是幂等的
sudo docker compose --env-file .env -f compose.yml --profile tools run --rm migrate
# 构建并启动两个业务服务、网关和两个前端
sudo docker compose --env-file .env -f compose.yml up -d --build
sudo docker compose --env-file .env -f compose.yml ps
```

查看日志：

```bash
sudo docker compose --env-file .env -f compose.yml logs -f erp store gateway
```

## 初始化 ERP 管理员

迁移完成后，使用当前开发机已验证的 `server/.runtime/initialize-erp-admin.sql`，安全传输到服务器本目录后执行。它包含本次单独生成的初始密码哈希，不在Git中。

也可复制仓库内 `initialize-erp-admin.sql.example` 为 `initialize-erp-admin.sql`，生成自己的BCrypt哈希后替换文件中**两处** `REPLACE_WITH_BCRYPT_HASH`。未替换的模板不会创建账号。

```bash
# 用临时Python虚拟环境生成哈希；密码使用隐藏输入，不写入命令历史。
python3 -m venv /tmp/erp-bootstrap-venv
/tmp/erp-bootstrap-venv/bin/pip install bcrypt
/tmp/erp-bootstrap-venv/bin/python -c 'import bcrypt,getpass; print(bcrypt.hashpw(getpass.getpass("New ERP password: ").encode(),bcrypt.gensalt(rounds=12)).decode())'
# MySQL没有发布宿主机端口，直接通过容器连接；密码从容器环境取得。
sudo docker compose --env-file .env -f compose.yml exec -T mysql sh -c \
  'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot --default-character-set=utf8mb4 ecommerce_system' < initialize-erp-admin.sql
```

脚本只会在 `admin` 角色存在且没有启用管理员时创建 `erp_admin`；返回 `CREATED` 表示成功，`SKIPPED` 表示未修改账号。重复执行不会覆盖已有密码或提权同名账号。请在同一数据库连接中按顺序执行整份SQL，不并发运行。数据库工具可选中全文执行；出错时先 `ROLLBACK`，修正后再执行。

登录ERP后在「后台人员」编辑初始账号更换密码，或先创建并验证新的管理员账号，再处理临时账号。后台不允许自删或删除最后一个管理员。不要把这份可用初始化SQL提交到Git，初始化完成后将其移出服务器网站目录。

如果使用托管 MySQL，按服务商地址、端口和 TLS 参数修改 `SPRING_DATASOURCE_URL`，并确保应用数据库用户具备迁移所需的 DDL 权限；迁移后可收回 DDL 权限。

## Nginx 域名反向代理

宿主机 Nginx 使用 [host-nginx.conf.example](../deploy/host-nginx.conf.example) 的两个 server 块，把域名替换为实际域名。复制到 `/etc/nginx/sites-available/ecommerce.conf`，启用后检查：

```bash
sudo ln -s /etc/nginx/sites-available/ecommerce.conf /etc/nginx/sites-enabled/ecommerce.conf
sudo nginx -t
sudo systemctl reload nginx
```

该配置把商城域名转到 `127.0.0.1:3000`，ERP 域名转到 `127.0.0.1:3001`。两个容器内部的 Nginx 再把 `/api/**` 转发到 Compose 中的 `gateway:8080`，SPA 路由回退到 `index.html`。正式环境应使用 Certbot 配置 HTTPS：

```bash
sudo certbot --nginx -d shop.example.com -d erp.example.com
```

## 更新发布和备份

两个业务服务应以相同代码版本发布；本方案是单机维护窗口更新，不保证零停机。在预发布验证新版本后，将代码放到服务器并先构建镜像，构建失败时旧容器继续运行。

```bash
cd /opt/ecommerce/server/deploy
sudo docker compose --env-file .env -f compose.yml build
# 停止入口及两个写入服务，保持MySQL/Redis/Nacos运行。
sudo docker compose --env-file .env -f compose.yml stop admin-web store-web gateway erp store
# 备份文件包含业务数据，设置私有权限。
umask 077
sudo docker compose --env-file .env -f compose.yml exec -T mysql sh -c \
  'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysqldump -uroot --single-transaction --no-tablespaces ecommerce_system' > backup-$(date +%F-%H%M).sql
# Linux默认Docker数据目录；自定义data-root时以docker volume inspect实际Mountpoint为准。
sudo tar -czf media-$(date +%F-%H%M).tgz -C /var/lib/docker/volumes/ecommerce-prod_media/_data .
sudo docker compose --env-file .env -f compose.yml --profile tools run --rm migrate
# 只有备份和迁移都成功才继续启动。
sudo docker compose --env-file .env -f compose.yml up -d --no-build
sudo docker compose --env-file .env -f compose.yml ps
```

逐条检查退出码，失败时停止后续步骤。保留备份与上一版本镜像，验收前不清理镜像。数据库迁移无法通过切换镜像自动回滚；有不兼容结构变更时应按预演方案恢复数据库和对应代码。不要执行 `docker compose down -v`，否则会删除业务数据卷。

## 健康检查和排障

```bash
curl -fsS https://shop.example.com/
curl -fsS https://erp.example.com/
curl -fsS https://shop.example.com/api/store/catalog/products?pageNum=1\&pageSize=10
sudo docker compose --env-file .env -f compose.yml ps
sudo docker compose --env-file .env -f compose.yml logs --tail=200 erp store gateway
```

生产环境不要执行本地 `verify_*.py`、`start_local.py` 或带固定测试账号的脚本。不要启用 `STORE_MAIL_MODE=capture`，不要把 MySQL root 账号暴露给公网。支付、物流供应商、营销、分销、积分和订单/评价/报表/内容等未实现功能不会因部署自动出现，范围见 [GIT_DELIVERY.md](GIT_DELIVERY.md)。

## 已验证配置

本地已用非真实密钥执行 `docker compose config --quiet`，并用本机已有 `nginx:alpine` 镜像检查 `nginx -t`。两端应用前一轮已通过生产构建；本轮只验证新增部署配置语法，未实际构建新Docker镜像或启动整套生产Compose，未验证域名/证书，未连接远程服务器，未推送Git。
