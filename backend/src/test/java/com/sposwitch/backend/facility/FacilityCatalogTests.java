package com.sposwitch.backend.facility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sposwitch.backend.common.error.ExternalApiException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class FacilityCatalogTests {

    private final SportsFacilityClient client = mock(SportsFacilityClient.class);
    private final Clock clock = mock(Clock.class);
    private final Queue<Runnable> tasks = new ArrayDeque<>();
    private final Instant start = Instant.parse("2026-09-30T00:00:00Z");
    private final FacilityCatalog catalog = new FacilityCatalog(client, clock, tasks::add);

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(start);
    }

    @Test
    void searchWhileFirstLoadIsRunningIsServiceUnavailable() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(1);
        when(client.fetchSeoul()).thenAnswer(invocation -> {
            started.countDown();
            assertTrue(release.await(5, TimeUnit.SECONDS));
            return List.of();
        });
        FacilityCatalog concurrentCatalog = new FacilityCatalog(client, clock,
                task -> Thread.ofVirtual().start(() -> {
                    try {
                        task.run();
                    } finally {
                        finished.countDown();
                    }
                }));
        try {
            concurrentCatalog.preload();
            assertTrue(started.await(5, TimeUnit.SECONDS));
            for (int i = 0; i < 20; i++) {
                assertLoading(concurrentCatalog);
            }
            verify(client, times(1)).fetchSeoul();
        } finally {
            release.countDown();
            assertTrue(finished.await(5, TimeUnit.SECONDS));
        }
        assertEquals(start, concurrentCatalog.nearby(37.5, 126.8, null, null).dataLoadedAt().toInstant());
    }

    @Test
    void initialFailureIsReportedAndRetriesWaitThirtySeconds() {
        when(client.fetchSeoul()).thenThrow(new ExternalApiException("시설 API 호출 실패"))
                .thenReturn(List.of(facility()));
        assertLoading(catalog);
        tasks.remove().run();

        for (int i = 0; i < 10; i++) {
            ExternalApiException failure = assertThrows(ExternalApiException.class,
                    () -> catalog.nearby(37.5, 126.8, null, null));
            assertEquals("시설 API 호출 실패", failure.getMessage());
        }
        assertTrue(tasks.isEmpty());
        verify(client, times(1)).fetchSeoul();

        when(clock.instant()).thenReturn(start.plusSeconds(29));
        assertThrows(ExternalApiException.class, () -> catalog.find("ID"));
        assertTrue(tasks.isEmpty());

        when(clock.instant()).thenReturn(start.plusSeconds(30));
        assertLoading(catalog);
        tasks.remove().run();
        assertEquals("ID", catalog.find("ID").orElseThrow().id());
        assertEquals(1, catalog.nearby(37.5, 126.8, null, null).totalCount());
        verify(client, times(2)).fetchSeoul();
    }

    @Test
    void expiredDataStaysAvailableDuringRefreshAndAfterFailure() {
        when(client.fetchSeoul()).thenReturn(List.of(facility()))
                .thenThrow(new ExternalApiException("시설 API 호출 실패"))
                .thenReturn(List.of());
        catalog.preload();
        tasks.remove().run();
        Instant refresh = start.plus(Duration.ofHours(25));
        when(clock.instant()).thenReturn(refresh);

        FacilityCatalog.Nearby duringRefresh = catalog.nearby(37.5, 126.8, null, null);
        assertEquals(1, duringRefresh.totalCount());
        assertEquals(start, duringRefresh.dataLoadedAt().toInstant());
        tasks.remove().run();
        assertEquals(1, catalog.nearby(37.5, 126.8, null, null).totalCount());
        assertTrue(tasks.isEmpty());

        when(clock.instant()).thenReturn(refresh.plusSeconds(30));
        assertEquals(1, catalog.nearby(37.5, 126.8, null, null).totalCount());
        tasks.remove().run();
        FacilityCatalog.Nearby updated = catalog.nearby(37.5, 126.8, null, null);
        assertEquals(0, updated.totalCount());
        assertEquals(refresh.plusSeconds(30), updated.dataLoadedAt().toInstant());
    }

    @Test
    void unexpectedFailureWithoutMessageStillHasAnErrorResponse() {
        when(client.fetchSeoul()).thenThrow(new IllegalStateException());
        catalog.preload();
        tasks.remove().run();
        ExternalApiException failure = assertThrows(ExternalApiException.class, () -> catalog.find("ID"));
        assertEquals("시설 정보를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.", failure.getMessage());
    }

    @Test
    void requestCanUseReloadThatCompletesImmediately() {
        when(client.fetchSeoul()).thenReturn(List.of(facility()));
        FacilityCatalog immediate = new FacilityCatalog(client, clock, Runnable::run);
        assertEquals(1, immediate.nearby(37.5, 126.8, null, null).totalCount());
        assertEquals(start, immediate.nearby(37.5, 126.8, null, null).dataLoadedAt().toInstant());
    }

    private static void assertLoading(FacilityCatalog catalog) {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> catalog.nearby(37.5, 126.8, null, null));
        assertEquals(503, exception.getStatusCode().value());
    }

    static Facility facility() {
        return Facility.from(new SportsFacilityClient.Item("ID", "시설", "정상운영", "테니스장", "테니스장",
                "실내", "공공", "서울특별시", "강서구", "", "서울특별시 강서구", "", "",
                "37.5", "126.8", "2026-09-30"));
    }
}
