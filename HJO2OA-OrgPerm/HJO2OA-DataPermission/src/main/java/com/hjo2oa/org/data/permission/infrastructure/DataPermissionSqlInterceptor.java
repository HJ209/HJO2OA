package com.hjo2oa.org.data.permission.infrastructure;

import com.hjo2oa.org.data.permission.application.DataPermissionApplicationService;
import com.hjo2oa.org.data.permission.application.DataPermissionCommands;
import com.hjo2oa.org.data.permission.domain.DataPermissionDecisionView;
import com.hjo2oa.org.data.permission.domain.DataPermissionSqlFilter;
import java.sql.Connection;
import java.util.Objects;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
@Intercepts({
        @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})
})
public class DataPermissionSqlInterceptor implements Interceptor {

    private final ObjectProvider<DataPermissionApplicationService> applicationServiceProvider;
    private final DataPermissionSqlFilter sqlFilter;

    public DataPermissionSqlInterceptor(
            ObjectProvider<DataPermissionApplicationService> applicationServiceProvider,
            DataPermissionSqlFilter sqlFilter
    ) {
        this.applicationServiceProvider = Objects.requireNonNull(
                applicationServiceProvider,
                "applicationServiceProvider must not be null"
        );
        this.sqlFilter = Objects.requireNonNull(sqlFilter, "sqlFilter must not be null");
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        DataPermissionRuntimeContext context = DataPermissionRuntimeContext.current();
        if (context == null) {
            return invocation.proceed();
        }
        StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
        BoundSql boundSql = statementHandler.getBoundSql();
        DataPermissionApplicationService applicationService = applicationServiceProvider.getObject();
        DataPermissionDecisionView decision = applicationService.decideRow(new DataPermissionCommands.RowDecisionQuery(
                context.businessObject(),
                context.tenantId(),
                context.subjects()
        ));
        MetaObject metaObject = SystemMetaObject.forObject(boundSql);
        metaObject.setValue("sql", sqlFilter.appendWhere(boundSql.getSql(), decision));
        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }
}
