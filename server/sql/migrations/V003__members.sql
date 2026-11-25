-- 商城身份独立于 sys_user；不复制后台账号，不赋予后台角色。
CREATE TABLE member_user (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '会员主键',
  username VARCHAR(32) NULL COMMENT '账号名',
  phone VARCHAR(20) NULL COMMENT '手机号（未验证）',
  email VARCHAR(254) NULL COMMENT '已验证邮箱',
  password_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码摘要',
  nickname VARCHAR(50) NOT NULL COMMENT '显示昵称',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
  session_version BIGINT NOT NULL DEFAULT 0 COMMENT '会话撤销版本',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间（北京时间）',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_member_username (username),
  UNIQUE KEY uk_member_phone (phone),
  UNIQUE KEY uk_member_email (email),
  KEY idx_member_create_time (create_time),
  CONSTRAINT chk_member_identity CHECK (username IS NOT NULL OR phone IS NOT NULL OR email IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商城会员';
CREATE TABLE member_address (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '地址主键',
  member_id BIGINT NOT NULL COMMENT '会员主键',
  recipient VARCHAR(50) NOT NULL COMMENT '收货人',
  phone VARCHAR(20) NOT NULL COMMENT '收货电话',
  province VARCHAR(50) NOT NULL COMMENT '省',
  city VARCHAR(50) NOT NULL COMMENT '市',
  district VARCHAR(50) NOT NULL COMMENT '区县',
  detail VARCHAR(200) NOT NULL COMMENT '详细地址',
  is_default TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认',
  default_member_id BIGINT GENERATED ALWAYS AS (CASE WHEN is_default = 1 THEN member_id ELSE NULL END) STORED COMMENT '默认地址唯一约束',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_member_default_address (default_member_id),
  KEY idx_address_member (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会员收货地址';
