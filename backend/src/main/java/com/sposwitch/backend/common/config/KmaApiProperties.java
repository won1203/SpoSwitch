package com.sposwitch.backend.common.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "external-api.kma")
public record KmaApiProperties(
        @NotBlank String baseUrl,
        @NotBlank String serviceKey
) {
}
