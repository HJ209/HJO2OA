package com.hjo2oa.data.common.infrastructure.persistence;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import java.time.Instant;
import java.util.UUID;
import org.apache.ibatis.reflection.MetaObject;

public class DataServicesMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        if (getFieldValByName("id", metaObject) == null) {
            setFieldValByName("id", UUID.randomUUID(), metaObject);
        }
        if (getFieldValByName("createdAt", metaObject) == null) {
            setFieldValByName("createdAt", Instant.now(), metaObject);
        }
        if (getFieldValByName("updatedAt", metaObject) == null) {
            setFieldValByName("updatedAt", Instant.now(), metaObject);
        }
        if (getFieldValByName("deleted", metaObject) == null) {
            setFieldValByName("deleted", 0, metaObject);
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        setFieldValByName("updatedAt", Instant.now(), metaObject);
    }
}
