package com.sposwitch.backend.common.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "external-api.fitness100")
public record Fitness100ApiProperties(
        @NotBlank String baseUrl,
        @NotBlank String serviceKey
) {
}
