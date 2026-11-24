-- 可重复执行。首次启用后开始记录，不伪造历史登录时间。
USE ecommerce_system;
SET @migration_sql = IF(
  EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
          AND table_name = 'sys_user' AND column_name = 'last_login_time'),
  'SELECT 1',
  'ALTER TABLE sys_user ADD COLUMN last_login_time DATETIME NULL COMMENT ''最近一次成功登录时间'''
);
PREPARE login_migration FROM @migration_sql;
EXECUTE login_migration;
DEALLOCATE PREPARE login_migration;
