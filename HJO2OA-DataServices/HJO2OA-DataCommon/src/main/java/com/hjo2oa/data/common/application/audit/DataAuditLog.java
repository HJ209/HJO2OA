package com.hjo2oa.data.common.application.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataAuditLog {

    String module();

    String action();

    String targetType() default "";

    boolean captureArguments() default false;

    boolean captureResult() default false;
}
