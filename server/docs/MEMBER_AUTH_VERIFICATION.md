# 商城会员认证验证说明

更新时间：2026-11-25。范围：模块2中的商城注册、登录、本人资料、退出、修改密码。地址与 ERP 会员/RBAC 管理随后已实现并通过核心验证；完整权限矩阵和模块收尾仍待完成，详见 PROJECT_HANDOVER.md 及 PLAN_AUDIT.md。

## 本地入口

- 商城首页：http://127.0.0.1:5174/ ，页头“登录 / 注册”。
- 商城登录：http://127.0.0.1:5174/login
- 商城注册：http://127.0.0.1:5174/register
- 个人中心：http://127.0.0.1:5174/account （未登录会返回登录页）
- ERP：http://127.0.0.1:5173/login ，后台账号与商城账号独立。
- Gateway：18080；ERP：18081；商城后端：18082。

## 手动核验

1. 点击注册，选择账号注册。账号为4–32位字母/数字/下划线，字母开头；密码8–64位，包含字母和数字且UTF-8不超过72字节。验证必填、重复账号和确认密码不一致。
2. 注册成功进入登录页，使用刚注册的账号和密码登录。可从商品页点击登录，完成后应返回原商品页。
3. 进入个人中心，修改昵称、刷新页面，确认资料和会话恢复。
4. 退出后再次打开个人中心，应要求重新登录。修改密码后所有已有商城会话应失效。
5. 手机注册使用手机号+密码；注册后不声明手机号已验证。
6. 邮箱注册点击“获取验证码”。当前本地启动器启用 capture，不发真实邮件。运行以下命令读取指定邮箱的本地测试邮件（仅供开发人员，在 server 目录执行）：

```powershell
python scripts/read_local_mail.py your-address@example.com
```

输入6位验证码后注册，再使用邮箱+密码登录。验证码10分钟过期，60秒内不可重发，连续5次错误失效，成功后不可重复使用。

## 自动测试与构建

在 `server/`：

```powershell
mvn -B -pl modules/store/store-service -am test "-Dtest=MemberAuthServiceTest,MemberMailSenderTest" "-Dsurefire.failIfNoSpecifiedTests=false"
python scripts/reset_verify_limits.py
.runtime/venv/Scripts/python scripts/verify_members.py
```

在 `web/`：

```powershell
npm run build
npx playwright test tests/member.spec.ts
```

验证使用真实数据库，不使用Mock或H2。API测试会在隔离验证库创建测试会员，不应对生产库执行；邮件测试不访问Resend。

## 认证与邮件配置

- 商城会员保存在 `member_user`，公开注册不接受角色、状态、后台身份。
- JWT必须满足商城密钥及受众；Redis `store:session:` 独立会话；浏览器 `store:token` 独立键。
- 退出撤销当前会话；改密递增数据库会话版本撤销所有会话；每次保护请求重新检查状态及版本。
- 生产默认 `STORE_MAIL_MODE=disabled`。发送真实邮件需配置 `STORE_MAIL_MODE=resend`、`RESEND_API_KEY`、已验证发件人 `STORE_MAIL_FROM`。这些配置不得写入前端或提交Git。
- 本地验证脚本显式启用 `capture`。capture在生产profile启动时报错，验证码文件不在静态媒体目录、不由接口返回。真实邮件交付未验证，也不属于默认测试。
- Flyway独立执行V003；两个应用不自行执行迁移。

## 已取得的结果

- JUnit认证及邮件配置边界9项通过。
- 真实Gateway/MySQL/Redis会员检查通过，包含账号/手机/邮箱、唯一性、密码哈希、跨端Token、单会话退出、改密全量撤销、账号禁用和OTP重放/错误次数。
- Chrome会员4项通过，另有商品及ERP4项回归通过；覆盖1440px、1200px、390px，无横向溢出；邮箱注册→登录返回商品列表→修改密码→重新登录通过。
- 商城TypeScript检查和生产构建通过。
- 截图：`server/.runtime/store-register-{1440,1200,390}.png`、`store-account-{1440,1200,390}.png`；截图与运行日志保存在忽略目录，不包含真实账号密码。
- 地址和ERP会员/RBAC已有核心实现及验证，但全权限矩阵、规范和模块收尾未完成，尚不达到整个模块独立提交门槛；未push。

重复压力验证会触发真实限流。`reset_verify_limits.py` 仅允许清理带 ecommerce-b2c/redis Docker 标签的隔离容器中的 `store:limit:*` 测试计数，不清理会话或验证码，也不改动生产限流策略。手动验收前可运行一次。
