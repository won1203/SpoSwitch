package com.sposwitch.backend.facility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class FacilityCatalogTests {

    @Test
    void searchWhileFirstLoadIsRunningIsServiceUnavailable() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        SportsFacilityClient client = mock(SportsFacilityClient.class);
        when(client.fetchSeoul()).thenAnswer(invocation -> {
            release.await();
            return List.of();
        });
        FacilityCatalog catalog = new FacilityCatalog(client);
        catalog.preload();

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> catalog.nearby(37.5, 126.8, null, null));

        assertEquals(503, exception.getStatusCode().value());
        release.countDown();
    }
}
