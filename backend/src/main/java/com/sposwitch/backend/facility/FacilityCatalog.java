package com.sposwitch.backend.facility;

import com.sposwitch.backend.common.error.ExternalApiException;
import com.sposwitch.backend.facility.Facility.Environment;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Keeps Seoul facilities in memory and answers radius searches.
 * The catalog is rebuilt from the public API after process startup; no database is required.
 */
@Component
public class FacilityCatalog {

    public static final int RADIUS_METERS = 3_000;
    public static final int MAX_RESULTS = 50;
    private static final Duration REFRESH_AFTER = Duration.ofHours(24);
    private static final Duration RETRY_AFTER_FAILURE = Duration.ofSeconds(30);
    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    private static final Logger log = LoggerFactory.getLogger(FacilityCatalog.class);

    private final SportsFacilityClient client;
    private final Clock clock;
    private final Executor executor;
    private final AtomicBoolean loading = new AtomicBoolean();
    private volatile CachedFacilities cache;
    private volatile Instant retryAt = Instant.MIN;
    private volatile String lastError;

    @Autowired
    public FacilityCatalog(SportsFacilityClient client) {
        this(client, Clock.systemUTC(), task -> Thread.ofVirtual().name("facility-reload").start(task));
    }

    FacilityCatalog(SportsFacilityClient client, Clock clock, Executor executor) {
        this.client = client;
        this.clock = clock;
        this.executor = executor;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void preload() {
        reloadInBackground();
    }

    public Nearby nearby(double latitude, double longitude, Environment environment, Goal goal) {
        CachedFacilities snapshot = current();
        List<Facility> inRadius = snapshot.items().stream()
                .filter(facility -> facility.matches(environment))
                .filter(facility -> goal == null || goal.matches(facility))
                .map(facility -> facility.atDistanceFrom(latitude, longitude))
                .filter(facility -> facility.distanceMeters() <= RADIUS_METERS)
                .sorted(Comparator.<Facility>comparingInt(Facility::distanceMeters).thenComparing(Facility::id))
                .toList();
        return new Nearby(
                inRadius.size(),
                RADIUS_METERS,
                OffsetDateTime.ofInstant(snapshot.loadedAt(), KOREA_ZONE),
                inRadius.subList(0, Math.min(MAX_RESULTS, inRadius.size()))
        );
    }

    public Optional<Facility> find(String id) {
        return current().items().stream().filter(facility -> facility.id().equals(id)).findFirst();
    }

    private CachedFacilities current() {
        CachedFacilities snapshot = cache;
        if (snapshot == null || snapshot.loadedAt().isBefore(clock.instant().minus(REFRESH_AFTER))) {
            reloadInBackground();
        }
        // A reload may have completed while this request was checking the cache.
        if (snapshot == null) {
            snapshot = cache;
        }
        if (snapshot == null) {
            String failure = lastError;
            if (failure != null && !loading.get()) {
                throw new ExternalApiException(failure);
            }
            // Still loading after startup is not an upstream failure: 503 tells clients to retry shortly.
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "시설 정보를 불러오는 중입니다. 잠시 후 다시 시도해 주세요.");
        }
        return snapshot;
    }

    private void reloadInBackground() {
        if (clock.instant().isBefore(retryAt) || !loading.compareAndSet(false, true)) {
            return;
        }
        // Another request may have finished a load between the first check and acquiring the slot.
        CachedFacilities latest = cache;
        Instant now = clock.instant();
        if (now.isBefore(retryAt)
                || (latest != null && !latest.loadedAt().isBefore(now.minus(REFRESH_AFTER)))) {
            loading.set(false);
            return;
        }
        try {
            executor.execute(this::reload);
        } catch (RuntimeException exception) {
            recordFailure(exception);
            loading.set(false);
        }
    }

    private void reload() {
        try {
            List<Facility> loaded = client.fetchSeoul();
            cache = new CachedFacilities(List.copyOf(loaded), clock.instant());
            retryAt = Instant.MIN;
            lastError = null;
            log.info("서울 체육시설 {}곳을 불러왔습니다.", loaded.size());
        } catch (RuntimeException exception) {
            recordFailure(exception);
        } finally {
            loading.set(false);
        }
    }

    private void recordFailure(RuntimeException exception) {
        lastError = exception instanceof ExternalApiException && exception.getMessage() != null
                && !exception.getMessage().isBlank() ? exception.getMessage()
                : "시설 정보를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.";
        retryAt = clock.instant().plus(RETRY_AFTER_FAILURE);
        log.warn("체육시설 불러오기 실패: {}", lastError);
    }

    private record CachedFacilities(List<Facility> items, Instant loadedAt) {
    }

    public record Nearby(int totalCount, int radiusMeters, OffsetDateTime dataLoadedAt, List<Facility> items) {
    }
}
