package com.sposwitch.backend.facility;

import com.sposwitch.backend.common.error.ExternalApiException;
import com.sposwitch.backend.facility.Facility.Environment;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Keeps Seoul facilities in memory and answers radius searches.
 * ponytail: ~13k rows filtered in memory in well under 1 ms, so no DB table yet.
 * Move to the MySQL `facility` table with a spatial index if data grows past Seoul or needs history.
 */
@Component
public class FacilityCatalog {

    public static final int RADIUS_METERS = 3_000;
    public static final int MAX_RESULTS = 50;
    private static final Duration REFRESH_AFTER = Duration.ofHours(24);
    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    private static final Logger log = LoggerFactory.getLogger(FacilityCatalog.class);

    private final SportsFacilityClient client;
    private final AtomicBoolean loading = new AtomicBoolean();
    private volatile List<Facility> facilities;
    private volatile Instant loadedAt;
    private volatile String lastError;

    public FacilityCatalog(SportsFacilityClient client) {
        this.client = client;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void preload() {
        reloadInBackground();
    }

    public Nearby nearby(double latitude, double longitude, Environment environment, Goal goal) {
        List<Facility> inRadius = current().stream()
                .filter(facility -> facility.matches(environment))
                .filter(facility -> goal == null || goal.matches(facility))
                .map(facility -> facility.atDistanceFrom(latitude, longitude))
                .filter(facility -> facility.distanceMeters() <= RADIUS_METERS)
                .sorted(Comparator.<Facility>comparingInt(Facility::distanceMeters).thenComparing(Facility::id))
                .toList();
        return new Nearby(
                inRadius.size(),
                RADIUS_METERS,
                OffsetDateTime.ofInstant(loadedAt, KOREA_ZONE),
                inRadius.subList(0, Math.min(MAX_RESULTS, inRadius.size()))
        );
    }

    public Optional<Facility> find(String id) {
        return current().stream().filter(facility -> facility.id().equals(id)).findFirst();
    }

    private List<Facility> current() {
        List<Facility> snapshot = facilities;
        if (snapshot == null || loadedAt.isBefore(Instant.now().minus(REFRESH_AFTER))) {
            reloadInBackground();
        }
        if (snapshot == null) {
            if (lastError != null && !loading.get()) {
                throw new ExternalApiException(lastError);
            }
            // Still loading after startup is not an upstream failure: 503 tells clients to retry shortly.
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "시설 정보를 불러오는 중입니다. 잠시 후 다시 시도해 주세요.");
        }
        return snapshot;
    }

    private void reloadInBackground() {
        if (!loading.compareAndSet(false, true)) {
            return;
        }
        Thread.ofVirtual().name("facility-reload").start(() -> {
            try {
                List<Facility> loaded = client.fetchSeoul();
                facilities = loaded;
                loadedAt = Instant.now();
                lastError = null;
                log.info("서울 체육시설 {}곳을 불러왔습니다.", loaded.size());
            } catch (RuntimeException exception) {
                lastError = exception.getMessage();
                log.warn("체육시설 불러오기 실패: {}", exception.getMessage());
            } finally {
                loading.set(false);
            }
        });
    }

    public record Nearby(int totalCount, int radiusMeters, OffsetDateTime dataLoadedAt, List<Facility> items) {
    }
}
