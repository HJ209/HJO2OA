package com.hjo2oa.infra.audit.application;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {

    String module();

    String action();

    String targetType();

    String targetId() default "";

    boolean captureArguments() default true;

    boolean captureResult() default true;
}
