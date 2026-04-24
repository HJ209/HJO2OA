package com.hjo2oa.data.openapi.infrastructure;

import com.hjo2oa.data.openapi.domain.OpenApiOperatorContext;
import com.hjo2oa.data.openapi.domain.OpenApiOperatorContextProvider;
import java.util.Objects;

public class StaticOpenApiOperatorContextProvider implements OpenApiOperatorContextProvider {

    private final OpenApiOperatorContext context;

    public StaticOpenApiOperatorContextProvider(OpenApiOperatorContext context) {
        this.context = Objects.requireNonNull(context, "context must not be null");
    }

    @Override
    public OpenApiOperatorContext currentContext() {
        return context;
    }
}
