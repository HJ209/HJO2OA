package com.hjo2oa.data.connector.interfaces;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConfirmConnectorHealthAbnormalRequest(
        @NotBlank(message = "确认备注不能为空")
        @Size(max = 256, message = "确认备注长度不能超过 256 个字符")
        String note
) {
}
