-- 模块2补充：后台会话撤销与菜单权限。已执行的 V003 不修改。
ALTER TABLE sys_user ADD COLUMN session_version BIGINT NOT NULL DEFAULT 0 COMMENT '后台会话撤销版本';
CREATE INDEX idx_role_menu_menu ON sys_role_menu(menu_id);
CREATE INDEX idx_user_role_role ON sys_user_role(role_id);
INSERT INTO sys_role(name,code,sort,status,remark)
SELECT '超级管理员','admin',0,1,'系统保留角色' WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE code='admin');
INSERT INTO sys_menu(parent_id,name,title,type,path,component,icon,permission,sort)
SELECT 0,'B2cMembers','商城会员','menu','/members','member/index','User','members:read',30
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path='/members' AND deleted=0);
INSERT INTO sys_menu(parent_id,name,title,type,path,component,icon,permission,sort)
SELECT 0,'B2cStaff','后台人员','menu','/staff','system/staff/index','UserFilled','staff:read',40
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path='/staff' AND deleted=0);
INSERT INTO sys_menu(parent_id,name,title,type,path,component,icon,permission,sort)
SELECT 0,'B2cRoles','角色权限','menu','/roles','system/role/index','Key','roles:read',41
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path='/roles' AND deleted=0);
INSERT INTO sys_menu(parent_id,name,title,type,path,component,icon,permission,sort)
SELECT 0,'B2cMenus','菜单权限','menu','/system/menu','system/menu/index','Menu','menu:read',42
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE path='/system/menu' AND deleted=0);
INSERT INTO sys_menu(parent_id,name,title,type,path,permission,sort,visible)
SELECT 0, CONCAT('B2cPermission',p.code),p.title,'button','',p.code,99,0 FROM (
  SELECT 'members:write' code,'管理会员状态' title UNION ALL SELECT 'staff:write','管理后台人员'
  UNION ALL SELECT 'roles:write','管理角色授权' UNION ALL SELECT 'menu:read','查看菜单配置'
  UNION ALL SELECT 'menu:write','管理菜单配置' UNION ALL SELECT 'product:read','查看商品分类品牌'
  UNION ALL SELECT 'product:write','编辑商品分类库存' UNION ALL SELECT 'upload:write','上传运营图片'
  UNION ALL SELECT 'dashboard:read','查看工作台'
) p WHERE NOT EXISTS (SELECT 1 FROM sys_menu m WHERE m.permission=p.code AND m.deleted=0);
