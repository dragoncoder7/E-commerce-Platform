package com.ecommerce.system.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import java.time.LocalDateTime;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

/** 为声明了填充策略的审计字段提供实际填充器，包括逻辑删除生成的更新。 */
@Component
public class ErpAuditFields implements MetaObjectHandler {
  @Override
  public void insertFill(MetaObject metaObject) {
    LocalDateTime now = LocalDateTime.now();
    strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
    strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
  }

  @Override
  public void updateFill(MetaObject metaObject) {
    if (metaObject.hasSetter("updateTime")) {
      setFieldValByName("updateTime", LocalDateTime.now(), metaObject);
    }
  }
}
