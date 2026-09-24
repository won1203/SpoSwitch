package com.sposwitch.backend.facility;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "external-api.sports-facility")
public record SportsFacilityApiProperties(
        @NotBlank String baseUrl,
        @NotBlank String serviceKey
) {
}
