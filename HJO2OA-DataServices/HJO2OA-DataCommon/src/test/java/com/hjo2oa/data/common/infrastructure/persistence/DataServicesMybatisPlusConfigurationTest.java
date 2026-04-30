package com.hjo2oa.data.common.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.junit.jupiter.api.Test;

class DataServicesMybatisPlusConfigurationTest {

    @Test
    void shouldRegisterSqlServerInterceptorsAndMetaObjectHandler() {
        DataServicesMybatisPlusConfiguration configuration = new DataServicesMybatisPlusConfiguration();

        MybatisPlusInterceptor interceptor = configuration.dataServicesMybatisPlusInterceptor();
        MetaObjectHandler metaObjectHandler = configuration.dataServicesMetaObjectHandler();
        TestEntity entity = new TestEntity();
        metaObjectHandler.insertFill(SystemMetaObject.forObject(entity));

        assertNotNull(interceptor);
        assertEquals(2, interceptor.getInterceptors().size());
        assertTrue(interceptor.getInterceptors().get(0) instanceof TenantLineInnerInterceptor);
        assertTrue(interceptor.getInterceptors().get(1) instanceof PaginationInnerInterceptor);
        assertNotNull(entity.getId());
        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());
        assertEquals(0, entity.getDeleted());
    }

    static class TestEntity extends BaseEntityDO {
    }
}
