package com.sposwitch.backend.facility;

import com.sposwitch.backend.facility.Facility.Environment;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/facilities")
public class FacilityController {

    private final FacilityCatalog catalog;

    public FacilityController(FacilityCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * Facilities within 3 km, nearest first.
     * environment: INDOOR or OUTDOOR (both-type facilities match either).
     * goal: keeps only sports that suit that exercise goal; omit it to see every sport.
     */
    @GetMapping
    public FacilityCatalog.Nearby nearby(
            @RequestParam @DecimalMin("-90.0") @DecimalMax("90.0") double latitude,
            @RequestParam @DecimalMin("-180.0") @DecimalMax("180.0") double longitude,
            @RequestParam(required = false) Environment environment,
            @RequestParam(required = false) Goal goal
    ) {
        return catalog.nearby(latitude, longitude, environment, goal);
    }

    /** latitude/longitude are optional and only used to fill distanceMeters. */
    @GetMapping("/{id}")
    public Facility detail(
            @PathVariable String id,
            @RequestParam(required = false) @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
            @RequestParam(required = false) @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude
    ) {
        if ((latitude == null) != (longitude == null)) {
            throw new IllegalArgumentException("거리 계산에는 latitude와 longitude를 함께 입력해야 합니다.");
        }
        Facility facility = catalog.find(id)
                .orElseThrow(() -> new IllegalArgumentException("시설을 찾을 수 없습니다."));
        return latitude == null || longitude == null ? facility : facility.atDistanceFrom(latitude, longitude);
    }
}
