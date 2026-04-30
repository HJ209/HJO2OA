package com.hjo2oa.org.role.resource.auth.interfaces;

import java.util.Objects;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
public class ApiPermissionWebMvcConfiguration implements WebMvcConfigurer {

    private final ApiPermissionAuthorizationInterceptor interceptor;

    public ApiPermissionWebMvcConfiguration(ApiPermissionAuthorizationInterceptor interceptor) {
        this.interceptor = Objects.requireNonNull(interceptor, "interceptor must not be null");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/**",
                        "/api/v1/auth/**",
                        "/api/v3/api-docs/**",
                        "/api/swagger-ui/**",
                        "/api/swagger-ui.html"
                );
    }
}
