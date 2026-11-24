# 商品类别管理

入口：侧边栏「商品类别」（`/product/category`），或商品表单中的「管理类别」。在表单内维护类别不会清空已填写的商品信息；保存类别后刷新选项，启用的类别可以立即用于新增商品。

支持名称/编号搜索、状态筛选、层级维护、排序、说明、新增、编辑、启停和删除。搜索子类时会保留其父级，以展示完整层级。

- 名称最多 100 字，编号最多 50 字，说明最多 500 字；编号不可重复，包含已删除类别。
- 上级不能选择自身或后代；移动类别会同步调整子类层级。
- 启用子类前需启用所有上级。停用类别前需停用子类并迁移关联商品。
- 删除前需移除子类并迁移关联商品。停用类别不可用于保存商品。
- `/product/category/all` 返回全部未删除类别，包括停用记录；现有 `/tree`、`/children/{id}` 查询保持兼容。
- POST/PUT `/product/category` 返回保存后的类别对象；DELETE `/product/category/{id}` 执行受保护的逻辑删除。

使用真实后端（`VITE_USE_MOCK=false`）。此模块沿用现有 `product_category` 表，无新增数据库迁移；首次部署须已执行 `server/sql/product_module.sql`，已有数据库不要重复执行建表脚本。

验证：前端执行 `npm run build`。后端使用 JDK 21，在 `server/` 运行：

```shell
mvn -pl modules/system/system-service -am test-compile org.apache.maven.plugins:maven-surefire-plugin:3.5.3:test -Dtest=CategoryManagementTest,CategoryApiTest,ProductManagementTest -Dsurefire.failIfNoSpecifiedTests=false
```

测试使用内存 H2 和模拟依赖，不发送邮件。覆盖层级循环、子树移动、编号占用、关联保护、停用状态及商品分类校验。
