package com.hjo2oa.shared.tenant;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import java.util.UUID;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;

public class SharedTenantLineHandler implements TenantLineHandler {

    private static final String TENANT_ID_COLUMN = "tenant_id";

    @Override
    public Expression getTenantId() {
        UUID tenantId = TenantContextHolder.requireTenantId();
        return new StringValue(tenantId.toString());
    }

    @Override
    public String getTenantIdColumn() {
        return TENANT_ID_COLUMN;
    }

    @Override
    public boolean ignoreTable(String tableName) {
        return TenantLineIgnoreStrategy.ignoreTable(tableName);
    }
}
