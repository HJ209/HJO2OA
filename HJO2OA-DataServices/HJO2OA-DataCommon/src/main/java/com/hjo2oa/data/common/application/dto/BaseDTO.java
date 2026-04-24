package com.hjo2oa.data.common.application.dto;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BaseDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String id;
    private String tenantId;
    private Instant createdAt;
    private Instant updatedAt;
}
