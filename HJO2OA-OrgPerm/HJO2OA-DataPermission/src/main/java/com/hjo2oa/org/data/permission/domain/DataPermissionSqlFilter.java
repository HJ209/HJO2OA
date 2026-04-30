package com.hjo2oa.org.data.permission.domain;

import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class DataPermissionSqlFilter {

    public String appendWhere(String sql, DataPermissionDecisionView decision) {
        Objects.requireNonNull(sql, "sql must not be null");
        Objects.requireNonNull(decision, "decision must not be null");
        String condition = decision.sqlCondition();
        if (condition == null || condition.isBlank()) {
            condition = decision.allowed() ? "1 = 1" : "1 = 0";
        }
        String normalized = sql.stripTrailing();
        String lower = normalized.toLowerCase(Locale.ROOT);
        int orderIndex = lower.lastIndexOf(" order by ");
        String suffix = "";
        if (orderIndex >= 0) {
            suffix = normalized.substring(orderIndex);
            normalized = normalized.substring(0, orderIndex);
            lower = normalized.toLowerCase(Locale.ROOT);
        }
        String connector = lower.contains(" where ") ? " AND " : " WHERE ";
        return normalized + connector + "(" + condition + ")" + suffix;
    }
}
