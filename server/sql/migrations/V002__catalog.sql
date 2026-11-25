CREATE TABLE product_sku (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '规格ID', product_id BIGINT NOT NULL COMMENT '商品ID',
  sku_code VARCHAR(64) NOT NULL COMMENT '规格编码', specification VARCHAR(200) NOT NULL COMMENT '规格名称',
  price DECIMAL(10,2) NOT NULL COMMENT '售价', stock INT NOT NULL DEFAULT 0 COMMENT '库存',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '状态',
  PRIMARY KEY(id), UNIQUE KEY uk_sku_code(sku_code), KEY idx_sku_product(product_id),
  CONSTRAINT chk_sku_stock CHECK(stock >= 0), CONSTRAINT chk_sku_price CHECK(price >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品销售规格';
INSERT INTO product_sku(product_id,sku_code,specification,price,stock,status)
SELECT id,CONCAT(product_code,'-DEFAULT'),'默认规格',sale_price,stock,1 FROM product WHERE deleted=0;
CREATE TABLE inventory_log (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '流水ID', sku_id BIGINT NOT NULL COMMENT '规格ID',
  delta INT NOT NULL COMMENT '变更数量', balance INT NOT NULL COMMENT '变更后库存',
  actor VARCHAR(64) NOT NULL COMMENT '身份及操作人', reason VARCHAR(200) NOT NULL COMMENT '调整原因',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY(id), KEY idx_inventory_sku_time(sku_id,create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存流水';
CREATE TABLE media_file (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '图片ID', owner_type VARCHAR(16) NOT NULL COMMENT '账号类型',
  owner_id BIGINT NOT NULL COMMENT '账号ID', url VARCHAR(500) NOT NULL COMMENT '图片地址',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  PRIMARY KEY(id), UNIQUE KEY uk_media_url(url), KEY idx_media_owner(owner_type,owner_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图片归属';
