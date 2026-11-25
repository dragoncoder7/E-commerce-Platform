# 本次代码校验与 Git 交付

日期：2026-11-25（北京时间）。目标分支：`codex-STORY-newBranch`。仅本地提交，不 push。

## 范围及提交方式

本次按用户后续指令校验并保存当前阶段成果，不恢复此前暂停的全六模块开发。服务隔离、商品浏览、会员/地址、ERP权限已经共享同一套入口、路由和认证变更，因此作为可构建的应用快照提交；验证脚本、浏览器用例和交接文档另作一次提交。没有将交织修改强拆成不能构建的“已完成模块”，也不宣称满足原计划所有模块的最终验收。

已交付代码：

- `server/modules/commerce/commerce-core`：商品、分类、品牌、SKU、库存流水、媒体等共享代码。
- `server/modules/erp/erp-service`：独立ERP应用、商品管理、会员状态、后台人员、角色和菜单权限。
- `server/modules/store/store-service`：独立商城应用、商品浏览、账号/手机号/邮箱会员认证、资料和地址管理。
- `admin/`、`web/`：独立前端及各自接口、会话状态、路由。
- `server/sql/migrations/`：V001基线、V002商品、V003会员及V003.1权限增量。已执行SQL不重写。
- 双服务Docker构建及`compose.b2c.yml`、配置示例、单元测试、真实数据库/API及Chrome验证脚本。

## 提交前修复

1. 商品主图复用图库的协议和上传身份检查；创建及更新拒绝未拥有的本地图片。既有商品图片可由有权限的运营人员继续使用。
2. 无协议图片地址返回业务错误，避免`URI.getScheme()`为空触发空指针。
3. 库存调整先锁定未删除商品；删除商品返回404，不更改SKU库存或写流水。
4. Resend SDK由`LATEST`固定为当前验证版本`4.31.0`。
5. 原EmailTest改为纯Mock单元测试，不依赖本机配置、不向真实邮箱发送邮件；真实EmailIntegrationTest继续从默认测试中排除。
6. Chrome角色多选下拉框改用Escape关闭，避免测试点击被浮层遮挡；不使用强制点击跳过交互问题。
7. 补充本地密钥、IDE、构建和浏览器产物忽略规则；ERP配置示例去掉旧服务名和固定JWT值。

## 验证

本轮重新执行，详细原始日志仅在本地`.runtime/`保留。

| 检查 | 结果 |
|---|---|
| `mvn -B -f server/pom.xml package` | 全部11个Reactor项目成功；ERP38项、Store13项，共51项测试，0失败/错误/跳过 |
| `admin/`中`npm run build` | 类型检查与生产构建通过 |
| `web/`中`npm run build` | 类型检查与生产构建通过 |
| `verify_catalog.py` | 商品/分类/品牌/SKU、主图和图库、删除库存、15次并发库存扣减、文档访问通过 |
| `verify_members.py` | 29项会员认证、跨端Token、密码/状态/会话、邮箱验证码边界通过 |
| `verify_addresses.py` | 归属、默认唯一、16次并发切换、25次并发新增容量限制通过 |
| `verify_member_management.py` | ERP权限、停用角色、会员禁用及恢复不复活旧Token通过 |
| `verify_rbac.py` | 人员/角色/菜单CRUD、会话撤销、实时授权、管理员保护、树循环和引用检查通过 |
| `verify_migrations.py` | 真实MySQL空库初始化、旧库结构预检/基线/升级、重复迁移、历史商品账号保留通过 |
| Chrome `npx playwright test` | 14/14通过（1.2分钟）；真实Gateway、已安装Chrome、非Mock |
| 暂存区检查 | `git diff --cached --check`；扫描实际暂存内容，未发现当前运行密钥或构建产物 |

商品单元测试中的H2仅覆盖查询/映射；库存并发与迁移证据来自真实MySQL。未执行真实邮件投递、生产部署或订单全链路。

复现（server目录）：

```powershell
mvn -B package
python scripts/start_local.py --restart
python scripts/reset_verify_limits.py
.runtime/venv/Scripts/python scripts/verify_catalog.py
.runtime/venv/Scripts/python scripts/verify_members.py
.runtime/venv/Scripts/python scripts/verify_addresses.py
.runtime/venv/Scripts/python scripts/verify_member_management.py
.runtime/venv/Scripts/python scripts/verify_rbac.py
.runtime/venv/Scripts/python scripts/verify_migrations.py
```

各前端分别`npm run build`；web目录运行`npx playwright test`。验证脚本只用于本地隔离环境，会创建固定测试管理员及测试数据，不能对生产库运行。迁移验证创建独立测试库，不删除现有库。

## 本地保留和不提交内容

| 文件/数据 | 处理 |
|---|---|
| 原`server/gateway/src/main/resources/application-local.yml` | 保持原文件及工作区修改，不暂存、不覆盖；此文件原已被Git跟踪，新增ignore不会掩盖其修改 |
| 原`server/sql/ecommerce-system.sql` | 保留原文件，使用本机`.git/info/exclude`排除，不作为迁移提交 |
| `server/.runtime/verify.env`、本地application-local.yml、前端.env.local | 本地保留并忽略，未提交数据库密码/JWT/邮件密钥 |
| `.runtime/`的日志、JAR、截图、报告、邮件、媒体、Python虚拟环境 | 本地保留并忽略，供核验和重启 |
| node_modules、dist、target、Python缓存、IDE iml | 保留本地并忽略；旧被跟踪iml随服务迁移移除跟踪，不提交新iml |
| Docker数据库/媒体卷及测试库 | 保留，不删除，不纳入Git |

没有执行`git clean`、覆盖用户文件、删除数据卷、强制重置或远程push。提交前保存两个原文件的SHA256至本地，提交后比对确认未改变。

## 启动及配置

当前机器：商城`http://127.0.0.1:5174/`，ERP`http://127.0.0.1:5173/login`；网关18080、ERP18081、Store18082；MySQL13306、Redis16379、Nacos18848。服务使用最新构建重启后保留运行。

依赖用`server/compose.b2c.yml`启动。新机器复制`server/.env.b2c.example`为`server/.runtime/verify.env`，自行设置数据库密码和两个不同的强JWT密钥；不提交该文件。后台本地前端设置`VITE_USE_MOCK=false`、`VITE_API_BASE_URL=/api/admin`、`VITE_PROXY_TARGET=http://127.0.0.1:18080`；商城参考`web/.env.example`。

完整目录、技术栈、接口和手动步骤见[交接文档](PROJECT_HANDOVER.md)、[逐项核验](PLAN_AUDIT.md)、[会员验证](MEMBER_AUTH_VERIFICATION.md)。这些文档保留前次限时暂停时点，当前Git状态以本文件和`git log`为准。

## 未完成与限制

本次提交不是生产上线认证。仍需完成分类/品牌并发引用边界、完整后台多图CRUD浏览器验收、全权限矩阵及静态导航与动态菜单一致性、共享DTO/Controller分层收尾、阿里巴巴静态规范门禁。旧`server/docker-compose.yml`与部分旧README命令仍待迁移，当前使用`compose.b2c.yml`。生产管理员安全初始化脚本尚未交付。

购物车/订单、评价、真实销售报表和内容管理仍是计划内未实现；支付及支付记录、物流供应商接口、营销、分销、积分、多租户和短信验证码属于计划排除范围。不能把阶段测试通过描述为整个平台已完成。

## 最终提交记录

- 应用提交：`024c9a774b8b370f19e8fb537188423471101594`，实现商城与ERP隔离及商品会员权限阶段功能。
- 验证和文档提交：本文件所在提交，使用`git log -2 --oneline`查询；不向自身内容写入无法预知的自身哈希。
- Chrome首次13/14通过，角色多选浮层遮挡测试点击；改为Escape关闭后完整重跑14/14通过。归档`server/.runtime/commit-browser-results.json`。
- 最终后端51项、双前端构建、真实API/MySQL和迁移全部通过；没有重新启动全六模块开发。
- 两个原文件哈希核对未改变。提交后预期工作区仅保留原网关本地配置修改，其余生成文件已提交或按上表忽略。
- 仅本地提交；未push。
